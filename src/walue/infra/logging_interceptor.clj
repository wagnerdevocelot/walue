(ns walue.infra.logging-interceptor
  (:require [walue.infra.logging :as logging])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(defn log-request
  "Interceptor para logar requisições HTTP"
  [uri method]
  (logging/info "Request received:" method uri))

(defn log-response
  "Interceptor para logar respostas HTTP"
  [start-time status]
  (let [duration (- (System/currentTimeMillis) start-time)]
    (logging/info "Request completed in" duration "ms with status" status)))

(defn log-evaluation
  "Interceptor para logar avaliação de portfólio"
  [portfolio criteria]
  (logging/info "Evaluating portfolio with" (count portfolio) "assets and" (count criteria) "criteria"))

(defn log-evaluation-completed
  "Interceptor para logar conclusão da avaliação de portfólio"
  []
  (logging/info "Portfolio evaluation completed"))

(defn log-error
  "Interceptor para logar erros"
  [error-message]
  (logging/error error-message))