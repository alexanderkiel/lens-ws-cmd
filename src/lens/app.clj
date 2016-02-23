(ns lens.app
  (:use plumbing.core)
  (:require [bidi.ring :as bidi-ring]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [lens.middleware.cors :refer [wrap-cors]]
            [lens.middleware.log :refer [wrap-log-errors]]
            [lens.middleware.multipart-redis :refer [redis-file-store]]
            [lens.handler :as h]))

(defn- route [opts]
  ["/" {"health" (h/health-handler opts)
        "command" {:post (h/command-handler opts)}
        "batch-command" {:post (h/batch-command-handler opts)}}])

(defn wrap-not-found [handler]
  (fn [req]
    (if-let [resp (handler req)]
      resp
      {:status 404})))

(defnk app
  "Whole app Ring handler."
  [file-storage-host :as opts]
  (let [redis-file-store (redis-file-store {:host file-storage-host})]
    (-> (bidi-ring/make-handler (route opts))
        (wrap-not-found)
        (wrap-keyword-params)
        (wrap-multipart-params {:store redis-file-store})
        (wrap-log-errors)
        (wrap-cors))))
