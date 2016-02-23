(ns lens.broker
  "RabbitMQ Broker.

  Create a new one with (new-broker {:host ... :exchange-name ...}). Send
  commands with (send-command broker command)."
  (:require [schema.core :as s :refer [Keyword Any Uuid]]
            [lens.logging :refer [info]]
            [lens.util :refer [NonBlankStr]]
            [langohr.core :as rmq]
            [langohr.channel :as ch]
            [langohr.queue :as qu]
            [langohr.basic :as lb]
            [com.stuartsierra.component :refer [Lifecycle]]
            [cognitect.transit :as t])
  (:import [java.io ByteArrayOutputStream]))

;; ---- Schemas ---------------------------------------------------------------

(def Params
  {Any Any})

(def Command
  "A command is something which a subject likes to do in a system.

  A command has a id which is an arbitrary UUID. The name of a command is a
  keyword like :create-subject."
  {:id Uuid
   :name Keyword
   (s/optional-key :params) Params
   :sub NonBlankStr})

;; ---- Commands --------------------------------------------------------------

(defn- write [o]
  (let [out (ByteArrayOutputStream.)]
    (t/write (t/writer out :msgpack) o)
    (.toByteArray out)))

(s/defn routing-key [queue-prefix :- NonBlankStr batch?]
  (cond-> queue-prefix
    (not batch?) (str ".commands")
    batch? (str ".batch-commands")))

(s/defn send-command
  "Sends command to broker.

  Uses the batch queue for batch commands."
  {:arglists '([broker command] [broker command batch?])}
  [{:keys [ch queue-prefix]} command :- Command & [batch?]]
  (lb/publish ch "" (routing-key queue-prefix batch?) (write command)))

;; ---- Broker ----------------------------------------------------------------

(defrecord Broker [host port username password conn ch queue-prefix]
  Lifecycle
  (start [broker]
    (info {:component "Broker"
           :msg (format "Start broker on queues %s and %s"
                        (routing-key queue-prefix false)
                        (routing-key queue-prefix true))})
    (let [opts (cond-> {}
                 host (assoc :host host)
                 port (assoc :port port)
                 username (assoc :username username)
                 password (assoc :password password))
          conn (rmq/connect opts)
          ch (ch/open conn)]
      (qu/declare ch (routing-key queue-prefix false)
                  {:durable true :auto-delete false})
      (qu/declare ch (routing-key queue-prefix true)
                  {:durable true :auto-delete false})
      (assoc broker :conn conn :ch ch)))

  (stop [broker]
    (info {:component "Broker" :msg "Stop broker"})
    (when ch (rmq/close ch))
    (when conn (rmq/close conn))
    (assoc broker :ch nil :conn nil)))

(defn new-broker [opts]
  (map->Broker opts))
