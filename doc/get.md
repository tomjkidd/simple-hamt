# Get

## Hash function

Before we talk about what the data structure looks like, it is useful to first visit the concept of a hash function.
For our purposes, a hash function is a function that takes a key and returns a value, called a `hash value`, that can be used to try to find the value associated with that key<sup>[1](#footnote-1)</sup>.

The hash function I used takes an 8-bit integer as input and returns that same 8-bit integer as the hash value.
Note that this means that only the integer keys 0-255 can be used as keys (and that there is no restriction on what those key's values are).
This constraint was introduced in order to keep the focus on how things work without introducing flexibility that is uneccessary (just use the native clojure hash-map!).
This integer is then used to determine how to look through the data structure for the value represented by the key.

## Segments

Once we have a hash value, the next concept needed is that of a segment<sup>[2](#footnote-2)</sup>.
There is more to be said about segments, with respect to creating the data structure, but that discussion will follow later.
For now, it is enough just to know how to calculate what the segments are for a given hash value.

To get the segments for a hash value, convert the hash value to binary and then break it into 2-bit segments from right-to-left.

For example, take the value `54`, which is `2r00110110` in binary.
This corresponds to the sequence of segments of [2r10 2r01 2r11 2r00].
Note that this is equivalent to popping the last two bits of the remaining bits at a time, and using those two bits, in order, as each segment's value.
Each hash value creates a sequence of 4 2-bit segments.
These segments are used in order to further index into the data structure.

## Access the data structure

To summarize, a key is used with a hash function to get a hash value.
That hash value is used to create a sequence of segments.
The sequence of segments are used to access the data structure.

The following data structure serves as an example that will allow us to understand how to navigate through a HAMT to determine if a given key is present.

```clojure
(def sample-data-structure
  {:type :root,
   :bitmap 14,
   :hash-table [{:type :node,
                 :key 1,
                 :value 1}
                {:type :sub-hash,
                 :bitmap 3,
                 :hash-table [{:type :node,
                               :key 2,
                               :value 2}
                              {:type :sub-hash,
                               :bitmap 3,
                               :hash-table [{:type :node,
                                             :key 6,
                                             :value 6}
                                            {:type :sub-hash,
                                             :bitmap 3,
                                             :hash-table [{:type :node,
                                                           :key 22,
                                                           :value 22}
                                                          {:type :node,
                                                           :key 86,
                                                           :value 86}]}]}]}
                {:type :node,
                 :key 3,
                 :value 3}]})

```

## Structure of nodes in sample

First, notice the single `:root` node.
It has a `:bitmap` and `:hash-table`, as described above.
The `:hash-table` contains 3 children, 2 of which are `:node` nodes, and a `:sub-hash` node.
Moving to the single `:sub-hash` node, it's `:hash-table` has 2 children, 1 `:node` and 1 `:sub-hash`.
Following this `:sub-hash` leads to another `:hash-table` with 2 children, 1 `:node` and 1 `:sub-hash`.
Finally, the inner-most `:sub-hash` has 2 `:node`s.
This data structure demonstrates use of all four levels of the tree, so we can build some intuition about how to check if a given key is present in the HAMT.

## Meaning of nodes

| bitmap (decimal) | bitmap (binary) | Meaning |
|----|--------|-----|
| 14 | 2r1110 | 3 children present (:node or :sub-hash nodes). Missing node in 0 position in :hash-table. |
|  3 | 2r0011 | 2 children present. Missing nodes in 2 and 3 positions in :hash-table. |

If we start at the `:root` node, we can use it's `:bitmap` value `14` to determine how many children it has.
The binary value of `14` is `1110`, and this means that `:hash-map` for `:root` contains 3 children at index values 1, 2, and 3.
It also means that there is no child at index 0.

## Determine if key is present

### Present keys

### Missing keys

## TODO: Exercises

* TODO: Create a data strucute where some of the :bitmaps are missing and have them filled in

* TODO: Create a data structure without the :key present in nodes, use :bitmaps to determine if values are present, and if so, what are the keys?

## TODO: Answers

```clojure

```

*Aside*

If you convert 228 to binary,
it can be segmented as follows (Most significant to least significant from left to right)

`11 10 01 00`

This allows me to first introduce the concept of segments. Using a 0-based index,
we are using four segments (2 bits each), where the index refers to the group
of bits, from right to left. Based on how I chose 228, each index is the same
as it's segment.

```clojure
(def hash-value (hash 228)) ;; => 228, 2r11100100
(get-hash-segment hash-value 0) ;; => 0, 2r00
(get-hash-segment hash-value 1) ;; => 1, 2r01
(get-hash-segment hash-value 2) ;; => 2, 2r10
(get-hash-segment hash-value 3) ;; => 3, 2r11
```

<a name="footnote-1">1</a>: Here we are ignoring the concerns that are usually associated with why we need a hash function in the first place, and seek to just assume that such a function exists and we can just use it.

<a name="footnote-2">2</a>: I created the name segment for my own purposes, given that the HAMT paper used index and I didn't want to confuse that with the fact that I use index exclusively to talk about indexing into the `:hash-table` of nodes.
