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
