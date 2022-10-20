(ns guestbook.routes.home
  (:require
   [guestbook.layout :as layout]
   [guestbook.db.core :as db]
   [clojure.java.io :as io]
   [guestbook.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [guestbook.validation :refer [validate-message]]))

(defn home-basic [{:keys [params flash] :as request}]
  (layout/render
   request
   "home_initial.html"
   (let [errors (:errors flash)
         messages {:messages (db/get-messages)}
         new-params (if (nil? errors) {} params)]
     (merge messages new-params {:errors errors}))))

(defn home-reagent [request]
  (layout/render request "home-reagent.html" {}))

(defn about-page [request]
  (layout/render request "about.html"))

(defn save-message [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (response/bad-request {:errors errors})
    (try
      (db/save-message! params)
      (response/ok {:status :ok})
      (catch Exception e
        (response/internal-server-error {:errors {:server-error ["failed to save message!"]}})))))

(defn save-message-initial [request]
  (let [errors (-> (save-message request) :body)]
    (home-basic (merge request {:flash errors}))))

(defn message-list [_]
  (response/ok {:messages (vec (db/get-messages))}))

(def home-page
  "Change to the latest version"
  home-reagent)

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/basic" {:get home-basic}]
   ["/reagent" {:get home-reagent}]
   ["/about" {:get about-page}]
   ["/message" {:post save-message}]
   ["/message-initial" {:post save-message-initial}]
   ["/messages" {:get message-list}]])

