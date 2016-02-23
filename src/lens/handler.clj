(ns lens.handler
  "HTTP Handlers"
  (:use plumbing.core)
  (:require [lens.broker :as b :refer [send-command]]
            [cognitect.transit :as t]
            [clojure.java.io :as io]
            [schema.core :as s :refer [Symbol Any Keyword]]
            [clj-uuid :as uuid])
  (:refer-clojure :exclude [read]))

(set! *warn-on-reflection* true)

;; ---- Health ----------------------------------------------------------------

(defn health-handler [_]
  (fn [_]
    {:status 200
     :body "OK"}))

;; ---- Command ---------------------------------------------------------------

(defn- read [^String s]
  (t/read (t/reader (io/input-stream (.getBytes s)) :json)))

(def ^:private Params
  {Any Any})

(def ^:private Command
  "Short humand-writeable Command."
  [(s/one Keyword "name")
   (s/optional Params "params")])

(defn- validate [command]
  (let [[name params] (s/validate Command command)]
    {:name name
     :params params}))

(defn- resolve-attachment [v attachments]
  (if (symbol? v)
    (if-let [attachment (get attachments (keyword (name v)))]
      attachment
      (throw (ex-info (str "Missing attachment: " v) {})))
    v))

(defn- resolve-attachments' [params attachments]
  (map-vals #(resolve-attachment % attachments) params))

(defn- resolve-attachments [command attachments]
  (update command :params resolve-attachments' attachments))

(defn attach-id [command]
  (assoc command :id (uuid/v4) :sub "system"))

(defn- coerce [command attachments]
  (-> command
      (read)
      (validate)
      (resolve-attachments attachments)
      (attach-id)))

(defn- error [msg]
  {:status 422
   :body msg})

(s/defn handle-command [broker command attachments & [batch?]]
  (try
    (let [command (coerce command attachments)]
      (send-command broker command batch?)
      {:status 200
       :body "OK"})
    (catch Exception e
      (error (.getMessage e)))))

(defnk command-handler [broker]
  (fnk [body]
    (handle-command broker (slurp body) {})))

(defnk batch-command-handler [broker]
  (fnk [params body]
    (if-let [command (:command params)]
      (handle-command broker command params true)
      (handle-command broker (slurp body) {} true))))
