(ns walue.port.evaluation-port
  (:require [walue.domain.evaluation :as evaluation]
            [walue.infra.logging :as logging]))

(defprotocol EvaluationPort
  "Port for portfolio evaluation services"
  (evaluate-portfolio [this portfolio criteria]))

(defrecord EvaluationService []
  EvaluationPort
  (evaluate-portfolio [_ portfolio criteria]
    (logging/info "Evaluating portfolio with" (count portfolio) "assets and" (count criteria) "criteria")
    (let [result (evaluation/evaluate-portfolio portfolio criteria)]
      (logging/info "Portfolio evaluation completed")
      {:resultado result})))