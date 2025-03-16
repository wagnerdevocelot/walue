(ns walue.infra.fitness
  (:require [clojure.tools.namespace.find :as find]
            [clojure.java.io :as io]
            [clojure.tools.namespace.file :as file]
            [clojure.tools.namespace.parse :as parse]
            [clojure.string :as str])
  (:import [java.io File]))

;; Definições de camadas arquiteturais
(def layers
  {:domain #{"walue.domain"}
   :port #{"walue.port"}
   :adapter #{"walue.adapter"}
   :infra #{"walue.infra"}
   :core #{"walue.core"}})

;; Regras de dependência permitidas entre camadas
(def allowed-deps
  {:domain #{}  ;; Domain não deve depender de nenhuma outra camada
   :port #{:domain} ;; Port pode depender apenas de Domain
   :adapter #{:domain :port} ;; Adapter pode depender de Domain e Port
   :infra #{} ;; Infra não deve depender de outras camadas (exceto clojure.*)
   :core #{:domain :port :adapter :infra}}) ;; Core pode depender de todas as camadas

(defn- ns-to-layer
  "Determina a qual camada um namespace pertence"
  [ns-sym]
  (let [ns-str (str ns-sym)]
    (first
     (for [[layer prefixes] layers
           :when (some #(str/starts-with? ns-str %) prefixes)]
       layer))))

(defn- get-clj-files
  "Retorna todos os arquivos .clj no diretório"
  [dir]
  (find/find-clojure-sources-in-dir (io/file dir)))

(defn- extract-namespace-and-deps
  "Extrai o namespace e suas dependências de um arquivo .clj"
  [file]
  (when-let [ns-decl (file/read-file-ns-decl file)]
    (when-let [ns-name (parse/name-from-ns-decl ns-decl)]
      [ns-name (parse/deps-from-ns-decl ns-decl)])))

(defn check-layer-dependencies
  "Verifica se as dependências entre camadas seguem as regras definidas"
  []
  (let [src-dir "src"
        files (get-clj-files src-dir)
        ns-deps (keep extract-namespace-and-deps files)
        violations (atom [])]

    (doseq [[ns-name deps] ns-deps]
      (let [src-layer (ns-to-layer ns-name)]
        (when src-layer
          (doseq [dep deps]
            (let [dep-layer (ns-to-layer dep)]
              (when (and dep-layer
                         (not (contains? (get allowed-deps src-layer) dep-layer))
                         (not= src-layer dep-layer))
                (swap! violations conj
                       {:source ns-name
                        :source-layer src-layer
                        :dependency dep
                        :dependency-layer dep-layer})))))))

    {:valid? (empty? @violations)
     :violations @violations}))

(defn check-domain-purity
  "Verifica se o domínio não tem dependências externas além da stdlib do Clojure"
  []
  (let [src-dir "src/walue/domain"
        files (get-clj-files src-dir)
        ns-deps (keep extract-namespace-and-deps files)
        violations (atom [])]

    (doseq [[ns-name deps] ns-deps]
      (doseq [dep deps]
        (let [dep-str (str dep)]
          (when-not (or (str/starts-with? dep-str "clojure.")
                        (str/starts-with? dep-str "walue.domain"))
            (swap! violations conj
                   {:namespace ns-name
                    :external-dependency dep})))))

    {:valid? (empty? @violations)
     :violations @violations}))

(defn check-circular-dependencies
  "Verifica a existência de dependências circulares"
  []
  (let [src-dir "src"
        files (get-clj-files src-dir)
        ns-deps (into {} (keep extract-namespace-and-deps files))
        visited (atom #{})
        path (atom [])
        cycles (atom [])]

    (letfn [(dfs [ns]
              (when-not (contains? @visited ns)
                (swap! visited conj ns)
                (swap! path conj ns)

                (doseq [dep (get ns-deps ns)]
                  (if (some #{dep} @path)
                    ;; Encontrou um ciclo
                    (let [cycle-start (.indexOf @path dep)
                          cycle (conj (vec (drop cycle-start @path)) dep)]
                      (swap! cycles conj cycle))
                    ;; Continue DFS
                    (dfs dep)))

                (swap! path pop)))]

      (doseq [ns (keys ns-deps)]
        (reset! path [])
        (dfs ns)))

    {:valid? (empty? @cycles)
     :cycles @cycles}))

(defn check-interface-isolation
  "Verifica se as interfaces (portas) estão corretamente isoladas"
  []
  (let [src-dir "src/walue/port"
        files (get-clj-files src-dir)
        violations (atom [])]

    (doseq [file files]
      (let [content (slurp file)
            file-name (.getName file)]
        (when-not (re-find #"defprotocol" content)
          (swap! violations conj
                 {:file file-name
                  :reason "Port file should define at least one protocol"}))))

    {:valid? (empty? @violations)
     :violations @violations}))

(defn check-adapter-implementation
  "Verifica se os adaptadores implementam portas"
  []
  (let [src-dir "src/walue/adapter"
        files (get-clj-files src-dir)
        violations (atom [])]

    (doseq [file files]
      (let [content (slurp file)
            file-name (.getName file)
            has-port-dependency (or
                                 (re-find #"require.*\[walue\.port" content)
                                 (re-find #"walue\.port\.[a-z-]+\s+:as" content))]
        (when-not has-port-dependency
          (swap! violations conj
                 {:file file-name
                  :reason "Adapter should depend on at least one port"}))))

    {:valid? (empty? @violations)
     :violations @violations}))

(defn run-fitness-checks
  "Executa todas as verificações de fitness e retorna o resultado completo"
  []
  (let [layer-deps-result (check-layer-dependencies)
        domain-purity-result (check-domain-purity)
        circular-deps-result (check-circular-dependencies)
        interface-result (check-interface-isolation)
        adapter-result (check-adapter-implementation)
        all-valid? (and (:valid? layer-deps-result)
                        (:valid? domain-purity-result)
                        (:valid? circular-deps-result)
                        (:valid? interface-result)
                        (:valid? adapter-result))]

    {:all-valid? all-valid?
     :layer-dependencies layer-deps-result
     :domain-purity domain-purity-result
     :circular-dependencies circular-deps-result
     :interface-isolation interface-result
     :adapter-implementation adapter-result}))

(defn -main
  "Ponto de entrada para verificação de fitness da arquitetura"
  [& args]
  (let [results (run-fitness-checks)
        all-valid? (:all-valid? results)]

    (println "\n=== Architectural Fitness Check Results ===\n")

    ;; Verifica dependências entre camadas
    (let [{:keys [valid? violations]} (:layer-dependencies results)]
      (println "Layer Dependencies Check:" (if valid? "PASSED ✓" "FAILED ✗"))
      (when-not valid?
        (println "  Violations:")
        (doseq [v violations]
          (println (str "  - " (:source v) " (" (name (:source-layer v)) ") depends on "
                       (:dependency v) " (" (name (:dependency-layer v)) ")")))))

    ;; Verifica pureza do domínio
    (let [{:keys [valid? violations]} (:domain-purity results)]
      (println "\nDomain Purity Check:" (if valid? "PASSED ✓" "FAILED ✗"))
      (when-not valid?
        (println "  Violations:")
        (doseq [v violations]
          (println (str "  - " (:namespace v) " depends on external " (:external-dependency v))))))

    ;; Verifica dependências circulares
    (let [{:keys [valid? cycles]} (:circular-dependencies results)]
      (println "\nCircular Dependencies Check:" (if valid? "PASSED ✓" "FAILED ✗"))
      (when-not valid?
        (println "  Cycles detected:")
        (doseq [cycle cycles]
          (println (str "  - " (str/join " -> " cycle))))))

    ;; Verifica isolamento de interfaces
    (let [{:keys [valid? violations]} (:interface-isolation results)]
      (println "\nInterface Isolation Check:" (if valid? "PASSED ✓" "FAILED ✗"))
      (when-not valid?
        (println "  Violations:")
        (doseq [v violations]
          (println (str "  - " (:file v) ": " (:reason v))))))

    ;; Verifica implementação de adaptadores
    (let [{:keys [valid? violations]} (:adapter-implementation results)]
      (println "\nAdapter Implementation Check:" (if valid? "PASSED ✓" "FAILED ✗"))
      (when-not valid?
        (println "  Violations:")
        (doseq [v violations]
          (println (str "  - " (:file v) ": " (:reason v))))))

    (println "\nOverall Result:" (if all-valid? "PASSED ✓" "FAILED ✗"))

    (System/exit (if all-valid? 0 1))))