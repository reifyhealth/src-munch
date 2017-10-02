(ns reifyhealth.src-munch.core
  "Functions that parse development artifacts to produce consumable clojure EDN"
  (:require [reifyhealth.src-munch.gallery.colors.parse :as gallery-colors]
            [reifyhealth.src-munch.gallery.icons.parse :as gallery-icons]))

(def parse-colors gallery-colors/parse)

(def parse-icons gallery-icons/parse)
