(ns guestbook.websocket
  (:require [cljs.reader :as edn]))

(defonce channel (atom nil))

(defn connect! [url receive-handler]
  (if-let [chan (js/WebSocket. url)]
    (do
        (.log js/console "Connected!")
        (set! (.-onmessage chan) #(do
                                     (.log js/console (str "Msg received: " (edn/read-string (.-data %))))
                                     ( ->> %
                                       .-data
                                       edn/read-string
                                       receive-handler)))
        (reset! channel chan))
    (throw (ex-info "Websocket connection failed" {:url url}))))

(defn send-message! [msg]
  (if-let [chan @channel]
    (do 
    (.log js/console (str "Sending message:" (pr-str msg)))
    (.send chan (pr-str msg)))
    (throw (ex-info "Couldn't sent the message, channel is closed" {:message msg}))))
