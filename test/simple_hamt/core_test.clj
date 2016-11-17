(ns simple-hamt.core-test
  (:refer-clojure :exclude [get assoc])
  (:require [clojure.test :refer :all]
            [simple-hamt.core :refer [empty-hash-map get assoc]]))

(defn- assoc-reducer
  "Commonly used test function"
  [acc cur]
  (assoc acc cur cur))

(deftest collision-test
  "This test was created to serve as a basic example of a HAMT that handles a known collision"
  (testing
      (let [hm (-> empty-hash-map
                   (assoc 36 36)
                   (assoc 228 228))]
        (is (= 36 (get hm 36)))
        (is (= 228 (get hm 228))))))

(deftest all-keys-test
  "A basic test that all valid keys can store and retrieve their values"
  (testing
      (let [hm (reduce assoc-reducer
                       empty-hash-map
                       (range 0 256))]
        (doall (map #(is (= % (get hm %))) (range 0 256))))))

(deftest all-keys-random-order
  "To ensure there are no insertion order dependencies with the HAMT"
  (testing
      (let [shuffled (shuffle (range 0 256))
            hm (reduce assoc-reducer
                       empty-hash-map
                       shuffled)]
        (doall (map #(is (= % (get hm %))) (range 0 256))))))

(deftest matching-segments-after-collision
  "This was created after a bug was discovered trying to get all-keys-random-order to pass.

  The issue was that inserting 23 after 15 was causing multiple calls to the code that handles
  collisions because 15 and 23 have a segment that matches after the collision. This test is
  meant to serve as a simple example of collision handling/order depenendent insert."
  (testing
      (let [idxs [15 23 0 20]
            hm (reduce (fn [acc cur]
                         (assoc acc cur cur))
                       empty-hash-map
                       idxs)]
        (doall (map #(is (= % (get hm %))) (shuffle idxs))))))
