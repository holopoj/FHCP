import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;

import java.io.*;
import java.util.*;

/**
 * Finds pairs of items with a high Pearson correlation coefficient among their transaction sets.  Since set membership
 * is binary, this is more appropriately termed the Phi Correlation.
 * Based on the paper:
 * Zhang, J., & Feigenbaum, J. (2006, November). Finding highly correlated pairs efficiently with powerful pruning.
 * In Proceedings of the 15th ACM international conference on Information and knowledge management (pp. 152-161). ACM.
 */
public class FHCP {
  private double theta = 0.3; // Pairs should have correlation > theta
  // k and t work together to determine the probability of false positives.
  private int k = 1; // number of simultaneous minhashes that need to match

  private double tau = 0.05; // false negative tolerance.  allow no more than this fraction of false negatives.

  // number of independent size k matches that we try, any one of them matching results in candidate
  private int t = (int) Math.ceil(Math.log(tau) / Math.log(1-Math.pow(theta, 2 * k)));

  private int minsup = 10; // each high-correlation item must occur in at least this many transactions

  /**
   * Create class with given parameters.
   * @param theta The minimum Phi correlation that two items should have between their transaction sets.
   * @param tau False negative tolerance.  Allow no more than this fraction of false negatives.
   * @param k Number simultaneous minhashes needed to match.  For large numbers of unique items, k=1 is okay,
   *          for many transactions and small number of unique item types, large k values may be reasonable.
   * @param minsup The minimum number of transactions two items must occur in together to be considered correlated.
   */
  public FHCP(double theta, double tau, int k, int minsup) {
    this.theta = theta;
    this.tau = tau;
    this.k = k;
    this.minsup = minsup;
    this.t = (int) Math.ceil(Math.log(tau) / Math.log(1-Math.pow(theta, 2 * k)));
  }

  /** Default values probably won't work well for your dataset, but sometimes you just don't want to think about it. */
  public FHCP() {
    this(0.3, 0.05, 1, 5);
  }

  public static LabeledTransactions readTransactions(InputStream is) {
    BufferedReader inBuf = new BufferedReader(new InputStreamReader(is));
    String line;
    try {
      LabeledTransactions transactions = new LabeledTransactions();
      while ((line = inBuf.readLine()) != null) {
        transactions.addTransaction(Splitter.on(" ").omitEmptyStrings().splitToList(line));
      }
      return transactions;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Returns the computed value for t, which is the number of groupings of k hashes we will compute and check. */
  public int getT() {
    return t;
  }

  /**
   * Finds items whose phi correlation between their binary transaction appearance vectors is above {@code theta}.
   * @param transactions Sets of items.  We're looking for items that tend to appear together in the same sets. The
   *                     transactions do not need to be in any particular order, nor do the items within them.
   */
  public List<IntPair> findCorrelatedPairs(List<int[]> transactions) {
    int maxItemID = transactions.stream().mapToInt(t -> Ints.max(t)).max().getAsInt();
    return findCorrelatedPairs(transactions, maxItemID);
  }

  /** Finds items whose phi correlation between their binary transaction appearance vectors is above {@code theta}.
   * @param transactions Sets of items.  We're looking for items that tend to appear together in the same sets. The
   *                     transactions do not need to be in any particular order, nor do the items within them.
   * @param maxItemID Greater or equal to the highest item id from any transaction, but don't make it too big or we'll
   *                  waster memory.
   * @return
   */
  public List<IntPair> findCorrelatedPairs(List<int[]> transactions, int maxItemID) {
    long start = System.currentTimeMillis();

    HashFunction hash = Hashing.murmur3_128();

    Multiset<IntPair> pairCounts = HashMultiset.create();
    Multiset<Integer> singleCounts = HashMultiset.create();

    // Compute minhashes for each item. We're minhashing the set of transactions the item occurs in, to compute
    // Jaccard sim between R(i1) and R(i2), where R(i) is the set of transactions i occurs in.
    int[][] H = new int[maxItemID+1][k * t]; // [item][min-hash u]
    // Initialize to MAX_VALUE since we're tracking min values in this array.
    for (int hRow = 0; hRow < H.length; hRow++) {
      for (int hCol = 0; hCol < H[hRow].length; hCol++) {
        H[hRow][hCol] = Integer.MAX_VALUE;
      }
    }
    for (int j = 0; j < transactions.size(); j++) {
      int[] trans = transactions.get(j);
      int[] copy = Ints.concat(trans);
      Arrays.sort(copy);
      for (int c1 = 0; c1 < copy.length-1; c1++) {
        for (int c2 = c1+1; c2 < copy.length; c2++) {
          pairCounts.add(new IntPair(copy[c1], copy[c2]));
        }
      }
      for (int i : trans) {
        singleCounts.add(i);

        // Using trick from Guava's BloomFilter class based on "Less Hashing, Same Performance: Building a Better Bloom Filter"
        // by Adam Kirsch and Michael Mitzenmacher.  Can get away with only two hash functions.  Not 100% sure the trick is
        // applicable here though.
        long hash64 = hash.hashInt(j).asLong();
        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);
        for (int u = 0; u < k * t; u++) {
          int h = hash1 + (u * hash2);
          if (h < H[i][u]) {
            H[i][u] = h;
          }
        }
      }
    }

    // Now build buckets corresponding to items that had matching minhashes on k different hashes.
    Set<IntPair> candidatePairs = new HashSet<>();
    for (int v = 0; v < t; v++) {
      Map<Integer,BitSet> buckets = new HashMap<>();
      for (int i = 0; i < H.length; i++) {
        Hasher hasher = hash.newHasher();
        for (int g = v * this.k; g < (v + 1) * this.k; g++) {
          hasher.putInt(H[i][g]);
        }
        int bucket = hasher.hash().asInt();
        if ( ! buckets.containsKey(bucket)) { buckets.put(bucket, new BitSet()); }
        buckets.get(bucket).set(i);
      }
      for (BitSet cands : buckets.values()) {
        for (int j1 = cands.nextSetBit(0); j1 != -1; j1 = cands.nextSetBit(j1 + 1)) {
          for (int j2 = cands.nextSetBit(j1+1); j2 != -1; j2 = cands.nextSetBit(j2 + 1)) {
            candidatePairs.add(new IntPair(j1, j2));
            if (j2 == Integer.MAX_VALUE) break; // prevent overflow
          }
          if (j1 == Integer.MAX_VALUE) break; // prevent overflow
        }
      }
    }

    // Confirm which pairs are truly correlated
    List<IntPair> confirmedCorrelated = new ArrayList<>();
    for (IntPair candPair : candidatePairs) {
      if (pairCounts.count(candPair) < minsup) continue; // TODO
      double N = transactions.size();;
      double spAB = pairCounts.count(candPair) / N;
      double spA = singleCounts.count(candPair.i1) / N;
      double spB = singleCounts.count(candPair.i2) / N;
      double phiAB = (spAB - spA * spB) / Math.sqrt(spA * spB * (1 - spA) * (1 - spB));
      if (phiAB > theta) {
        candPair.setPhi(phiAB);
        confirmedCorrelated.add(candPair);
      }
    }

    System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms.");

    return confirmedCorrelated;
  }

  /** Needed so we can hash pairs of int's */
  public static class IntPair implements Comparable<IntPair> {
    final int i1;
    final int i2;
    double phi = -1;

    public IntPair(int i1, int i2) {
      this.i1 = i1;
      this.i2 = i2;
    }

    public void setPhi(double phi) {
      this.phi = phi;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      IntPair intPair = (IntPair) o;

      if (i1 != intPair.i1) return false;
      return i2 == intPair.i2;
    }

    @Override
    public int hashCode() {
      int result = i1;
      result = 31 * result + i2;
      return result;
    }

    @Override
    public int compareTo(IntPair o) {
      return Double.compare(this.phi, o.phi);
    }
  }

  /**
   * Tracks the item id -- label mapping.
   */
  public static class LabeledTransactions {
    List<int[]> transactions = new ArrayList<>();
    BiMap<String,Integer> labels = HashBiMap.create();

    public void addTransaction(List<String> trans) {
      int[] intTrans = new int[trans.size()];
      for (int i = 0; i < trans.size(); i++) {
        intTrans[i] = getIdOrAdd(trans.get(i));
      }
      transactions.add(intTrans);
    }

    private int getIdOrAdd(String s) {
      if ( ! labels.containsKey(s)) {
        labels.put(s, labels.size());
      }
      return labels.get(s);
    }
  }

  /** Read in the file specified and print pairs of items with high phi correlation */
  public static void main(String... args) throws IOException {
    LabeledTransactions labeledTransxs = readTransactions(new FileInputStream(args[0]));
    int maxItem = labeledTransxs.transactions.stream().mapToInt(t -> Ints.max(t)).max().getAsInt();

    List<IntPair> correlatedPairs = new FHCP().findCorrelatedPairs(labeledTransxs.transactions, maxItem);
    correlatedPairs.stream().sorted(Comparator.reverseOrder())
            .limit(100)
            .forEachOrdered(pair -> {
              System.out.printf("%20s %20s : %.3f\n",
                      labeledTransxs.labels.inverse().get(pair.i1),
                      labeledTransxs.labels.inverse().get(pair.i2),
                      pair.phi);
            });
  }
}
