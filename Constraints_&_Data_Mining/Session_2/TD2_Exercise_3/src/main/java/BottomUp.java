import java.util.*;

public class BottomUp {
    private double minSupport; // Minimum support threshold
    private Database database;

    public BottomUp(Database database, double minSupport) {
        this.database = database;
        this.minSupport = minSupport;
    }

    // Generate all frequent itemsets using a bottom-up approach
    public List<Map<Set<String>, Integer>> run() {
        List<Map<Set<String>, Integer>> allFrequentItemsets = new ArrayList<>();

        // Step 1: Start with single-item sets (level 1)
        Map<Set<String>, Integer> frequent1Itemsets = generateFrequent1Itemsets();
        allFrequentItemsets.add(frequent1Itemsets);

        // Step 2: Generate higher-level itemsets bottom-up
        Map<Set<String>, Integer> currentFrequentItemsets = frequent1Itemsets;

        while (!currentFrequentItemsets.isEmpty()) {
            Set<Set<String>> nextLevelCandidates = generateCandidates(currentFrequentItemsets.keySet());
            Map<Set<String>, Integer> nextLevelFrequentItemsets = countAndFilterCandidates(nextLevelCandidates);

            if (!nextLevelFrequentItemsets.isEmpty()) {
                allFrequentItemsets.add(nextLevelFrequentItemsets);
                currentFrequentItemsets = nextLevelFrequentItemsets;
            } else {
                break;
            }
        }

        return allFrequentItemsets;
    }

    // Generate frequent 1-itemsets
    private Map<Set<String>, Integer> generateFrequent1Itemsets() {
        Map<Set<String>, Integer> itemCounts = new HashMap<>();

        for (Transaction transaction : database.getTransactions()) {
            for (String item : transaction.getItems()) {
                Set<String> itemSet = Set.of(item);
                itemCounts.put(itemSet, itemCounts.getOrDefault(itemSet, 0) + 1);
            }
        }

        return filterByMinSupport(itemCounts);
    }

    // Generate candidate itemsets for the next level
    private Set<Set<String>> generateCandidates(Set<Set<String>> previousItemsets) {
        Set<Set<String>> candidates = new HashSet<>();
        List<Set<String>> itemsetList = new ArrayList<>(previousItemsets);

        for (int i = 0; i < itemsetList.size(); i++) {
            for (int j = i + 1; j < itemsetList.size(); j++) {
                Set<String> unionSet = new HashSet<>(itemsetList.get(i));
                unionSet.addAll(itemsetList.get(j));

                if (unionSet.size() == itemsetList.get(i).size() + 1) {
                    candidates.add(unionSet);
                }
            }
        }

        return candidates;
    }

    // Count and filter candidates by support
    private Map<Set<String>, Integer> countAndFilterCandidates(Set<Set<String>> candidates) {
        Map<Set<String>, Integer> supportCounts = new HashMap<>();

        for (Transaction transaction : database.getTransactions()) {
            for (Set<String> candidate : candidates) {
                if (transaction.getItems().containsAll(candidate)) {
                    supportCounts.put(candidate, supportCounts.getOrDefault(candidate, 0) + 1);
                }
            }
        }

        return filterByMinSupport(supportCounts);
    }

    // Filter itemsets by minimum support
    private Map<Set<String>, Integer> filterByMinSupport(Map<Set<String>, Integer> itemCounts) {
        Map<Set<String>, Integer> frequentItemsets = new HashMap<>();
        int minCount = (int) Math.ceil(minSupport * database.size());

        for (Map.Entry<Set<String>, Integer> entry : itemCounts.entrySet()) {
            if (entry.getValue() >= minCount) {
                frequentItemsets.put(entry.getKey(), entry.getValue());
            }
        }

        return frequentItemsets;
    }

    // Compare performance between Apriori and Bottom-Up
    public static void comparePerformance(Database db, double minSupport) {
        Apriori apriori = new Apriori(db, minSupport);
        BottomUp bottomUp = new BottomUp(db, minSupport);

        long startApriori = System.currentTimeMillis();
        List<Map<Set<String>, Integer>> aprioriResult = apriori.run();
        long endApriori = System.currentTimeMillis();

        long startBottomUp = System.currentTimeMillis();
        List<Map<Set<String>, Integer>> bottomUpResult = bottomUp.run();
        long endBottomUp = System.currentTimeMillis();

        System.out.println("Apriori Time: " + (endApriori - startApriori) + "ms");
        System.out.println("Bottom-Up Time: " + (endBottomUp - startBottomUp) + "ms");

        System.out.println("Apriori Result: " + aprioriResult);
        System.out.println("Bottom-Up Result: " + bottomUpResult);
    }
}