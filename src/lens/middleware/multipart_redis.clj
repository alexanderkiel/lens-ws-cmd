(ns lens.middleware.multipart-redis
  (:require [clojure.java.io :as io]
            [taoensso.carmine :as car :refer [wcar]]
            [clj-uuid :as uuid])
  (:import [java.io ByteArrayOutputStream]))

(set! *warn-on-reflection* true)

(defn redis-file-store
  "Returns a function that stores multipart file parameters in redis.
  Accepts the following options:

  :host       - redis host
  :port       - redis port
  :expires-in - delete temporary files older than this many seconds
                (defaults to 3600 - 1 hour)

  The multipart parameters will be stored as maps with the following keys:

  :filename     - the name of the uploaded file
  :content-type - the content type of the upload file
  :host         - redis host
  :port         - redis port
  :key          - the key under which the file is sorted
  :size         - the size in bytes of the uploaded data"
  {:arglists '([options])}
  [{:keys [expires-in host port]
    :or {expires-in 3600
         host "127.0.0.1"
         port 6379}}]
  (fn [item]
    (let [key (str (uuid/v4))
          out (ByteArrayOutputStream.)
          _ (io/copy (:stream item) out)
          bytes (.toByteArray out)]
      (wcar {:pool {} :spec {:host host :port port}}
        (car/set key bytes))
      (-> (select-keys item [:filename :content-type])
          (assoc :host host
                 :port port
                 :key key
                 :size (count bytes))))))
