(ns guestbook.websocket
  (:require-macros [mount.core :refer [defstate]])
  (:require [re-frame.core :as rf]
            [taoensso.sente :as sente]
            mount.core))

(defstate socket
          :start (sente/make-channel-socket!
                   "/ws"
                   (.-value (.getElementById js/document "token"))
                   {:type :auto
                    :wrap-recv-evs? false}))

(defn send! [message]
  (if-let [send-fn (:send-fn @socket)]
    (send-fn message)
    (throw (ex-info "Couldn't send message, channel is not open!"
                    {:message message}))))

(defmulti handle-message
          (fn [{:keys [id]} _] id))

(defmethod handle-message :message/add
  [_ msg-add-event]
  (rf/dispatch msg-add-event))

(defmethod handle-message :message/creation-errors
  [_ [_ response]]
  (rf/dispatch [:form/set-server-errors (:errors response)]))

;; ---------------------------------------------------------------------------
;; Default Handlers
(defmethod handle-message :chsk/handshake
  [{:keys [event]} _]
  (.log js/console "Connection Established: " (pr-str event)))
(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State Changed: " (pr-str event)))
(defmethod handle-message :default
  [{:keys [event]} _]
  (.warn js/console "Unknown websocket message: " (pr-str event)))

;; ---------------------------------------------------------------------------
;; Router
(defn receive-message!
  [{:keys [id event] :as ws-message}]
  (do
    (.log js/console "Event Received: " (pr-str event))
    (handle-message ws-message event)))
(defstate channel-router
          :start (sente/start-chsk-router!
                   (:ch-recv @socket)
                   #'receive-message!)
          :stop (when-let [stop-fn @channel-router]
                  (stop-fn)))
