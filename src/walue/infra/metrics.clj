(ns walue.infra.metrics
  (:require [walue.infra.logging :as logging])
  (:import [java.util.concurrent.atomic AtomicLong]))

(defonce metrics-registry (atom {}))

(defn create-counter [name description]
  (let [counter (AtomicLong. 0)]
    (swap! metrics-registry assoc name {:type :counter
                                        :description description
                                        :value counter})
    counter))

(defn increment-counter [counter]
  (.incrementAndGet ^AtomicLong counter))

(defn increment-counter-by [counter value]
  (.addAndGet ^AtomicLong counter value))

(defn get-counter-value [counter]
  (.get ^AtomicLong counter))

(defn create-gauge [name description value-fn]
  (swap! metrics-registry assoc name {:type :gauge
                                      :description description
                                      :value-fn value-fn})
  value-fn)

(defn get-all-metrics []
  (reduce-kv 
    (fn [acc k v]
      (let [value (cond
                    (= (:type v) :counter) (get-counter-value (:value v))
                    (= (:type v) :gauge) ((:value-fn v))
                    :else nil)]
        (assoc acc k {:type (:type v)
                      :description (:description v)
                      :value value})))
    {}
    @metrics-registry))

(defn init-metrics []
  (let [request-counter (create-counter "http_requests_total" "Total number of HTTP requests")
        error-counter (create-counter "http_errors_total" "Total number of HTTP errors")]
    {:request-counter request-counter
     :error-counter error-counter}))