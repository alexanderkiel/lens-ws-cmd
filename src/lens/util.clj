(ns lens.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [schema.core :as s :refer [Int]]
            [cognitect.transit :as t]
            [lens.logging :refer [trace]])
  (:import [java.io ByteArrayOutputStream]
           [java.util.concurrent ExecutionException]))

(defn unwrap-execution-exception [e]
  (if (instance? ExecutionException e)
    (.getCause e)
    e))

(defn error-type
  "Returns the error type of exceptions from transaction functions or nil."
  [e]
  (:type (ex-data (unwrap-execution-exception e))))

(defn parse-long [s]
  (Long/parseLong s))

(def Ms
  "Duration in milliseconds."
  s/Num)

(s/defn duration :- Ms
  "Returns the duaration in milliseconds from a System/nanoTime start point."
  [start :- Int]
  (/ (double (- (System/nanoTime) start)) 1000000.0))

;; ---- Transit ---------------------------------------------------------------

(defn write-transit [format write-opts o]
  (let [out (ByteArrayOutputStream.)]
    (t/write (t/writer out format write-opts) o)
    (io/input-stream (.toByteArray out))))

;; ---- Schema ----------------------------------------------------------------

(def NonBlankStr
  (s/constrained s/Str (complement str/blank?) 'non-blank?))

(def PosInt
  (s/constrained s/Int pos? 'pos?))

(def NonNegInt
  (s/constrained s/Int (comp not neg?) 'non-neg?))
