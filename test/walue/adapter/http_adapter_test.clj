(ns walue.adapter.http-adapter-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [walue.adapter.http-adapter :as http]
            [walue.port.evaluation-port :as port]))

(defn- parse-json-body [response]
  (json/read-str (:body response) :key-fn keyword))

(deftest http-adapter-integration-test
  (testing "Portfolio evaluation endpoint"
    (let [evaluation-service (port/->EvaluationService)
          app (http/create-app evaluation-service)
          request-body {:portfolio [{:ticker "PETR4"
                                     :pl 8.5
                                     :tag_along 80
                                     :corrupcao false}
                                    {:ticker "ITUB4"
                                     :pl 12.3
                                     :tag_along 100
                                     :corrupcao false}]
                        :criterios [{:nome "PL < 10"
                                     :tipo "numerico"
                                     :campo "pl"
                                     :operador "<"
                                     :valor 10
                                     :peso 1.5}
                                    {:nome "Tag Along 100%"
                                     :tipo "booleano"
                                     :campo "tag_along"
                                     :operador "=="
                                     :valor 100
                                     :peso 2.0}
                                    {:nome "Sem escândalos de corrupção"
                                     :tipo "booleano"
                                     :campo "corrupcao"
                                     :operador "=="
                                     :valor false
                                     :peso 3.0}]}
          request (-> (mock/request :post "/api/evaluate")
                      (mock/json-body request-body))
          response (app request)
          body (parse-json-body response)]
      
      (is (= 200 (:status response)))
      (is (vector? (:resultado body)))
      (is (= 2 (count (:resultado body))))
      (is (= "ITUB4" (:ticker (first (:resultado body)))))
      (is (= "PETR4" (:ticker (second (:resultado body)))))
      (is (= 5.0 (:score (first (:resultado body)))))
      (is (= 4.5 (:score (second (:resultado body))))))

    (testing "Health check endpoint"
      (let [evaluation-service (port/->EvaluationService)
            app (http/create-app evaluation-service)
            request (mock/request :get "/health")
            response (app request)
            body (parse-json-body response)]
        
        (is (= 200 (:status response)))
        (is (= "UP" (:status body)))))

    (testing "Invalid request handling"
      (let [evaluation-service (port/->EvaluationService)
            app (http/create-app evaluation-service)
            request (-> (mock/request :post "/api/evaluate")
                        (mock/json-body {:invalid "request"}))
            response (app request)
            body (parse-json-body response)]
        
        (is (= 400 (:status response)))
        (is (contains? body :error))))))