(require '[reifyhealth.src-munch.core :as core]
         '[reifyhealth.src-munch.util :refer [parse-args]])

(defn -main
  "Use args to generate the gallery color edn data."
  []
  (core/parse-colors (parse-args)))

(-main)
