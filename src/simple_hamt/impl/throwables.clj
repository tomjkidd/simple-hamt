(ns simple-hamt.impl.throwables
  (:require [simple-hamt.impl.constants :refer [node-types number-of-segments number-of-children]]))

(defn throw-msg
  [msg]
  (throw (ex-info msg
                  {})))

(defn invalid-hash-key
  [key]
  (when (or
         (not (integer? key))
         (>= key (int (Math/pow number-of-segments number-of-children))))
    (throw-msg (str "simple-hamt: Invalid hash key: " key))))

(defn invalid-node-type
  [{:keys [type] :as hm}]
  (when-not
      (node-types type)
      (throw-msg "simple-hamt: Unknown node type")))

(defn invalid-position
  [position]
  (when-not
      (and (>= position 0)
           (< position number-of-segments))
      (throw-msg "simple-hamt: Invalid position")))
