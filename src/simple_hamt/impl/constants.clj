(ns simple-hamt.impl.constants)

(def node-types #{:root :sub-hash :node})

(def root-keys #{:type :bitmap :hash-table})

(def sub-hash-keys #{:type :bitmap :hash-table})

(def node-keys #{:type :key :value})

;; Each node can have up to 4 children
(def number-of-children 4)

(def number-of-segments 4)

;; 2 bits can be used to index into 4 children
(def bits-per-segment (int (/ (Math/log number-of-children) (Math/log 2))))
