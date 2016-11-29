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
