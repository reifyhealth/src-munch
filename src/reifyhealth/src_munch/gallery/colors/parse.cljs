(ns reifyhealth.src-munch.gallery.colors.parse
  "Parses color data from our scss artifacts to provide it for display
  in the gallery."
  (:require [clojure.string :as str]
            [goog.object :as go]
            [reifyhealth.src-munch.util :refer [slurp spit delete-if-exists format]]))

(require '[cljs.pprint :refer [pprint]])

(def symbols-parser (js/require "scss-symbols-parser"))

(def cp (js/require "child_process"))
(def spawnSync (go/get cp "spawnSync"))

(def css (js/require "css"))

;; ---------
;; Node based helpers
;; ---------

(defn- parse-scss
  "Use the scss parser to parse a file, return a clojure hash-map instead of json"
  [f]
  (as-> f $
    (slurp $)
    (.parseSymbols symbols-parser $)
    (js->clj $ :keywordize-keys true)))

;; ---------
;; Pipeline functions (using accumulator to capture state)
;; ---------

(defn- parse-variables-scss
  "Use the scss parser to parse _variables.scss"
  [{:keys [variables-filepath] :as acc}]
  (assoc acc :color-imports (:imports (parse-scss variables-filepath))))

(defn- parse-colors-scss
  "Use the imported colors to parse the actual color scss files"
  [{:keys [scss-main-dir color-imports] :as acc}]
  (->> color-imports
       (map :filepath)
       (map #(let [{:keys [variables]} (parse-scss (str scss-main-dir "/" %))]
               {:filename %
                :colors   variables}))
       (assoc acc :parsed-colors)))

(defn- generate-color-scss
  "Uses parsed colors to create a scss file

  This is needed in order to calculate the scss values in isolation"
  [{:keys [gallery-color-scss-filepath parsed-colors] :as acc}]
  (->> parsed-colors
       (map-indexed (fn [idx {:keys [colors]}]
                      (map (fn [{:keys [name]}]
                             (format ".%d .%s { color: %s;}" idx (subs name 1) name))
                           colors)))
       flatten
       (clojure.string/join \newline)
       (str "@import \"main/variables\";" \newline)
       (spit gallery-color-scss-filepath))
  acc)

(defn- compile-color-scss
  "Runs the scss-compiler to produce a css file with the color values we need."
  [{:keys [verbose scss-compiler scss-compiler-source scss-compiler-css-output-dir] :as acc}]
  ;; Use the scss-compiler directly
  (let [args    (clj->js [scss-compiler-source
                          "--recursive"
                          "--follow"
                          "--source-map" "true"
                          "--source-map-contents"
                          "--output"
                          scss-compiler-css-output-dir])
        process (as-> args $
                  (spawnSync scss-compiler $)
                  (js->clj $ :keywordize-keys true))]
    (when verbose (pprint process))
    (if (-> process :status zero?)
      acc
      (assoc acc :errors [{:msg  (str "Unable to compile " scss-compiler-source)
                           :info process}]))))

(defn- get-selectors
  [stylesheet-rule]
  (-> stylesheet-rule
      (go/get "selectors")
      first))

(defn- parse-color-css
  "Parse the css to produce the color edn data"
  [{:keys [css-filename parsed-colors] :as acc}]
  (let [filename-map       (->> parsed-colors
                                (map-indexed (fn [idx {:keys [filename]}] {(str idx) filename}))
                                (apply merge))
        input              (slurp css-filename)
        ast                (.parse css input nil)
        output             (->> (-> ast (go/get "stylesheet") (go/get "rules"))
                                (filter #(some? (get-selectors %)))
                                (map #(let [selectors                     (get-selectors %)
                                            [file-index-class name-class] (clojure.string/split selectors " ")
                                            file-index                    (when file-index-class (subs file-index-class 1))
                                            name                          (when name-class (str "$" (subs name-class 1)))]
                                        (-> %
                                            (go/get "declarations")
                                            (first)
                                            (js->clj :keywordize-keys true)
                                            (select-keys [:value])
                                            (assoc :name name :file (filename-map file-index))))))
        color-edn          (->> output
                                (partition-by :file)
                                (mapv (fn [colors]
                                        {:color-group-name (-> colors first :file)
                                         :colors           (mapv #(select-keys % [:name :value]) colors)})))]
    (assoc acc :color-edn color-edn)))

(defn- spit-color-edn
  "Creates resources/public/gallery-color-data.edn, to be used directly by the gallery
  to generate the color swatches."
  [{:keys [color-edn-filepath color-edn] :as acc}]
  (spit color-edn-filepath (with-out-str (pprint color-edn)))
  acc)

(defn- cleanup
  "Remove intermediate files created by this script"
  [{:keys [gallery-color-scss-filepath css-filename] :as acc}]
  (doseq [path [gallery-color-scss-filepath
                css-filename
                (str css-filename ".map")]]
    (delete-if-exists path))
  acc)

(defn- has-errors?
  "At the acc level, determine if the pipeline currently has any errors"
  [{:keys [errors]}]
  (seq errors))

(defn- report-errors
  "Display pipeline errors to the user"
  [{:keys [errors] :as acc}]
  (println "The state of the accumulator when error occurred:")
  (pprint acc)
  (println "ERRORS OCCURED:")
  (doseq [e errors] (println (:msg e))))

(defn- process-parser-pipeline
  "Handles the steps to produce the color edn used by the gallery"
  [init pipeline-fns]
  (->> pipeline-fns
       (reduce (fn [acc pipeline-fn]
                 (let [results (pipeline-fn acc)]
                   (if (has-errors? results)
                     (do (report-errors results)
                         (reduced results))
                     results)))
               init)
       cleanup))

(defn parse
  "Parse scss resources to produce color edn data"
  [init]
  (let [{:keys [color-edn color-edn-filepath]} (process-parser-pipeline (select-keys init [:variables-filepath
                                                                                           :scss-main-dir
                                                                                           :gallery-color-scss-filepath
                                                                                           :compile-scss-filepath
                                                                                           :css-filename
                                                                                           :color-edn-filepath
                                                                                           :verbose
                                                                                           :scss-compiler
                                                                                           :scss-compiler-source
                                                                                           :scss-compiler-css-output-dir])
                                                                        [parse-variables-scss
                                                                         parse-colors-scss
                                                                         generate-color-scss
                                                                         compile-color-scss
                                                                         parse-color-css
                                                                         spit-color-edn])]
    (when color-edn
      (println (str "The following was written to " color-edn-filepath))
      (pprint color-edn))))
