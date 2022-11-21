(ns guestbook.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [guestbook.validation :refer [validate-message]]
            [guestbook.websocket :as ws]
            [guestbook.components :as cmp]
            [mount.core :as mount]))

(defn elem-by-id [id] (.getElementById js/document id))

(defn elem-val [elem] (.-value elem))

(defn log [s] (.log js/console s))

;; Register event handlers
(do
  (rf/reg-event-fx :app/initialize (fn [_ _] {:db {:messages/loading? true}}))
  (rf/reg-event-db :messages/set (fn [db [_ messages]] (-> db (assoc :messages/loading? false
                                                                     :messages/list messages))))
  (rf/reg-event-db :message/add (fn [db [_ message]] (do
                                                       (log (str "Message to add " message))
                                                       (update db :messages/list conj message))))
  (rf/reg-event-fx :message/send! (fn [{:keys [db]} [_ fields]]
                                    (ws/send! [:message/create! fields])
                                    {:db (dissoc db :form/server-errors)})))

;; Register subscriptions
(do
  (rf/reg-sub :messages/loading? (fn [db _] (:messages/loading? db)))
  (rf/reg-sub :messages/list (fn [db _] (:messages/list db []))))

(defn get-messages [msg-setter]
  (GET "/api/messages" {:headers {"Accept" "application/transit+json"}
                        :handler msg-setter}))

(defn send-message! [fields errors]
  (log (str "send message " fields))
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (try 
      (rf/dispatch [:message/send! @fields])
      (reset! errors nil)                         
      (reset! fields nil)
      (catch ExceptionInfo e (log (str "Error: " (ex-data e)))))))

(defn reagent-params []
  (let [messages (r/atom nil)] {:init #(log "Reagent initialized")
                                :send-msg   #(swap! messages conj (assoc % :timestamp (js/Date.)))
                                :msg-setter #(reset! messages (:messages %))
                                :msgs       messages
                                :loading?   (fn [] false)}))

(defn re-frame-v1-params []
  (let [messages (rf/subscribe [:messages/list])] {:init       #(do (mount/start)
                                                                  (rf/dispatch [:app/initialize])
                                                                    (log "Reframe v1 initialized"))
                                                   :send-msg   #(rf/dispatch [:message/add %])
                                                   :msg-setter #(rf/dispatch [:messages/set (:messages %)])
                                                   :msgs       messages
                                                   :loading?   (fn [] @(rf/subscribe [:messages/loading?]))}))

(def app-versions {:reagent    (reagent-params)
                   :re-frame-v1 (re-frame-v1-params)})

(def app-version (or (-> "app-version" elem-by-id elem-val keyword) :reagent))

(defn exec-params [] (app-version app-versions))

(defn ^:dev/after-load mount-components []
  (let [{:keys [msg-setter msgs loading?]} (exec-params)]
    (rf/clear-subscription-cache!)
    (log "Mounting components...")
    (dom/render [cmp/home send-message! #(get-messages msg-setter) msgs loading?]
                (elem-by-id "content"))
    (log "Components mounted")))


(defn init! []
  (let [{:keys [init msg-setter]} (exec-params)]
    (log (str "Initializing app - " app-version))
    (init)
    (get-messages msg-setter)
    (mount-components)))

