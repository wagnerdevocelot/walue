(defproject walue "0.1.0-SNAPSHOT"
  :description "Portfolio evaluation service using Domain-Driven Design and Hexagonal Architecture"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.namespace "1.4.4"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [ring/ring-json "0.5.1"]]
  :main ^:skip-aot walue.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[ring/ring-mock "0.4.0"]
                                  [clj-http "3.12.3"]]}})