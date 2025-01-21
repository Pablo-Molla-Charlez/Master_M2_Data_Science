package golomb;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class GolombRuler4 {

    public static void main(String[] args) {

        int m = 8;
        // Create a new model instance
        Model model = new Model("Golomb Ruler");

        // VARIABLES
        // Array of marks to be placed on the ruler
        IntVar[] ticks = model.intVarArray("ticks", m, 0, m*m, false);
        // Array of distances between every pair of distinct marks
        IntVar[] diffs = model.intVarArray("diffs", (m * (m - 1)) / 2, 0, m*m, false);

        // CONSTRAINTS
        // The first mark is positioned at 0
        model.arithm(ticks[0], "=", 0).post();

        // Marks must be ordered, meaning each mark is greater than the previous one
        for (int i = 0, k = 0; i < m - 1; i++) {
            model.arithm(ticks[i + 1], ">", ticks[i]).post();
            // Calculate the distances between each pair of distinct marks
            for (int j = i + 1; j < m; j++, k++) {
                // Declare the constraint for the distance between two marks
                model.scalar(new IntVar[]{ticks[j], ticks[i]}, new int[]{1, -1}, "=", diffs[k]).post();
            }
        }

        // All distances must be distinct
        model.allDifferent(diffs, "BC").post();

        // SOLVER
        Solver solver = model.getSolver();
        // Show resolution statistics
        solver.showShortStatistics();
        // Find the optimal solution by minimizing the position of the last mark
        solver.findOptimalSolution(ticks[m - 1], false);

        // Print detailed solver statistics
        solver.printStatistics();
    }
}