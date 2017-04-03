# Overview
Java implementation of the paper "Finding Highly Correlated Pairs with Powerful Pruning" by
Zhang, J., & Feigenbaum, J. (2006, November). *In Proceedings of the 15th ACM international conference on Information and knowledge management* (pp. 152-161). ACM. [pdf](http://www.cs.yale.edu/homes/jf/ZF.pdf)

The only dependency is [Guava](https://github.com/google/guava).

Licensed under the Apache 2.0 License.


The input are many transactions (aka baskets), each of which is a set of items.  This algorithm will efficiently find items that tend to occur together in the same transactions.  Technically, this is finding all pairs of items whose phi coefficient [Wikipedia](https://en.wikipedia.org/wiki/Phi_coefficient) is above a specified threshold.  This correlation is measured on the sets of transactions the two items occur in.  These transactions can be any sets, such as baskets of products a user purchased or words in documents.  This project provides sets of ingredients used by recipes, extracted from the Kaggle [What's Cooking](https://www.kaggle.com/c/whats-cooking) dataset in the file `kaggle_whats_cookin.txt`.  That data has one listing of ingredient per line and looks like this:

```romaine_lettuce black_olives grape_tomatoes garlic pepper purple_onion seasoning garbanzo_beans feta_cheese_crumbles
plain_flour ground_pepper salt tomatoes ground_black_pepper thyme eggs green_tomatoes yellow_corn_meal milk vegetable_oil
eggs pepper salt mayonaise cooking_oil green_chilies grilled_chicken_breasts garlic_powder yellow_onion soy_sauce butter chicken_livers
...
```

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

While this listing is useful, it does not account for the fact that certain ingredients are more common than others and will therefore occur with many other ingredients purely because they are both frequent. That's why "salt" shows up so often.  Let's use the FHCP algorithm to find only pairs of ingredients that occur together at least ten times, and whose phi coefficient is at least 0.3:

```
        ingredient1           ingredient2   Phi
 ==============================================
      green_cardamom       brown_cardamom : 0.460
          warm_water     active_dry_yeast : 0.453
              pastry     single_crust_pie : 0.450
       garlic_powder         onion_powder : 0.436
           soy_sauce           sesame_oil : 0.431
               mirin                 sake : 0.409
          buttermilk          baking_soda : 0.408
              wasabi          nori_sheets : 0.405
  kaffir_lime_leaves             galangal : 0.393
      green_cardamom          shahi_jeera : 0.385
        garlic_paste         ginger_paste : 0.379
       baking_powder          baking_soda : 0.368
          lemongrass             galangal : 0.367
       baking_powder    all-purpose_flour : 0.364
     cinnamon_sticks                clove : 0.348
 dried_bonito_flakes                konbu : 0.341
          warm_water            dry_yeast : 0.337
        ground_cumin     ground_coriander : 0.336
          cumin_seed      coriander_seeds : 0.329
        garam_masala     coriander_powder : 0.328
          mascarpone          ladyfingers : 0.321
               konbu        bonito_flakes : 0.319
        garlic_paste     coriander_powder : 0.307
          cumin_seed      ground_turmeric : 0.306
        garam_masala      ground_turmeric : 0.305
        rice_noodles          beansprouts : 0.304
     light_soy_sauce       dark_soy_sauce : 0.301
          lemongrass   kaffir_lime_leaves : 0.301
     ground_turmeric     coriander_powder : 0.301
```

Much more interesting.  The pairs make sense, yeast needs warm water to be activated, and many of the pairs reflect common regional pairings.

While the phi coefficient is a good measure of associativity, and it is equal to Pearson correlation for binary data, is proportional to the Chi square test, and relates to Jaccard similarity, it isn't the only one.  There are other measures of associativity on 2x2 contingency tables, such as Fisher's eact test and the g-score.  The algorithm implemented here uses Minhashes to find items whose transaction sets have high Jaccard similarity in a first pass.  Then the phi coefficient is calculated in a second pass.  This second pass could easily be replaced by one that caclulated an alternative associativity measure.

# Usage
The algorithm has four parameters:
<dl>
  <dt>theta</dt>
  <dd>The minimum Phi correlation that two items should have between their transaction sets. (e.g., 0.3)</dd>
  
  <dt>tau</dt>
  <dd>False negative tolerance.  Allow no more than this fraction of false negatives. (e.g., 0.05 means 5%)</dd>
  
  <dt>minsup</dt>
  <dd>The minimum number of transactions two items must occur in together to be considered correlated. (e.g., 10)</dd>
  
  <dt>k</dt>
  <dd>Number simultaneous minhashes needed to match.  For large numbers of unique items such as in the recipe dataset, k=1 is okay.  For many transactions and small number of unique item types, large k values may be reasonable.</dd>
</dl>


Everything is implemented in `FHCP.java`, which has a `main()` method that takes in the data file name:

`java -classpath . FHCP kaggle_whats_cookin.txt`

A great library for frequent itemset mining is [SPMF](http://www.philippe-fournier-viger.com/spmf/).
