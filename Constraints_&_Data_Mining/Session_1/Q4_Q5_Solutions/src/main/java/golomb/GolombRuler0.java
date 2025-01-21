package golomb;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class GolombRuler0 {

    public static void main(String[] args) {



        int m = 8;
// A new model instance
        Model model = new Model("Golomb ruler");

// VARIABLES
// set of marks that should be put on the ruler
        IntVar[] ticks = ticks = model.intVarArray("a", m, 0, m*m, false);

// CONSTRAINTS

        for (int i = 0 ; i < m - 1; i++) {
            // // the mark variables are ordered
            model.arithm(ticks[i + 1], ">", ticks[i]).post();
        }

        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                for (int l = 0; l < m; l++) {
                    for (int k = l + 1; k < m; k++) {
                        if ((i != l || j != k) && (i != k || j != l)) {
                            model.scalar(
                                    new IntVar[]{ticks[j], ticks[i], ticks[k], ticks[l]},
                                    new int[]{1, -1, -1, 1},
                                    "!=", 0
                            ).post();
                        }
                    }
                }
            }
        }

        // SOLVER
        Solver solver = model.getSolver();
        // Show resolution statistics
        solver.showShortStatistics();
        // Find a solution of the CSP version
        solver.solve();

        // Print detailed solver statistics
        solver.printStatistics();


    }


}