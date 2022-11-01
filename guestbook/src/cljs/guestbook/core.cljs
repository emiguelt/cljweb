(ns guestbook.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [guestbook.validation :refer [validate-message]]
            [guestbook.components :as cmp]))

(defn elem-by-id [id] (.getElementById js/document id))

(defn elem-val [elem] (.-value elem))

(defn log [s] (.log js/console s))

;; Register event handlers
(do
  (rf/reg-event-fx :app/initialize (fn [_ _] {:db {:messages/loading? true}}))
  (rf/reg-event-db :messages/set (fn [db [_ messages]] (-> db (assoc :messages/loading? false
                                                                     :messages/list messages))))
  (rf/reg-event-db :messages/add (fn [db [_ message]] (update db :messages/list conj message))))

;; Register subscriptions
(do
  (rf/reg-sub :messages/loading? (fn [db _] (:messages/loading? db)))
  (rf/reg-sub :messages/list (fn [db _] (:messages/list db []))))

(defn get-messages [msg-setter]
  (GET "/api/messages" {:headers {"Accept" "application/transit+json"}
                    :handler msg-setter}))


(defn send-message! [msg-setter fields errors]
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (POST "/api/messages"
          {:format        :json
           :headers       {"Accept"       "application/transit+json"
                           "x-csrf-token" (elem-val (elem-by-id "token"))}
           :params        @fields
           :handler       #(do (msg-setter @fields)
                               (reset! errors nil)
                               (reset! fields nil))
           :error-handler (fn [e] (.error js/console (str "error:" e))
                            (reset! errors (-> e :response :errors)))})))

(defn reagent-params []
  (let [messages (r/atom nil)] {:init #(log "Reagent initialized")
                                :send-msg   #(swap! messages conj (assoc % :timestamp (js/Date.)))
                                :msg-setter #(reset! messages (:messages %))
                                :msgs       messages
                                :loading?   (fn [] false)}))

(defn re-frame-v1-params []
  (let [messages (rf/subscribe [:messages/list])] {:init       #(do (rf/dispatch [:app/initialize])
                                                                    (log "Reframe v1 initialized"))
                                                   :send-msg   #(rf/dispatch [:messages/add (assoc % :timestamp (js/Date.))])
                                                   :msg-setter #(rf/dispatch [:messages/set (:messages %)])
                                                   :msgs       messages
                                                   :loading?   (fn [] @(rf/subscribe [:messages/loading?]))}))

(def app-versions {:reagent    (reagent-params)
                   :re-frame-v1 (re-frame-v1-params)})

(def app-version (or (-> "app-version" elem-by-id elem-val keyword) :reagent))

(defn exec-params [] (app-version app-versions))

(defn ^:dev/after-load mount-components []
  (let [{:keys [send-msg msg-setter msgs loading?]} (exec-params)]
    (rf/clear-subscription-cache!)
    (log "Mounting components...")
    (dom/render [cmp/home (partial send-message! send-msg) #(get-messages msg-setter) msgs loading?]
                (elem-by-id "content"))
    (log "Components mounted")))


(defn init! []
  (let [{:keys [init msg-setter]} (exec-params)]
    (log (str "Initializing app - " app-version))
    (init)
    (get-messages msg-setter)
    (mount-components)))
