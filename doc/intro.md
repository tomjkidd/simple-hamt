# Motivation

One of the foundational elements of a Computer Science education is the study of data structures. 
This topic opens up ideas related to the effects on performance that different design decisions make.
Thinking about how to arrange data has implications for how easy and fast it can be collected and searched through.
Knowledge of data structures allows you to consider how and why complexity is introduced, and how to make trade-offs to ensure that important operations are efficient. 


The goal of this writing is to gain more depth in understanding data structures used in a functional programming context.
Rather than an implementation consisting of in-place updates (the common approach in most languages like Java, which is where I first learned to construct data structures via creating classes to hold and connect data), Clojure uses the idea of structural sharing to allow new structures to be constructed primarily refering to the old structure, while changing just the refereneces necessary to add the change.
I use Clojure a lot these days, and the language provides a wealth of information for building a case against the use of in-place mutation.


Rich Hickey's implementation of Clojure's hash maps was based on Phil Bagwell's [Ideal Hash Trees](http://lampwww.epfl.ch/papers/idealhashtrees.pdf) paper.
After reading the paper and looking at the Java implementation for Clojure, and I figured that I understood the gist of what was going on. But I was curious if I really understood how it worked.


This repository is a constrained Clojure-based implementation of how a HAMT works.
I have maintained some of the naming from the original paper, but my implementation is only inspired by the prose used to describe the algorithms for get/insert.
In fact, the reason I felt that this writing was necessary was because of the brevity of the paper.
I hope that this idea is more accessible to even novice developers as a result of this effort.

# Constraining the problem

First and foremost, I use existing hash-maps from Clojure to handle the underlying
implementation. These hash-maps are used pretty much as records, so it would be
possible to change to records, and attempt a more or less pure Clojure implementation,
but for my purposes, this is not pressing. Hash maps in Clojure are a solved problem
and the goal is for better understanding, not that someone will use this code instead.


The actual implementation of Clojure's immutable, persistent hash-map uses
a tree structure with 32 children per node. While this leads to a wide, shallow tree,
(which has implications for the performance) I wanted to communicate how this
type of data structure works on a scale more easily understood by a human.


I chose to use a tree structure with 4 children per node. This leads to 2 bits
per segment, where a segment is just how many bits are need to uniquely identify
a child.


I also chose to limit the number of levels in the tree to 4.


Given these constraints, a root node can have up to four children, and each of
those nodes can have up to four children. Because there are only 4 levels, this
leads to a max tree size of 

`4^4 = 256`

This seems sufficiently large to provide examples of collisions and how the tree
grows, yet small enough to consider the whole structure even when completely full.

# Data Structure

## Node Types

The data structure is made up of three types of nodes

```clojure
(def node-types #{:root :sub-hash :node})
```
### :root

There is a single `:root` node, which can be accessed with `simple-hamt.core/empty-hash-map`.
This node has a `:bitmap` of 0 and a `:hash-table` of [].
`:bitmap` is an integer that represents the presence of children in `:hash-table`.
`:hash-table` is a sparse array of child nodes.
Note that this means that it is possible to have missing values for some indices.
The number-of-children determines how many bits are needed in `:bitmap`, which is 4 in our case.
Even though no care was taken to make this a word, only the first four bits of `:bitmap` contain relevant information.
This means that the integers 0 - 15 are the only valid bitmap values.

Lets look at 15, a made up bitmap value, to start.

15 can be converted to binary

`1111`

Each bit represents the presence of a child, so to get the number of children that
are contained in `:hash-table`, convert `:bitmap` to binary, and count the number
of `1`s that are there (4 in this case).

The basic idea of the bitmap is that these four bits will tell you where to look for values in the sparse array `:hash-table`.

The Least Significant Bit (rightmost) is where indexing into the array starts.
For each bit, a 1 means a child is present, and a 0 means that there is no child.
So, a 1 in the rightmost position means there is a child at index 0 in `:hash-table`. 
Moving left, the next position indicates there is a child at index 1.
Moving left again, the next position indicates a third child at index 2.
Finally, the leftmost position indicates a fourth child at index 3.

To try another, consider the `:bitmap` 5

5 can be converted to binary

`0101`

This means there are two children in `:hash-table`, one at index 0 and another
at index 2.

*Aside*
TODO: Talk about `:hash-table` only using space for present children and how this contrasts to other schemes that have to create references even for children that aren't present.

Now that we have some idea about how `:bitmap` and `:hash-table` are related, we can talk about the other node types, `:sub-hash` and `:node`.
When any `:hash-table` has children, each child can only be one of these types.

### :sub-hash

The nice thing is, since we have discussed `:root`, we already understand `:sub-hash` nodes.
`:sub-hash` nodes have `:bitmap` and `:hash-table` keys, just like the `:root` node.
The only reason I differentiate between the two is so that it is clear where the root of the tree is.

### :node

`:node` nodes are ones that actually relate to the data you want to store in the tree, which contrast the purpose of `:sub-hash` nodes (which are used for book-keeping and orginization to allow quick access and modification).
These nodes have `:key` and `:value` keys.
`:key` is needed to properly identify matches when retrieving values from the HAMT.
`:value` allows for the values to be stored.

### Data Structure Summary

We have now talked about all of the components that are used as building blocks to assemble our HAMTs.
A `:root` node is used like a container to hold all of our data.
`:sub-hash` nodes are used to grow the data structure as more data is added, allowing more levels of the tree to be created through their `:bitmap` and `:hash-table` keys.
`:node` nodes are used to hold the raw data that is stored in the tree.

Now that we have the building blocks, it is time to talk about how they work.
First we'll take a look at an existing data structure and work through how to access data that is stored in the tree.
This ignores how the data structure was created, with a focus on how to navigate through the tree.
After that, we'll discuss how to create the data structure.

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

--- Aside

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


# TODO: Insert

TODO: Provide examples of collisions and how they are handled.

By choosing two values whos segments are close together, we can work through a
useful scenario.

228, 2r11100100
36,  2r00100100

Note that the fourth segment is where these numbers differ

<a name="footnote-1">1</a>: Here we are ignoring the concerns that are usually associated with why we need a hash function in the first place, and seek to just assume that such a function exists and we can just use it.

<a name="footnote-2">2</a>: I created the name segment for my own purposes, given that the HAMT paper used index and I didn't want to confuse that with the fact that I use index exclusively to talk about indexing into the `:hash-table` of nodes.
