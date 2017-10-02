(ns reifyhealth.src-munch.util
  "Provides some fns that will make working with nodejs easier"
  (:require [goog.object :as go]))

(def fs (js/require "fs"))
(def minimist (js/require "minimist"))
(def util (js/require "util"))
(def format* (go/get util "format"))

(defn parse-args
  "Get the args from the command line in a hash-map form"
  []
  (-> js/process
      (go/get "argv")
      (minimist)
      (js->clj :keywordize-keys true)))

(defn slurp
  "Read a file"
  [path]
  (str (.readFileSync fs path)))

(defn spit
  "Write a file"
  [f data]
  (.writeFileSync fs f data))

(defn delete-if-exists
  "Delete a file, if it exists"
  [f]
  (when (.existsSync fs f)
    (.unlinkSync fs f)))

(defn format
  [& args]
  (apply format* args))
