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
