import java.util.Set;

public class Transaction {
    private Set<String> items;

    public Transaction(Set<String> items) {
        this.items = items;
    }

    public Set<String> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}