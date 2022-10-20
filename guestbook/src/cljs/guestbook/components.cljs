(ns guestbook.components
  (:require [clojure.string :as string]
            [reagent.core :as r] ))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn message-form [send-msg messages]
  (let [fields (r/atom {})
        errors (r/atom {})]
    (fn []
      [:div
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :name} "Name"]
        [errors-component errors :name]
        [:input.input
         {:type      :text
          :name      :name
          :on-change #(swap! fields assoc :name (-> % .-target .-value))
          :value     (:name @fields)}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [errors-component errors :message]
        [:textarea.textarea
         {:name      :message
          :on-change #(swap! fields assoc :message (-> % .-target .-value))
          :value     (:message @fields)}]]
       [:input.button.is-primary
        {:type :submit
         :on-click #(send-msg fields errors messages)
         :value "comment"}]])))

(defn message-list [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

(defn home [send-msg get-msgs message-listener]
  (let [messages (message-listener)]
    (get-msgs messages)
    [:div.content>div.columns.is-centered>div.columns.is-two-thirds
     [:div.columns>div.column
      [:h2 "Messages"]
      [message-list messages]]
     [:div.columns>div.column
      [message-form send-msg messages]]]))
