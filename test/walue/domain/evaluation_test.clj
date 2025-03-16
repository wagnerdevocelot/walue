(ns walue.domain.evaluation-test
  (:require [clojure.test :refer :all]
            [walue.domain.evaluation :as evaluation]))

(deftest test-evaluate-criterion
  (testing "Numeric criterion evaluation"
    (let [asset {:pl 8.5}
          criterion {:tipo "numerico" :campo "pl" :operador "<" :valor 10.0}]
      (is (true? (evaluation/evaluate-criterion asset criterion))))

    (let [asset {:pl 12.3}
          criterion {:tipo "numerico" :campo "pl" :operador "<" :valor 10.0}]
      (is (false? (evaluation/evaluate-criterion asset criterion))))

    (let [asset {:pl 10.0}
          criterion {:tipo "numerico" :campo "pl" :operador "==" :valor 10.0}]
      (is (true? (evaluation/evaluate-criterion asset criterion)))))

  (testing "Boolean criterion evaluation"
    (let [asset {:corrupcao false}
          criterion {:tipo "booleano" :campo "corrupcao" :operador "==" :valor false}]
      (is (true? (evaluation/evaluate-criterion asset criterion))))

    (let [asset {:tag_along 100}
          criterion {:tipo "booleano" :campo "tag_along" :operador "==" :valor 100}]
      (is (true? (evaluation/evaluate-criterion asset criterion))))))

(deftest test-calculate-asset-score
  (testing "Score calculation for asset meeting all criteria"
    (let [asset {:ticker "PETR4" :pl 8.5 :tag_along 100 :corrupcao false}
          criteria [{:nome "PL < 10" :tipo "numerico" :campo "pl" :operador "<" :valor 10 :peso 1.5}
                    {:nome "Tag Along 100%" :tipo "booleano" :campo "tag_along" :operador "==" :valor 100 :peso 2.0}
                    {:nome "Sem escândalos de corrupção" :tipo "booleano" :campo "corrupcao" :operador "==" :valor false :peso 3.0}]]
      (is (= 6.5 (evaluation/calculate-asset-score asset criteria)))))

  (testing "Score calculation for asset meeting some criteria"
    (let [asset {:ticker "PETR4" :pl 11.0 :tag_along 80 :corrupcao false}
          criteria [{:nome "PL < 10" :tipo "numerico" :campo "pl" :operador "<" :valor 10 :peso 1.5}
                    {:nome "Tag Along 100%" :tipo "booleano" :campo "tag_along" :operador "==" :valor 100 :peso 2.0}
                    {:nome "Sem escândalos de corrupção" :tipo "booleano" :campo "corrupcao" :operador "==" :valor false :peso 3.0}]]
      (is (= 3.0 (evaluation/calculate-asset-score asset criteria))))))

(deftest test-evaluate-portfolio
  (testing "Portfolio evaluation and ranking"
    (let [portfolio [{:ticker "PETR4" :pl 8.5 :tag_along 80 :corrupcao false}
                     {:ticker "ITUB4" :pl 12.3 :tag_along 100 :corrupcao false}]
          criteria [{:nome "PL < 10" :tipo "numerico" :campo "pl" :operador "<" :valor 10 :peso 1.5}
                    {:nome "Tag Along 100%" :tipo "booleano" :campo "tag_along" :operador "==" :valor 100 :peso 2.0}
                    {:nome "Sem escândalos de corrupção" :tipo "booleano" :campo "corrupcao" :operador "==" :valor false :peso 3.0}]
          result (evaluation/evaluate-portfolio portfolio criteria)]
      (is (= 2 (count result)))
      (is (= "ITUB4" (:ticker (first result))))
      (is (= "PETR4" (:ticker (second result))))
      (is (= 5.0 (:score (first result))))
      (is (= 4.5 (:score (second result)))))))