(ns guestbook.routes.services
  (:require
    [guestbook.messages :as msgs]
    [guestbook.middleware :as middleware]
    [ring.util.http-response :as response]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]))

(defn save-message [{:keys [params]}]
  (try
    (msgs/save-message! params)
    (response/ok {:status :ok})
    (catch Exception e
      (let [{id     :guestbook/error-id
             errors :errors} (ex-data e)]
        (case id
          :validation (response/bad-request {:errors errors})
          ;;else
          (do (.printStackTrace e)
              (response/internal-server-error {:errors {:server-error ["Failed to save message!"]}})))))))

(defn message-list [_]
  (response/ok (msgs/message-list)))
(defn service-routes []
  ["/api" {:middleware [middleware/wrap-formats]
           :swagger {:id ::api}}
   ["" {:no-doc true}
    ["/swagger.json" {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*" {:get (swagger-ui/create-swagger-ui-handler {:url "/api/swagger.json"})}]]
   ["/messages" {:get  message-list
                :post save-message}]])
