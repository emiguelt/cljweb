(ns guestbook.routes.home
  (:require
    [guestbook.layout :as layout]
    [guestbook.routes.services :as services]
    [guestbook.middleware :as middleware]
    [ring.util.response]
    [guestbook.messages :as msgs]))

(defn home-basic [{:keys [params flash] :as request}]
  (layout/render
   request
   "home_initial.html"
   (let [errors (:errors flash)
         messages (msgs/message-list)
         new-params (if (nil? errors) {} params)]
     (merge messages new-params {:errors errors}))))

(defn home-reagent [request]
  (layout/render request "home-reagent.html" {}))

(defn home-re-frame-v1 [request]
  (layout/render request "home-reframe-v1.html" {}))

(defn about-page [request]
  (layout/render request "about.html"))

(defn save-message-initial [request]
  (let [errors (-> (services/save-message request) :body)]
    (home-basic (merge request {:flash errors}))))


(def home-page
  "Change to the latest version"
  home-re-frame-v1)

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/basic" {:get home-basic}]
   ["/reagent" {:get home-reagent}]
   ["/reframe-v1" {:get home-re-frame-v1}]
   ["/about" {:get about-page}]
   ["/message-initial" {:post save-message-initial}]])

