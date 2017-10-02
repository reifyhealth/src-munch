(require '[reifyhealth.src-munch.core :as core]
         '[reifyhealth.src-munch.util :refer [parse-args]])

(defn -main
  "Use iconmmon-filepath and icon-edn-filepath args to generate the gallery icon edn data."
  []
  (let [{:keys [icomoon-filepath icon-edn-filepath]} (parse-args)]
    (core/parse-icons icomoon-filepath icon-edn-filepath)))

(-main)
