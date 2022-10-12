(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(defn message-form []
  (let [fields (r/atom {})]
    (fn []
      [:div
       [:div.field
        [:label.label {:for :name} "Name"]
        [:input.input
         {:type      :text
          :name      :name
          :on-change #(swap! fields assoc :name (-> % .-target .-value))
          :value     (:name @fields)}]]
       [:div.field
        [:label.label {:for :name} "Message"]
        [:textarea.textarea
         {:name      :message
          :on-change #(swap! fields assoc :message (-> % .-target .-value))
          :value     (:message @fields)}]]
       [:input.button.is-primary
        {:type :submit
         :value "comment"}]])))

(defn home []
  [:div.content>div.columns.is-centered>div.columns.is-two-thirds
   [:div.columns>div.column
    [message-form]]])

(dom/render [home] (.getElementById js/document "content"))
