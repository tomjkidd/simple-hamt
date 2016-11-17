(ns simple-hamt.core-test
  (:refer-clojure :exclude [get assoc])
  (:require [clojure.test :refer :all]
            [simple-hamt.core :refer [empty-hash-map get assoc]]))

(deftest collision-test
  (testing
      (let [hm (-> empty-hash-map
                   (assoc 36 36)
                   (assoc 228 228))]
        (is (= 36 (get hm 36)))
        (is (= 228 (get hm 228))))))

(deftest all-keys-test
  (testing
      (let [hm (reduce (fn [acc cur]
                         (assoc acc cur cur))
                       empty-hash-map
                       (range 0 256))]
        (map #(is (= % (get hm %)))))))
