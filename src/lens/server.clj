(ns lens.server
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [org.httpkit.server :refer [run-server]]
            [lens.logging :refer [info]]
            [lens.app :refer [app]]))

(defrecord Server [port thread stop-fn]
  Lifecycle
  (start [server]
    (info {:component "Server" :msg (str "Start server on port " port)})
    (let [handler (app server)
          opts (assoc server :max-body (* 100 1024 1024))]
      (assoc server :stop-fn (run-server handler opts))))
  (stop [server]
    (info {:component "Server" :msg "Stop server"})
    (stop-fn)
    (assoc server :stop-fn nil)))

(defn new-server [file-storage-host]
  (map->Server {:file-storage-host file-storage-host}))
