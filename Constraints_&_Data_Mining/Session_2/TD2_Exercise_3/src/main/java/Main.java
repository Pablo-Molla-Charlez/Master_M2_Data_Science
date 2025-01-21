import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception{
        // Example database
//        List<Transaction> transactions = Arrays.asList(
//                new Transaction(Set.of("A", "B", "C")),
//                new Transaction(Set.of("A", "C")),
//                new Transaction(Set.of("A", "B")),
//                new Transaction(Set.of("B", "C")),
//                new Transaction(Set.of("A", "B", "C"))
//        );
        List<Transaction> D2 = Arrays.asList(
                new Transaction(Set.of("A","C","D")),
                new Transaction(Set.of("B","C","E")),
                new Transaction(Set.of("A","B","C","E")),
                new Transaction(Set.of("B","E")),
                new Transaction(Set.of("A","B","C","E")),
                new Transaction(Set.of("B","C","E"))
                );

        Database db = new Database(D2);

        // Run Apriori
        //Apriori apriori = new Apriori(db, 0.5); // 50% minimum support
        // Q1
        int support = 3;
        double minSupport = (double) support / db.size();
        Apriori apriori = new Apriori(db,minSupport);
        List<Map<Set<String>, Integer>> frequentItemsets = apriori.run();

        // Display results
        System.out.println("======Q1 Frequent Itemsets:======");
        for (int i = 0; i < frequentItemsets.size(); i++) {
            System.out.println("Level " + (i + 1) + ": " + frequentItemsets.get(i));
        }
        //Q2
        List<Map<Set<String>, Integer>> frequentItemsetsQ2 = apriori.runUsingOrdLex();
        // Display results
        System.out.println("======Q2 Frequent Itemsets:======");
        for (int i = 0; i < frequentItemsetsQ2.size(); i++) {
            System.out.println("Level " + (i + 1) + ": " + frequentItemsetsQ2.get(i));
        }

        //Q3, load datasets,
        /**
         * According to the results, we find that in the case of a large amount of data, the lower the support,
         * the more frequent terms, and the shorter the time efficiency of the optimization algorithm
         */
        System.out.println("======Q3 compare the Performance of the two versions======");
        String dataSetPath = "/Users/chenchenjunjie/m2/dm_cp/fim-pablo-junjie/datasets/BMS.txt";
        Apriori.comparePerformance(dataSetPath,0.05);

        //Q4 Compare the apriori algo and bottomUp algo
        System.out.println("======Q4 compare the apriori algo and bottomUp algo======");
        String dataSetPathQ4 = "/Users/chenchenjunjie/m2/dm_cp/fim-pablo-junjie/datasets/mushroom.txt";
        Database db_ext = Utils.loadDataset(dataSetPathQ4);
        BottomUp.comparePerformance(db_ext,0.4);

        //choose small datasets
        dataSetPathQ4 = "/Users/chenchenjunjie/m2/dm_cp/fim-pablo-junjie/datasets/vote.txt";
        db_ext = Utils.loadDataset(dataSetPathQ4);
        BottomUp.comparePerformance(db_ext,0.4);


        //Q5 Revise the Apriori algorithm to extract only frequent itemsets with a size greater than a specified value size.
        // Implement this modified version.
        System.out.println("======Q5 Revise the Apriori algorithm to extract only frequent itemsets======");
        dataSetPath = "/Users/chenchenjunjie/m2/dm_cp/fim-pablo-junjie/datasets/BMS.txt";
        AprioriQ5.comparePerformance(dataSetPath,0.05,7);

    }
}