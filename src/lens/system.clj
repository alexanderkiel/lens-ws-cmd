(ns lens.system
  (:use plumbing.core)
  (:require [com.stuartsierra.component :as comp]
            [lens.server :refer [new-server]]
            [lens.broker :refer [new-broker]]
            [lens.util :as u]))

(defnk new-system [lens-ws-cmd-version port broker-host queue-prefix file-storage-host]
  (comp/system-map
    :version lens-ws-cmd-version
    :port (u/parse-long port)
    :thread 4

    :broker
    (new-broker {:host broker-host :queue-prefix queue-prefix})

    :server
    (comp/using (new-server file-storage-host) [:port :thread :broker])))
