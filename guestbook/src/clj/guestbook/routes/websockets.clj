(ns guestbook.routes.websockets
  (:require [clojure.tools.logging :as log]
            [guestbook.messages :as msgs]
            [mount.core :refer [defstate]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [guestbook.middleware :as middleware]))

(defstate socket
          :start (sente/make-channel-socket!
                   (get-sch-adapter)
                   {:user-id-fn (fn [ring-req] (get-in ring-req [:params :client-id]))}))

(defn send! [uid message]
  (println "Sending message: " message)
  ((:send-fn socket) uid message))

(defmulti handle-message (fn [{:keys [id]}] id))

(defmethod handle-message :default
  [{:keys [id]}]
  (log/debug "Received unrecognized websocket event type: " id))

(defmethod handle-message :message/create!
  [{:keys [?data uid] :as message}]
  (let [response (try
                   (msgs/save-message! ?data)
                   (assoc ?data :timestamp (java.util.Date.))
                   (catch Exception e
                     (let [{id     :guestbook/error-id
                            errors :errors} (ex-data e)]
                       (case id
                         :validation {:errors errors}
                         ;;else
                         (do (.printStackTrace e)
                             {:errors {:server-error ["Failed to save message!"]}})))))]
    (if (:errors response)
      (send! uid [:message/creation-errors response])
      (doseq [uid (:any @(:connected-uids socket))]
        (send! uid [:message/add response])))))

(defn receive-message! [{:keys [id] :as message}]
  (log/debug "Got message with id: " id)
  (handle-message message))

(defstate channel-router
          :start (sente/start-chsk-router!
                   (:ch-recv socket)
                   #'receive-message!)
          :stop (when-let [stop-fn channel-router]
                  (stop-fn)))

(defn websocket-routes []
  ["/ws"
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]
    :get (:ajax-get-or-ws-handshake-fn socket)
    :post (:ajax-post-fn socket)}])
