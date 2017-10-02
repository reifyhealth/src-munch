(ns reifyhealth.src-munch.gallery.icons.parse
  "The logic used to parse icon data from our icomoon artifacts
  in order to provide it for display in the gallery."
  (:require
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [goog.object :as go]
            [reifyhealth.src-munch.util :refer [slurp spit]]))

(require '[cljs.pprint :refer [pprint]])

(def html-parser (js/require "htmlparser"))
(def DefaultHandler (go/get html-parser "DefaultHandler"))
(def Parser (go/get html-parser "Parser"))

(defn- first-match-children
  "Apply filter to get the first match, then take it's children"
  [pred coll]
  (-> (filter pred coll)
      first
      :children))

(defn- name-equal-to
  "Returns a function that checks if an html AST node's name is equal to `n`"
  [n]
  (fn [{:keys [name]} _]
    (= name n)))

(defn flatten-icon-data
  "Icons with the same name are coupled together, like \"up-arrow, sorted\""
  [{:keys [name value]}]
  (mapv #(hash-map :name (str/trim %) :value value) (str/split name #",")))

(defn- locate-icons
  "Traverse the html AST to find the icomoon glyph data"
  [dom]
  (as-> dom $
    (js->clj $ :keywordize-keys true)
    (first-match-children (name-equal-to "svg") $)
    (first-match-children (name-equal-to "defs") $)
    (first-match-children (name-equal-to "font") $)
    (filter (fn [{:keys [name attribs]} _]
              (and (= name "glyph")
                   (:glyph-name attribs)))
            $)
    (map (fn [{:keys [attribs]}]
           (-> attribs
               (select-keys [:glyph-name :unicode])
               (rename-keys {:glyph-name :name
                             :unicode :value})))
         $)
    (mapcat flatten-icon-data $)
    (sort-by :name $)))

(defn- spit-icon-edn
  "Creates resources/public/gallery-icon-data.edn, to be used directly by the gallery
  to generate the icons in the Iconography section."
  [to-output-filepath parsed-icons]
  (spit to-output-filepath parsed-icons))

(defn- parse-html
  "Parse the icomoon svg file for the icon data it holds"
  [input-filepath to-output-filepath]
  (let [parse-handler (fn [error dom]
                        (if error
                          (pprint error)
                          (let [parsed-icons (locate-icons dom)]
                            (pprint parsed-icons)
                            (spit-icon-edn to-output-filepath parsed-icons))))
        handler       (DefaultHandler. parse-handler)
        parser        (Parser. handler)]
    (as-> input-filepath $
      (slurp $)
      (.parseComplete parser $))))

(defn parse
  "Parse icomoon resources to produce icon edn data"
  [input-filepath to-output-filepath]
  (parse-html input-filepath to-output-filepath))
