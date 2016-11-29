# TODO: Insert

TODO: Provide examples of collisions and how they are handled.

By choosing two values whos segments are close together, we can work through a
useful scenario.

228, 2r11100100
36,  2r00100100

Note that the fourth segment is where these numbers differ

<a name="footnote-1">1</a>: Here we are ignoring the concerns that are usually associated with why we need a hash function in the first place, and seek to just assume that such a function exists and we can just use it.

<a name="footnote-2">2</a>: I created the name segment for my own purposes, given that the HAMT paper used index and I didn't want to confuse that with the fact that I use index exclusively to talk about indexing into the `:hash-table` of nodes.
