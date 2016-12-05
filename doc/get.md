# Get

## Hash function

Before we talk about what the data structure looks like, it is useful to first visit the concept of a hash function.
For our purposes, a hash function is a function that takes a key and returns a value, called a `hash value`, that can be used to try to find the value associated with that key in the data structure<sup>[1](#footnote-1)</sup>.

The hash function I used takes an 8-bit integer as input and returns that same 8-bit integer as the hash value.
Note that this means that only the integer keys 0-255 can be used as keys (and that there is no restriction on what those key's values are).
This constraint was introduced in order to keep the focus on how things work without introducing flexibility that is unnecessary (just use the native Clojure hash-map!).
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
These segments are used in order to further index into the data structure (there are 4 segments to handle collisions).

## Basic Algorithm

To summarize:
* A key is used with a hash function to get a hash value.
* That hash value is used to create a sequence of segments.
* The sequence of segments are used to access the data structure.

To determine if a key maps to a value, start with the first segment.
Using it's value as an index into the root `:hash-table`, we can check the `:bitmap` to check if a child node is present.
If a `0` is encountered in the bitmap, then we know that the key does not have a value in the hash map.
If a `1` is encountered, we have need to investigate the `:hash-table` and use the node's type to determine how to proceed.
If the node is a `:node` type, we check to make sure the keys are equal, and if so return the node's `:value`, if not return nil (the value is not in the hash map).
If the node is a `:sub-hash` type, we discard the current segment and perform the same steps using the next segment in the sequence with the `:hash-table` in this `:sub-hash` node.

Keeping this in mind, let's see how to use this algorithm using a sample data structure.

## Access the data structure

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

Here is a node visualization of the data structure

![Sample data structure visualization](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure.svg)

The root node is the leftmost node in the image.
As you move right, the relationship of nodes to their children is indicated by the black lines connecting nodes.
Fo each node, the children are ordered top-to-bottom, in index order 0, 1, 2, and 3.
The `:root` and `:sub-hash` nodes are orange, and the `:node` nodes are purple.
There are black X's to show where missing nodes are, this is what makes `:hash-map` a sparse array.
Note the four levels of the tree, below the root node.
The nodes have been given identifier's `A, B, C, D, E, F, G, H, I, and J` to make it easier to talk about them.

### Structure of nodes in sample

Node `A` is the `:root` node
Nodes `C`, `F`, and `H` are `:sub-hash` nodes.
Nodes `B`, `D`, `E`, `G`, `I`, and `J` are `:node` nodes.

The `:bitmap` and `:hash-table` data in each `:sub-hash` node determines the relationship between that node and it's children.
`A` has 3 children, `B`, `C`, and `D`.
`C` has 2 children, `E` and `F`.
`F` has 2 children, `G` and `H`.
`H` has 2 children, `I` and `J`.

This data structure demonstrates use of all four levels of the tree, so we can build some intuition about how to check if a given key is present in the HAMT.

### Meaning of bitmaps

| bitmap (decimal) | bitmap (binary) | Meaning |
|----|--------|-----|
| 14 | 2r1110 | 3 children present (could be :node or :sub-hash nodes). Missing node in 0 position in :hash-table. |
|  3 | 2r0011 | 2 children present. Missing nodes in 2 and 3 positions in :hash-table. |

If we start at the `:root` node, we can use it's `:bitmap` value `14` to determine how many children it has.
The binary value of `14` is `1110`, and this means that `:hash-map` for `:root` contains 3 children at index values 1, 2, and 3.
It also means that there is no child at index 0.

The rest of the `:sub-hash` nodes have a `:bitmap` value `3`, which in binary is `0011`. This means that `:hash-map` for these nodes contains 2 children at index values 0 and 1.

## Determine if key is present

### Present keys

Based on the data structure, we know that the keys `1`, `2`, `3`, `6`, `22`, and `86` are present.
We can convert these keys to their hash values, and then get the segments that represent them.
These results are summarized in the following table.

| key | hash value (decimal) | hash value (binary) |  segments (binary) | segments (decimal) |
|---|---|---|---|
|  1 |  1 | [00 00 00 01] | [01 00 00 00] | [1 0 0 0] |
|  2 |  2 | [00 00 00 10] | [10 00 00 00] | [2 0 0 0] |
|  3 |  3 | [00 00 00 11] | [11 00 00 00] | [3 0 0 0] |
|  6 |  6 | [00 00 01 10] | [10 01 00 00] | [2 1 0 0] |
| 22 | 22 | [00 01 01 10] | [10 01 01 00] | [2 1 1 0] |
| 86 | 86 | [01 01 01 10] | [10 01 01 01] | [2 1 1 1] |

This table and the visualization can help us understand how to find values given keys.

![Sample data structure visualization with access to node B](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure-access1.svg)
*Access to data structure node with key and value 1*

To access `1`, we look at the first segment, which is the value 1.
Starting at the `:root` node, we look at the `:bitmap` to see if there is a child at that index.
`14` corresponds to `2r1110`, which means there is a child in the 1 index of `:hash-table`.
Because index 0 is 0, this means that the first element in `:hash-table` is where we look.
This node has `:node` type, so we check the `:key`, which is equal to the key we are looking for, so `:value` is returned as `1`.


![Sample data structure visualization with access to node E](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure-access2.svg)
*Access to data structure node with key and value 2*

To access `2`, we look at the first segment, which is the value 2.
Starting at the `:root` node, we look at the `:bitmap` to see if there is a child at that index.
`14` corresponds to `2r1110`, which means that there is a child in the 2 index of `:hash-table`.
Index 0 is 0 and index 1 is 1, which means that one child comes before the value, so the second element in `:hash-table` is where we look.
This node has `:sub-hash` type, which means that there is a collision and that we have to use the next segment and repeat the process.
This `:sub-hash` node has a `:bitmap` value of `3`, which corresponds to `2r0011`, meaning that a child is present in the `:hash-table` for index values of 0 and 1.
Segment 0 corresponds to the first element in `:hash-table`.
This node has `:node` type, and it's `:key` matches the key we are searching for, so `:value` is used to return `2`.


![Sample data structure visualization with access to node D](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure-access3.svg)
*Access to data structure node with key and value 3*

To access `3`, we look at the first segment, which is the value 3.
Starting at the `:root` node, we look at the `:bitmap` to see if there is a child at that index.
`14` corresponds to `2r1110`, which means that there is a child in the 3 index of `:hash-table`.
Index 0 is 0, index 1 is 1, and index 2 is 1, which means that two children come before the value, so the third element in `:hash-table` is where we look.
This node has `:node` type, and it's `:key` matches the key we are searching for, so `:value` is used to return `3`.


![Sample data structure visualization with access to node G](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure-access6.svg)
*Access to data structure node with key and value 6*

To access `6`, we look at the first segment, which is the value 2.
Starting at the `:root` node, we look at the `:bitmap` to see if there is a child at that index.
`14` corresponds to `2r1110`, which means that...
TODO: Finish description

![Sample data structure visualization with access to node I](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure-access22.svg)
*Access to data structure node with key and value 22*
TODO: Finish description

![Sample data structure visualization with access to node J](https://cdn.rawgit.com/tomjkidd/simple-hamt/master/doc/images/sample-data-structure-access86.svg)
*Access to data structure node with key and value 86*
TODO: Finish description

*TODO: These procedures are meant to be concrete and easy to follow, but there is a lot of redundancy. Is it possible to remove that?*

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

<a name="footnote-1">[1]</a>: Here we are ignoring the concerns that are usually associated with why we need a hash function in the first place, and seek to just assume that such a function exists and we can just use it.

<a name="footnote-2">[2]</a>: I created the name segment for my own purposes, given that the HAMT paper used index and I didn't want to confuse that with the fact that I use index exclusively to talk about indexing into the `:hash-table` of nodes.
