# Motivation

One of the foundational elements of a Computer Science education is the study of data structures. 
This topic opens up ideas related to the effects on performance that different design decisions make.
Thinking about how to arrange data has implications for how easy and fast it can be collected and searched through.
Knowledge of data structures allows you to consider how and why complexity is introduced, and how to make trade-offs to ensure that important operations are efficient. 

The goal of this writing is to gain more depth in understanding data structures in the functional programming context.
I use Clojure a lot these days, and the language provides a wealth of information for building a case against the in-place mutation common to the implementation of common data structures in Java, which is where I first learned to construct data structures via creating classes to hold and connect data.

Rich Hickey's implementation of Clojure's hash maps was based on Phil Bagwell's
'Ideal Hash Trees' paper. After reading the paper and looking at the Java 
implementation for Clojure, I was curious if I really understood how it
worked.

# Constraining the problem

The actual implementation of Clojure's immutable, persistent hash map uses
a tree structure with 32 children per node. While this leads to a wide tree,
which has implications for the performance, I wanted to communicate how this
type of data structure works on a human scale.

I chose to use a tree structure with 4 children per node. This leads to 2 bits
per segment.

I also chose to limit the number of levels in the tree to 4.

Given these constraints, a root node can have up to four children, and each of
those nodes can have up to four children. Because there are only 4 levels, this
leads to a max tree size of 

`4^4 = 256`

I am choosing the value 228 to start the discussion. If you convert 228 to binary,
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

# TODO: Data Structure

# TODO: Get

# TODO: Insert

By choosing two values whos segments are close together, we can work through a
useful scenario.

228, 2r11100100
36,  2r00100100

Note that the fourth segment is where these numbers differ
