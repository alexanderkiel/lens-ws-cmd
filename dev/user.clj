(ns user
  (:use plumbing.core)
  (:use criterium.core)
  (:require [clojure.pprint :refer [pprint pp]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [schema.core :as s]
            [com.stuartsierra.component :as comp]
            [lens.system :refer [new-system]]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [lens.util :as u]))

(s/set-fn-validation! true)

(def system nil)

(defn init []
  (when-not system (alter-var-root #'system (constantly (new-system env)))))

(defn start []
  (alter-var-root #'system comp/start))

(defn stop []
  (alter-var-root #'system comp/stop))

(defn startup []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/startup))

;; Init Development
(comment
  (startup)
  )

;; Reset after making changes
(comment
  (reset)
  )

(defn- home [filename]
  (io/file (System/getProperty "user.home") filename))

(defn- write-transit [o]
  (u/write-transit :json {} o))

(defn- send-command [uri cmd & attachments]
  (if attachments
    (http/post uri {:multipart (conj attachments
                                     {:name "command"
                                      :content (write-transit cmd)})})
    (http/post uri {:body (write-transit cmd)})))

(comment
  (-> (send-command "http://localhost:5008/command"
                    [:create-study {:study-oid "S001"}])
      (deref)
      (update :body slurp)
      (select-keys [:status :body]))

  (-> (send-command "http://localhost:5008/batch-command"
                    '[:import-clinical-data {:file file}]
                    {:name "file"
                     :content (home "z/S001_D00044.xml")
                     :filename "S001_D00044.xml"})
      (deref)
      (update :body slurp)
      (select-keys [:status :body]))

  (-> (send-command "http://localhost:5008/batch-command"
                    '[:import-clinical-data {:file file}]
                    {:name "file"
                     :content (home "z/S001_T00001.xml")
                     :filename "S001_T00001.xml"})
      (deref)
      (update :body slurp)
      (select-keys [:status :body]))

  )

