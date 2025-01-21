import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AprioriQ5 {
    private double minSupport; // Minimum support threshold
    private int minItemsetSize; // Minimum size of frequent itemsets
    private Database database;

    public AprioriQ5(Database database, double minSupport, int minItemsetSize) {
        this.database = database;
        this.minSupport = minSupport;
        this.minItemsetSize = minItemsetSize;
    }

    // Step 1: Generate frequent 1-itemsets
    public Map<Set<String>, Integer> generateFrequentItemsets() {
        Map<Set<String>, Integer> frequentItemsets = new HashMap<>();

        for (Transaction transaction : database.getTransactions()) {
            for (String item : transaction.getItems()) {
                Set<String> itemSet = Set.of(item);
                frequentItemsets.put(itemSet, frequentItemsets.getOrDefault(itemSet, 0) + 1);
            }
        }

        // Filter by minSupport
        int minCount = (int) Math.ceil(minSupport * database.size());
        frequentItemsets.entrySet().removeIf(entry -> entry.getValue() < minCount);

        return frequentItemsets;
    }

    // Step 2: Generate candidate k-itemsets from (k-1)-itemsets
    public Set<Set<String>> generateCandidates(Set<Set<String>> previousItemsets) {
        Set<Set<String>> candidates = new HashSet<>();
        List<Set<String>> previousList = new ArrayList<>(previousItemsets);

        for (int i = 0; i < previousList.size(); i++) {
            for (int j = i + 1; j < previousList.size(); j++) {
                Set<String> candidate = new HashSet<>(previousList.get(i));
                candidate.addAll(previousList.get(j));

                if (candidate.size() == previousList.get(i).size() + 1) {
                    candidates.add(candidate);
                }
            }
        }

        return candidates;
    }

    // Run Apriori with the Child Operator Based on Lexicographical Order
    public Set<Set<String>> generateCandidatesWithLex(Set<Set<String>> previousItemsets) {
        Set<Set<String>> candidates = new HashSet<>();
        List<Set<String>> previousList = new ArrayList<>(previousItemsets);

        // Sort itemsets lexicographically
        previousList.sort((set1, set2) -> {
            List<String> list1 = new ArrayList<>(set1);
            List<String> list2 = new ArrayList<>(set2);
            Collections.sort(list1);
            Collections.sort(list2);
            return list1.toString().compareTo(list2.toString());
        });

        for (int i = 0; i < previousList.size(); i++) {
            for (int j = i + 1; j < previousList.size(); j++) {
                Set<String> candidate = new TreeSet<>(previousList.get(i));
                candidate.addAll(previousList.get(j));

                if (candidate.size() == previousList.get(i).size() + 1) {
                    candidates.add(candidate);
                }
            }
        }

        return candidates;
    }

    // Step 3: Count support for candidates
    public Map<Set<String>, Integer> countSupport(Set<Set<String>> candidates) {
        Map<Set<String>, Integer> supportCount = new HashMap<>();

        for (Transaction transaction : database.getTransactions()) {
            Set<String> transactionItems = transaction.getItems();
            for (Set<String> candidate : candidates) {
                if (transactionItems.containsAll(candidate)) {
                    supportCount.put(candidate, supportCount.getOrDefault(candidate, 0) + 1);
                }
            }
        }

        return supportCount;
    }

    // Step 4: Filter candidates by support and size
    public Map<Set<String>, Integer> filterCandidates(Map<Set<String>, Integer> candidates) {
        Map<Set<String>, Integer> frequentItemsets = new HashMap<>();
        int minCount = (int) (minSupport * database.size());

        for (Map.Entry<Set<String>, Integer> entry : candidates.entrySet()) {
            if (entry.getValue() >= minCount && entry.getKey().size() >= minItemsetSize) {
                System.out.println("11111");
                frequentItemsets.put(entry.getKey(), entry.getValue());
            }
        }

        return frequentItemsets;
    }

    // Main method to run Apriori
    public List<Map<Set<String>, Integer>> run() {
        List<Map<Set<String>, Integer>> allFrequentItemsets = new ArrayList<>();
        Set<Set<String>> currentCandidates = new HashSet<>();

        // Step 1: Generate frequent 1-itemsets
        Map<Set<String>, Integer> frequent1Itemsets = generateFrequentItemsets();
        allFrequentItemsets.add(frequent1Itemsets);

        currentCandidates = frequent1Itemsets.keySet();

        // Step 2: Iteratively generate higher-order frequent itemsets
        while (!currentCandidates.isEmpty()) {
            Set<Set<String>> candidates = generateCandidates(currentCandidates);
            Map<Set<String>, Integer> supportCounts = countSupport(candidates);
            Map<Set<String>, Integer> frequentItemsets = filterCandidates(supportCounts);

            if (!frequentItemsets.isEmpty()) {
                allFrequentItemsets.add(frequentItemsets);
                currentCandidates = frequentItemsets.keySet();
            } else {
                break;
            }
        }

        return allFrequentItemsets;
    }

    public List<Map<Set<String>, Integer>> runUsingOrdLex() {
        List<Map<Set<String>, Integer>> allFrequentItemsets = new ArrayList<>();
        Set<Set<String>> currentCandidates = new HashSet<>();

        // Step 1: Generate frequent 1-itemsets
        Map<Set<String>, Integer> frequent1Itemsets = generateFrequentItemsets();
        allFrequentItemsets.add(frequent1Itemsets);

        currentCandidates = frequent1Itemsets.keySet();

        // Step 2: Iteratively generate higher-order frequent itemsets
        while (!currentCandidates.isEmpty()) {
            Set<Set<String>> candidates = generateCandidatesWithLex(currentCandidates);
            Map<Set<String>, Integer> supportCounts = countSupport(candidates);
            Map<Set<String>, Integer> frequentItemsets = filterCandidates(supportCounts);

            if (!frequentItemsets.isEmpty()) {
                allFrequentItemsets.add(frequentItemsets);
                currentCandidates = frequentItemsets.keySet();
            } else {
                break;
            }
        }

        return allFrequentItemsets;
    }

    // Compare performance between standard Apriori and optimized Apriori
    public static void comparePerformance(String datasetPath, double minSupport, int minItemsetSize) throws IOException {
        // Load dataset
        Database database = Utils.loadDataset(datasetPath);
        // Standard Apriori
        long startTime = System.currentTimeMillis();
        AprioriQ5 standardApriori = new AprioriQ5(database, minSupport, minItemsetSize);
        List<Map<Set<String>, Integer>> standardResult = standardApriori.run();
        for (int i = 0; i < standardResult.size(); i++) {
            System.out.println("Level " + (i + 1) + ": " + standardResult.get(i));
        }
        long standardTime = System.currentTimeMillis() - startTime;

        // Optimized Apriori (using child+lex)
        startTime = System.currentTimeMillis();
        AprioriQ5 optimizedApriori = new AprioriQ5(database, minSupport, minItemsetSize);
        List<Map<Set<String>, Integer>> optimizedResult = optimizedApriori.runUsingOrdLex();
        for (int i = 0; i < optimizedResult.size(); i++) {
            System.out.println("Level " + (i + 1) + ": " + optimizedResult.get(i));
        }
        long optimizedTime = System.currentTimeMillis() - startTime;

        System.out.println("Standard AprioriQ5 runtime: " + standardTime + " ms");
        System.out.println("Optimized AprioriQ5 runtime: " + optimizedTime + " ms");

        // Verify that the results are consistent
        System.out.println("Are the results the same? " + standardResult.equals(optimizedResult));
    }
}