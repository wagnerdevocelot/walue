(ns walue.infra.fitness-test
  (:require [clojure.test :refer :all]
            [walue.infra.fitness :as fitness]))

(deftest test-architectural-fitness
  (testing "Layer dependencies compliance"
    (let [result (fitness/check-layer-dependencies)]
      (is (:valid? result)
          (str "Layer dependencies violated: "
               (pr-str (:violations result))))))

  (testing "Domain purity"
    (let [result (fitness/check-domain-purity)]
      (is (:valid? result)
          (str "Domain purity violated: "
               (pr-str (:violations result))))))

  (testing "No circular dependencies"
    (let [result (fitness/check-circular-dependencies)]
      (is (:valid? result)
          (str "Circular dependencies detected: "
               (pr-str (:cycles result))))))

  (testing "Interface isolation"
    (let [result (fitness/check-interface-isolation)]
      (is (:valid? result)
          (str "Interface isolation violated: "
               (pr-str (:violations result))))))

  (testing "Adapter implementation"
    (let [result (fitness/check-adapter-implementation)]
      (is (:valid? result)
          (str "Adapter implementation violated: "
               (pr-str (:violations result)))))))