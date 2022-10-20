(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [guestbook.validation :refer [validate-message]]
            [guestbook.components :as cmp]))

(defn get-messages [messages]
  (GET "/messages" {:headers {"Accept" "application/transit+json"}
                    :handler #(reset! messages (:messages %))}))


(defn send-message! [fields errors messages]
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (POST "/message"
          {:format        :json
           :headers       {"Accept"       "application/transit+json"
                           "x-csrf-token" (.-value (.getElementById js/document "token"))}
           :params        @fields
           :handler       #(do (swap! messages conj (assoc @fields :timestamp (js/Date.)))
                               (reset! errors nil)
                               (reset! fields nil))
           :error-handler (fn [e] (.error js/console (str "error:" e))
                            (reset! errors (-> e :response :errors)))})))

(dom/render [cmp/home send-message! get-messages #(r/atom nil)]
            (.getElementById js/document "content"))
