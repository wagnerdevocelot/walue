(ns walue.adapter.http-adapter
  (:require [clojure.data.json :as json]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [walue.port.evaluation-port :as evaluation-port]
            [walue.port.logging-port :as logging-port]))

(defn- handle-evaluate-portfolio [request evaluation-service logging-service]
  (try
    (let [body (:body request)
          portfolio (or (get body "portfolio") (get body :portfolio))
          criteria (or (get body "criterios") (get body :criterios))]
      (if (and portfolio criteria)
        (let [result (evaluation-port/evaluate-portfolio evaluation-service portfolio criteria)]
          {:status 200
           :body result})
        {:status 400
         :body {:error "Invalid request. Portfolio and criteria are required."}}))
    (catch Exception e
      (logging-port/log-error logging-service (str "Error evaluating portfolio: " (.getMessage e)))
      {:status 500
       :body {:error "An error occurred while processing your request."}})))

(defn- handle-health-check [_]
  {:status 200
   :body {:status "UP"}})

(defn make-handler [evaluation-service logging-service]
  (fn [request]
    (let [uri (:uri request)
          method (:request-method request)]
      (logging-port/log-info logging-service (str "Request received: " method " " uri))
      (cond
        (and (= uri "/api/evaluate") (= method :post))
        (handle-evaluate-portfolio request evaluation-service logging-service)

        (and (= uri "/health") (= method :get))
        (handle-health-check request)

        :else
        {:status 404
         :body {:error "Not found"}}))))

(defn wrap-logging [handler logging-service]
  (fn [request]
    (let [start (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start)]
      (logging-port/log-info logging-service
                             (str "Request completed in " duration " ms with status " (:status response)))
      response)))

(defn wrap-exception [handler logging-service]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (logging-port/log-error logging-service (str "Unhandled exception: " (.getMessage e)))
        {:status 500
         :body {:error "Internal server error"}}))))

(defn create-app [evaluation-service logging-service]
  (-> (make-handler evaluation-service logging-service)
      (wrap-logging logging-service)
      (wrap-exception logging-service)
      (wrap-json-body {:keywords? false})
      (wrap-json-response)
      (wrap-params)))