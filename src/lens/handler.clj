(ns lens.handler
  "HTTP Handlers

  * health-handler
  * command-handler
  * batch-command-handler"
  (:use plumbing.core)
  (:require [clj-uuid :as uuid]
            [clojure.java.io :as io]
            [cognitect.transit :as t]
            [lens.broker :as b]
            [schema.core :as s :refer [Any Keyword Str Symbol Uuid]])
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
   (s/optional (s/cond-pre Uuid Params) "id-or-params")
   (s/optional Params "params")])

(defn- validate [command]
  (s/validate Command command))

(extend-protocol uuid/UUIDRfc4122
  nil
  (uuid? [x] false))

(s/defn decode [command :- Command]
  (let [[name id-or-params params] command]
    {:name name
     :id (if (uuid/uuid? id-or-params) id-or-params (uuid/v4))
     :params (if (map? id-or-params) id-or-params params)}))

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

(defn attach-sub [command]
  (assoc command :sub "system"))

(s/defn coerce :- b/Command
  [command :- Str attachments]
  (-> command
      (read)
      (validate)
      (decode)
      (resolve-attachments attachments)
      (attach-sub)))

(defn- error [msg]
  {:status 422
   :body msg})

(s/defn handle-command [broker command :- Str attachments & [batch?]]
  (try
    (let [command (coerce command attachments)]
      (b/send-command broker command batch?)
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
