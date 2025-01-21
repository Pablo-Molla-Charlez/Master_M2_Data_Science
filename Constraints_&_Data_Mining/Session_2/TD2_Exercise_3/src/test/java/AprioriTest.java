import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AprioriTest {

    private Database database;
    private Apriori apriori;

    @BeforeEach
    void setUp() {
        // Exemple de base de données de transactions
        List<Transaction> transactions = Arrays.asList(
                new Transaction(Set.of("A", "B", "C")),
                new Transaction(Set.of("A", "C")),
                new Transaction(Set.of("A", "B")),
                new Transaction(Set.of("B", "C")),
                new Transaction(Set.of("A", "B", "C"))
        );
        database = new Database(transactions);
        apriori = new Apriori(database, 0.5); // Seuil de support de 50%
    }

    @Test
    void testGenerateFrequentItemsets() {
        // Implémentation de l'algorithme attendue
        Map<Set<String>, Integer> expected = new HashMap<>();
        expected.put(Set.of("A"), 4);
        expected.put(Set.of("B"), 4);
        expected.put(Set.of("C"), 4);

        // Appel à la méthode
        Map<Set<String>, Integer> result = apriori.generateFrequentItemsets();

        // Vérification des résultats
        assertEquals(expected, result, "Les itemsets fréquents de niveau 1 ne correspondent pas aux attentes.");
    }

    @Test
    void testGenerateCandidates() {
        // Itemsets fréquents de niveau 1
        Set<Set<String>> previousItemsets = Set.of(Set.of("A"), Set.of("B"), Set.of("C"));

        // Résultat attendu
        Set<Set<String>> expected = Set.of(
                Set.of("A", "B"),
                Set.of("A", "C"),
                Set.of("B", "C")
        );

        // Appel à la méthode
        Set<Set<String>> result = apriori.generateCandidates(previousItemsets);

        // Vérification des résultats
        assertEquals(expected, result, "Les candidats générés ne correspondent pas aux attentes.");
    }

    @Test
    void testCountSupport() {
        // Candidats à tester
        Set<Set<String>> candidates = Set.of(
                Set.of("A", "B"),
                Set.of("A", "C"),
                Set.of("B", "C"),
                Set.of("A", "B", "C")
        );

        // Résultat attendu
        Map<Set<String>, Integer> expected = new HashMap<>();
        expected.put(Set.of("A", "B"), 3);
        expected.put(Set.of("A", "C"), 3);
        expected.put(Set.of("B", "C"), 3);
        expected.put(Set.of("A", "B", "C"), 2);

        // Appel à la méthode
        Map<Set<String>, Integer> result = apriori.countSupport(candidates);

        // Vérification des résultats
        assertEquals(expected, result, "Le comptage des supports ne correspond pas aux attentes.");
    }

    @Test
    void testFilterCandidates() {
        // Comptage des supports
        Map<Set<String>, Integer> candidates = new HashMap<>();
        candidates.put(Set.of("A", "B"), 3);
        candidates.put(Set.of("A", "C"), 3);
        candidates.put(Set.of("B", "C"), 3);
        candidates.put(Set.of("A", "B", "C"), 2);

        // Résultat attendu (avec minSupport = 50% de 5 transactions = 2.5)
        Map<Set<String>, Integer> expected = new HashMap<>();
        expected.put(Set.of("A", "B"), 3);
        expected.put(Set.of("A", "C"), 3);
        expected.put(Set.of("B", "C"), 3);
        expected.put(Set.of("A", "B", "C"), 2);

        // Appel à la méthode
        Map<Set<String>, Integer> result = apriori.filterCandidates(candidates);

        // Vérification des résultats
        assertEquals(expected, result, "Les candidats filtrés ne correspondent pas aux attentes.");
    }

    @Test
    void testRun() {
        // Résultats attendus pour les itemsets fréquents
        List<Map<Set<String>, Integer>> expected = new ArrayList<>();

        // Niveau 1
        Map<Set<String>, Integer> level1 = new HashMap<>();
        level1.put(Set.of("A"), 4);
        level1.put(Set.of("B"), 4);
        level1.put(Set.of("C"), 4);
        expected.add(level1);

        // Niveau 2
        Map<Set<String>, Integer> level2 = new HashMap<>();
        level2.put(Set.of("A", "B"), 3);
        level2.put(Set.of("A", "C"), 3);
        level2.put(Set.of("B", "C"), 3);
        expected.add(level2);

        // Niveau 3
        Map<Set<String>, Integer> level3 = new HashMap<>();
        level3.put(Set.of("A", "B", "C"), 2);
        expected.add(level3);

        // Appel à la méthode
        List<Map<Set<String>, Integer>> result = apriori.run();

        // Vérification des résultats
        assertEquals(expected, result, "Les itemsets fréquents générés par Apriori ne correspondent pas aux attentes.");
    }
}