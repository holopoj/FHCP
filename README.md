# FHCP
Java implementation of the paper "Finding Highly Correlated Pairs with Powerful Pruning" by
Zhang, J., & Feigenbaum, J. (2006, November). *In Proceedings of the 15th ACM international conference on Information and knowledge management* (pp. 152-161). ACM. [pdf](http://www.cs.yale.edu/homes/jf/ZF.pdf)

The only dependency is [Guava](https://github.com/google/guava).

Licensed under the Apache 2.0 License.


The input are many transactions (aka baskets), each of which is a set of items.  This algorithm will efficiently find items that tend to occur together in the same transactions.  Technically, this is finding all pairs of items whose $\phi$ correlation [Wikipedia](https://en.wikipedia.org/wiki/Phi_coefficient) is above a specified threshold.  This correlation is measured on the sets of transactions the two items occur in.
