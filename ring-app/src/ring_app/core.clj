(ns ring-app.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.http-response :as response]
            [ring.middleware.reload :refer [wrap-reload]]
            [muuntaja.middleware :as muuntaja]
            [reitit.ring :as reitit]))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn wrap-formats [handler]
  (-> handler (muuntaja/wrap-format)))

(defn html-handler [request-map]
   (response/ok (str "<html><body>Your IP is: " 
              (:remote-addr request-map)
              "</body></html>")))

(defn json-handler [{{:keys [name]} :path-params {:keys [id]} :body-params}]
  (response/ok {:result {:name name :id id}}))

(def routes
  [["/" {:get html-handler}]
   ["/some/:name" {:post json-handler}]])

(def handler
  (reitit/ring-handler 
    (reitit/router routes)
    (reitit/create-default-handler
      {:not-found(constantly (response/not-found "404 - Page not found"))})))

(defn -main []
  (jetty/run-jetty 
    (-> #'handler 
        wrap-nocache 
        wrap-formats 
        wrap-reload)
    {:port 3000 :join? false}))

