(ns simple-hamt.core
  (:refer-clojure :exclude [get assoc hash-map])
  (:require [simple-hamt.impl.core :refer [get* assoc* empty-hash-map* hash*]]))

(def empty-hash-map
  empty-hash-map*)

(defn get
  [hm k]
  (let [hash-val (hash* k)]
    (get* hm {:hash-val hash-val
              :seg-index 0})))

(defn assoc
  [hm k v]
  (let [hash-val (hash* k)]
    (assoc* hm k v hash-val 0)))

(defn hash-map
  [& kvs]
  (let [map-entries (partition 2 kvs)]
    (reduce (fn [acc [k v]]
              (assoc acc k v))
            empty-hash-map
            map-entries)))
