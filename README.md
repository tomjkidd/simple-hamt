# simple-hamt

A Hash Array Mapped Trie toy implementation in Clojure

## Usage

```
(require '[simple-hamt.core :refer [hash-map empty-hash-map get assoc]])

(def hm
  (-> empty-hash-map
    (assoc 0 0)
    (assoc 1 1)))
(get hm 0) ;; => 0
(get hm 1) ;; => 1
```

## License

Copyright © 2016 Tom Kidd

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
