(ns walue.core
  (:require [ring.adapter.jetty :as jetty]
            [walue.adapter.http-adapter :as http]
            [walue.port.evaluation-port :as port]
            [walue.infra.logging :as logging]
            [walue.infra.metrics :as metrics])
  (:gen-class))

(def server-atom (atom nil))

(defn start-server [port]
  (let [evaluation-service (port/->EvaluationService)
        app (http/create-app evaluation-service)
        metrics (metrics/init-metrics)
        server (jetty/run-jetty app {:port port :join? false})]
    (reset! server-atom server)
    (logging/info "Server started on port" port)
    server))

(defn stop-server []
  (when-let [server @server-atom]
    (.stop server)
    (reset! server-atom nil)
    (logging/info "Server stopped")))

(defn- get-env-var [name default]
  (if-let [value (System/getenv name)]
    value
    default))

(defn- get-port []
  (let [port-str (get-env-var "PORT" "8080")]
    (try
      (Integer/parseInt port-str)
      (catch NumberFormatException _
        (logging/warn "Invalid PORT environment variable value:" port-str "- using default 8080")
        8080))))

(defn- get-log-level []
  (let [level-str (get-env-var "LOG_LEVEL" "info")]
    (try
      (keyword level-str)
      (catch Exception _
        (println "Invalid LOG_LEVEL environment variable value:" level-str "- using default :info")
        :info))))

(defn -main [& args]
  (let [port (if (seq args)
               (try
                 (Integer/parseInt (first args))
                 (catch NumberFormatException _
                   (get-port)))
               (get-port))
        log-level (get-log-level)]
    (logging/set-log-level! log-level)
    (logging/info "Starting server with log level:" log-level)
    (start-server port)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable stop-server))))