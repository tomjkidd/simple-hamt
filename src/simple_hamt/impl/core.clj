(ns simple-hamt.impl.core
  (:refer-clojure :exclude [hash])
  (:require [simple-hamt.impl.constants :refer [number-of-children number-of-segments bits-per-segment]]
            [simple-hamt.impl.throwables :as throwables]))

;; NOTE: the :key is needed to ensure that a match isn't just a LSB partial match.

;; NOTE: Only integer keys are used, in order to keep the hash function simple.

;; NOTE: Everything here is meant to be written with ease of understanding first,
;; and then optimization.

(defn hash*
  "The hash function that turns a key into hash-value"
  [key]
  (throwables/invalid-hash-key key)
  
  (mod key (int (Math/pow number-of-children number-of-segments))))

(def empty-hash-map*
  {:type :root
   :bitmap 2r00000000
   :hash-table []})

(defn- get-hash-segment
  "Hash values are broken into n-bit segments, each segment is an index into the hash-value
   
  The segments move from left (LSB) to right (MSB)"
  [hash-value segment]
  (let [mask (reduce (fn [acc cur]
                       (bit-set acc cur))
                     0
                     (range 0 bits-per-segment))
        shifted (bit-shift-right hash-value (* segment bits-per-segment))]
    (bit-and mask shifted)))

(defn- segment-seq
  "Get a sequence of the segments (LSB to MSB) of a hash-value"
  [hash-value]
  (map #(get-hash-segment hash-value %) (range 0 number-of-segments)))

(defn- in-bitmap?
  "Determines if the 0-based child index (position) is active in bitmap

  This is used to determine which elements are present in hash-table, the sparse array used for child nodes in the HAMT"
  [bitmap position]
  (throwables/invalid-position position)
  
  (->> (bit-shift-right bitmap position)
       (bit-and 1)
       (= 1)))

(defn- get-hash-table-index
  "Get the bitmask dependent index into a node hash-table

  Here bitmap represents children that are present, so this provides an index into hash-table, the sparse array used for child nodes in the HAMT"
  [bitmap position]
  (reduce (fn [acc cur]
            (if (bit-test bitmap cur)
              (inc acc)
              acc))
          0
          (range 0 position)))

(defn- update-bitmap
  "Update the bitmap to include a 1 in the 0-based index position

  NOTE: 0 -> 1, 1 -> 2, 2 -> 4, 3 -> 8, etc."
  [bitmap index]
  (throwables/invalid-position index)
  (bit-or bitmap (int (Math/pow 2 index))))

(defn get*
  "Retrieve a value from the HAMT.

  NOTE: This uses hash-val, the result of applying the hash* function the the desired key"
  [hm {:keys [hash-val seg-index] :as state}]
  (throwables/invalid-node-type hm)

  (let [{:keys [bitmap hash-table]} hm
        index (get-hash-segment hash-val seg-index)
        empty? (not (in-bitmap? bitmap index))]
    (if empty?
      nil
      (let [hash-table-index (get-hash-table-index bitmap index)
            {:keys [type key value]
             :as node} (clojure.core/nth hash-table
                                         hash-table-index)]
        (cond
          (= type :node)
          ;; NOTE: This check ensures that if two keys share the same segment that
          ;; only one of them uniquely maps to the value.
          (if (= key hash-val)
            value
            nil)

          (= type :sub-hash)
          (get* node
                {:hash-val hash-val
                 :seg-index (inc seg-index)}))))))

(defn- insert
  "Insert a child inot hash-table of the current node

  Manages the bitmap as well, but does not handle collisions!"
  [{:keys [bitmap hash-table] :as hm} index k v]
  (let [new-bitmap (update-bitmap bitmap index)
        new-hash-table (->> (conj hash-table {:type :node
                                              :key k
                                              :value v})
                            (sort-by :key)
                            (into []))
        new-hm (assoc hm
                      :bitmap new-bitmap
                      :hash-table new-hash-table)]
    new-hm))

(defn- build-subhash
  "Creates a new node to properly handle a collision.
  
  Handles the conflict when assoc'ing a new node has a collision"
  [old-node new-node seg-index]
  (let [old-segs (drop seg-index (segment-seq (hash* (:key old-node))))
        new-segs (drop seg-index (segment-seq (hash* (:key new-node))))
        segs (map vector old-segs new-segs (range))
        sh (reduce (fn [acc [o n i]]
                     (if (= o n)
                       ;; Collision, will need to create a :sub-hash node, but defer the actual work
                       (if (:finished acc)
                         acc
                         (update acc :colliding-segs #(conj % o)))
                       (let [subhash {:type :sub-hash
                                      :bitmap 0
                                      :hash-table []}
                             
                             subhash-with-old
                             (insert subhash o (:key old-node) (:value old-node))
                             
                             subhash-with-old-and-new
                             (insert subhash-with-old n (:key new-node) (:value new-node))
                             
                             new-subhash-node
                             (loop [rem (:colliding-segs acc)
                                    sh subhash-with-old-and-new]
                               (if (nil? (first rem))
                                 sh
                                 (recur (rest rem) {:type :sub-hash
                                                    :bitmap (update-bitmap 0 (first rem))
                                                    :hash-table [sh]})))]
                         (assoc acc
                                :finished true
                                :subhash-node new-subhash-node))))
                   {:colliding-segs '()
                    :finished false
                    :subhash-node nil}
                   segs)]
    (:subhash-node sh)))

(defn assoc*
  "Associate a key and value into the HAMT"
  [hm k v hash-val seg-index]
  (throwables/invalid-node-type hm)

  (let [{:keys [bitmap hash-table]} hm
        index (get-hash-segment hash-val seg-index)
        collision-detected? (in-bitmap? bitmap index)]
    (if (not collision-detected?)
      (insert hm index k v)
      (let [hash-table-index (get-hash-table-index bitmap index)
            {:keys [type key value]
             :as collision-node} (clojure.core/nth hash-table
                                         hash-table-index)]
        (cond
          (= type :node)
          (if (= k key)
            (let [new-node (assoc collision-node :value v)
                  new-hash-table (assoc hash-table hash-table-index new-node)]
              (assoc hm :hash-table new-hash-table))
            
            (let [old-node collision-node
                  new-node {:type :node
                            :key k
                            :value v}
                  new-subhash-node (build-subhash old-node new-node (inc seg-index))
                  new-hash-table (assoc hash-table hash-table-index new-subhash-node)]
              (assoc hm :hash-table new-hash-table)))

          (= type :sub-hash)
          (let [new-subhash-node (assoc* collision-node k v hash-val (inc seg-index))
                new-hash-table (assoc hash-table hash-table-index new-subhash-node)]
            (assoc hm :hash-table new-hash-table)))))))
