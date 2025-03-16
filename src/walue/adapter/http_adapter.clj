(ns walue.adapter.http-adapter
  (:require [clojure.data.json :as json]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [walue.infra.logging :as logging]
            [walue.port.evaluation-port :as port]))

(defn- handle-evaluate-portfolio [request evaluation-service]
  (try
    (let [body (:body request)
          portfolio (or (get body "portfolio") (get body :portfolio))
          criteria (or (get body "criterios") (get body :criterios))]
      (if (and portfolio criteria)
        (let [result (port/evaluate-portfolio evaluation-service portfolio criteria)]
          {:status 200
           :body result})
        {:status 400
         :body {:error "Invalid request. Portfolio and criteria are required."}}))
    (catch Exception e
      (logging/error "Error evaluating portfolio:" (.getMessage e))
      {:status 500
       :body {:error "An error occurred while processing your request."}})))

(defn- handle-health-check [_]
  {:status 200
   :body {:status "UP"}})

(defn make-handler [evaluation-service]
  (fn [request]
    (let [uri (:uri request)
          method (:request-method request)]
      (logging/info "Request received:" method uri)
      (cond
        (and (= uri "/api/evaluate") (= method :post))
        (handle-evaluate-portfolio request evaluation-service)
        
        (and (= uri "/health") (= method :get))
        (handle-health-check request)
        
        :else
        {:status 404
         :body {:error "Not found"}}))))

(defn wrap-logging [handler]
  (fn [request]
    (let [start (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start)]
      (logging/info "Request completed in" duration "ms with status" (:status response))
      response)))

(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (logging/error "Unhandled exception:" (.getMessage e))
        {:status 500
         :body {:error "Internal server error"}}))))

(defn create-app [evaluation-service]
  (-> (make-handler evaluation-service)
      (wrap-logging)
      (wrap-exception)
      (wrap-json-body {:keywords? false})
      (wrap-json-response)
      (wrap-params)))