(ns walue.port.evaluation-port
  (:require [walue.domain.evaluation :as evaluation]))

(defprotocol EvaluationPort
  "Port for portfolio evaluation services"
  (evaluate-portfolio [this portfolio criteria]))

(defrecord EvaluationService []
  EvaluationPort
  (evaluate-portfolio [_ portfolio criteria]
    (let [result (evaluation/evaluate-portfolio portfolio criteria)]
      {:resultado result})))