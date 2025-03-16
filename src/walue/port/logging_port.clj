(ns walue.port.logging-port)

(defprotocol LoggingPort
  "Porta para serviços de logging"
  (log-info [this message] "Registra uma mensagem informativa")
  (log-warn [this message] "Registra uma mensagem de aviso")
  (log-error [this message] "Registra uma mensagem de erro")
  (log-debug [this message] "Registra uma mensagem de debug"))

(defrecord LoggingService []
  LoggingPort
  (log-info [_ message]
    (println (str "{\"level\":\"info\",\"message\":\"" message "\"}")))
  (log-warn [_ message]
    (println (str "{\"level\":\"warn\",\"message\":\"" message "\"}")))
  (log-error [_ message]
    (println (str "{\"level\":\"error\",\"message\":\"" message "\"}")))
  (log-debug [_ message]
    (println (str "{\"level\":\"debug\",\"message\":\"" message "\"}"))))