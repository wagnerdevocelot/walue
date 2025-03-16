(ns walue.domain.evaluation)

(defn- get-field
  "Get a field from an asset, supporting both keyword and string keys"
  [asset field-name]
  (or (get asset (keyword field-name))
      (get asset field-name)))

(defn- get-value
  "Get a value from a map, supporting both keyword and string keys"
  [map key]
  (or (get map (keyword key))
      (get map key)))

(defn- evaluate-numeric-criterion
  "Evaluate a numeric criterion against an asset"
  [asset criterion]
  (let [field-name (or (:campo criterion) (get criterion "campo"))
        field-value (get-field asset field-name)
        criterion-value (or (:valor criterion) (get criterion "valor"))
        operator (or (:operador criterion) (get criterion "operador"))]
    (cond
      (= operator "<") (< field-value criterion-value)
      (= operator ">") (> field-value criterion-value)
      (= operator "<=") (<= field-value criterion-value)
      (= operator ">=") (>= field-value criterion-value)
      (= operator "==") (= field-value criterion-value)
      :else false)))

(defn- evaluate-boolean-criterion
  "Evaluate a boolean criterion against an asset"
  [asset criterion]
  (let [field-name (or (:campo criterion) (get criterion "campo"))
        field-value (get-field asset field-name)
        criterion-value (or (:valor criterion) (get criterion "valor"))]
    (= field-value criterion-value)))

(defn evaluate-criterion
  "Evaluate a single criterion against an asset"
  [asset criterion]
  (let [tipo (or (:tipo criterion) (get criterion "tipo"))]
    (cond
      (= tipo "numerico") (evaluate-numeric-criterion asset criterion)
      (= tipo "booleano") (evaluate-boolean-criterion asset criterion)
      :else false)))

(defn calculate-asset-score
  "Calculate score for a single asset based on all criteria"
  [asset criteria]
  (reduce 
    (fn [score criterion]
      (if (evaluate-criterion asset criterion)
        (+ score (or (:peso criterion) (get criterion "peso")))
        score))
    0
    criteria))

(defn evaluate-portfolio
  "Main domain function that evaluates a portfolio based on criteria"
  [portfolio criteria]
  (let [evaluated-assets (map 
                           (fn [asset]
                             {:ticker (or (:ticker asset) (get asset "ticker"))
                              :score (calculate-asset-score asset criteria)})
                           portfolio)]
    (sort-by :score > evaluated-assets)))