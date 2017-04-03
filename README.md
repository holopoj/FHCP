# FHCP
Java implementation of the paper "Finding Highly Correlated Pairs with Powerful Pruning" by
Zhang, J., & Feigenbaum, J. (2006, November). *In Proceedings of the 15th ACM international conference on Information and knowledge management* (pp. 152-161). ACM. [pdf](http://www.cs.yale.edu/homes/jf/ZF.pdf)

The only dependency is [Guava](https://github.com/google/guava).

Licensed under the Apache 2.0 License.


The input are many transactions (aka baskets), each of which is a set of items.  This algorithm will efficiently find items that tend to occur together in the same transactions.  Technically, this is finding all pairs of items whose phi coefficient [Wikipedia](https://en.wikipedia.org/wiki/Phi_coefficient) is above a specified threshold.  This correlation is measured on the sets of transactions the two items occur in.  These transactions can be any sets, such as baskets of products a user purchased or words in documents.  This project provides sets of ingredients used by recipes, borrowed from the Kaggle [What's Cooking](https://www.kaggle.com/c/whats-cooking) dataset in the file kaggle_whats_cookin.txt.

Here are pairs of ingredients sorted only by the number of ingredients they occur in together.  
```
          ingredient1          ingredient2 freq
 ==============================================
                 salt               onions 4392
                 salt            olive_oil 4180
                 salt                water 3960
               pepper                 salt 3844
               garlic                 salt 3749
                 salt    all-purpose_flour 3079
                 salt                sugar 3061
                 salt        garlic_cloves 2998
                 salt               butter 2777
                 salt  ground_black_pepper 2737
               garlic               onions 2659
               onions            olive_oil 2207
            olive_oil        garlic_cloves 2103
                 salt        vegetable_oil 2101
               garlic            olive_oil 2080
                water               onions 1974
                 salt                 eggs 1919
                 salt         black_pepper 1792
                 salt           large_eggs 1700
                 salt             tomatoes 1682
               onions        garlic_cloves 1643
               garlic                water 1605
                water                sugar 1576
                 salt         ground_cumin 1533
  ground_black_pepper            olive_oil 1520
  ```

While this listing is useful, it does not account for the fact that certain ingredients are more common than others and will therefore occur with many other ingredients purely because they are both frequent. That's why "salt" shows up so often.
