(ns walue.infra.logging
  (:require [clojure.data.json :as json]
            [clojure.string :as str])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(def log-level (atom :info))

(def log-levels {:debug 0
                 :info 1
                 :warn 2
                 :error 3})

(defn- should-log? [level]
  (>= (get log-levels level) (get log-levels @log-level)))

(defn- get-timestamp []
  (.format (LocalDateTime/now) (DateTimeFormatter/ISO_LOCAL_DATE_TIME)))

(defn- format-log-entry [level message]
  (let [log-entry {:timestamp (get-timestamp)
                   :level (name level)
                   :message (if (string? message)
                              message
                              (str/join " " message))}]
    (json/write-str log-entry)))

(defn set-log-level! [level]
  (reset! log-level level))

(defn log [level & message]
  (when (should-log? level)
    (println (format-log-entry level message))))

(defn debug [& message]
  (apply log :debug message))

(defn info [& message]
  (apply log :info message))

(defn warn [& message]
  (apply log :warn message))

(defn error [& message]
  (apply log :error message))