import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Utils {
    public static Database loadDataset(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignore lines starting with "#" or empty lines
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                // Split by whitespace (spaces, tabs, etc.)
                String[] tokens = line.trim().split("\\s+");
                Set<String> items = new HashSet<>(Arrays.asList(tokens));

                // Add a new transaction
                transactions.add(new Transaction(items));
            }
        }
        System.out.println("Loaded " + transactions.size() + " transactions from the dataset.");
        return new Database(transactions);
    }
}