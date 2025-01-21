import java.util.List;

public class Database {
    private List<Transaction> transactions;

    public Database(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int size() {
        return transactions.size();
    }
}