package sudoku;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import static org.chocosolver.solver.search.strategy.Search.minDomLBSearch;
import static org.chocosolver.util.tools.ArrayUtils.append;

/**
 * GTSudoku is a variant of the traditional Sudoku that includes
 * additional inequality ("greater than" or "less than") constraints
 * between certain adjacent cells, sometimes known as Futoshiki-like constraints.
 */
public class GTSudoku {
    /**
     * n: The size of the Sudoku (e.g., typically 9 for a 9x9).
     */
    static int n;
    /**
     * s: The square root of n (e.g., 3 when n=9).
     */
    static int s;
    /**
     * instance: Indicates the Sudoku instance size.
     *           Here, it is defaulted to 9 if no option is specified.
     */
    private static int instance;
    /**
     * timeout: Time limit (in milliseconds) for solver execution.
     */
    private static long timeout = 3600000; // one hour

    /**
     * rows, cols, shapes: 2D arrays to store Sudoku variables.
     * Each element in rows[i][j] references a Choco IntVar corresponding
     * to the cell in row i, column j.
     */
    IntVar[][] rows, cols, shapes;

    /**
     * The main constraint solver model provided by the Choco library.
     */
    Model model;

    /**
     * Main entry point.
     * 1) Parses command-line arguments.
     * 2) Initializes the size of Sudoku.
     * 3) Solves the GT Sudoku puzzle with the specified constraints.
     */
    public static void main(String[] args) throws ParseException {

        // Configure and parse command line options
        final Options options = configParameters();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine line = parser.parse(options, args);

        // Check if help was requested
        boolean helpMode = line.hasOption("help");
        if (helpMode) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sudoku", options, true);
            System.exit(0);
        }

        // Set default instance size to 9 unless provided as an option
        instance = 9;

        // Check arguments/options (like instance size or timeout)
        for (Option opt : line.getOptions()) {
            checkOption(line, opt.getLongOpt());
        }

        // Initialize puzzle size and sub-grid size
        n = instance;
        s = (int) Math.sqrt(n);

        // Create and solve the puzzle
        new GTSudoku().solve();
    }

    /**
     * Prints the Sudoku grid (including any solution) in a decorative border.
     */
    public void printGrid() {

        // These strings form borders for different parts of the grid.
        // They adapt if n > 9 for larger Sudoku boards.
        String a, aa;
        a = "┌───";
        aa = "┌───";
        String b, bb;
        b = "├───";
        bb = "├───";
        String c, cc;
        c = "─┬────┐";
        cc = "─┬────┐";
        String d, dd;
        d = "─┼────┤";
        dd = "─┼────┤";
        String e, ee;
        e = "─┬───";
        ee = "─┬───";
        String f, ff;
        f = "─┼───";
        ff = "─┼───";
        String g, gg;
        g = "└────┴─";
        gg = "└────┴─";
        String h, hh;
        h = "───┘";
        hh = "───┘";
        String k, kk;
        k = "───┴─";
        kk = "───┴─";
        String esp = " ";

        // If the puzzle is larger than 9x9, reuse the same borders
        // or adapt them as needed (here just reassigning).
        if (n > 9) {
            a = aa;
            b = bb;
            c = cc;
            d = dd;
            e = ee;
            f = ff;
            g = gg;
            h = hh;
            k = kk;
        }

        // Print each row of the puzzle
        for (int i = 0; i < n; i++) {

            // Print horizontal borders
            for (int line = 0; line < n; line++) {
                if (line == 0) {
                    // The first cell in the row
                    System.out.print(i == 0 ? a : b);
                } else if (line == n - 1) {
                    // Last cell in the row
                    System.out.print(i == 0 ? c : d);
                } else {
                    // Intermediate cells
                    System.out.print(i == 0 ? e : f);
                }
            }
            System.out.println("");
            // Print the row content
            System.out.print("│ ");
            for (int j = 0; j < n; j++) {
                if (rows[i][j].getValue() > 9) {
                    System.out.print(rows[i][j].getValue() + " │ ");
                } else {
                    System.out.print(esp + rows[i][j].getValue() + " │ ");
                }
            }

            // If this is the last row, print the bottom border
            if (i == n - 1) {
                System.out.println("");
                for (int line = 0; line < n; line++) {
                    System.out.print(line == 0 ? g : (line == n - 1 ? h : k));
                }
            }
            System.out.println("");
        }
    }

    /**
     * Solves the puzzle by:
     * 1) Building the model with rows, columns, shapes, and inequality constraints.
     * 2) Iterating over each solution found by the solver and printing it.
     */
    public void solve() {

        // Build the constraint model
        buildGTSudoModel();

        int nbsol = 0;
        // As long as a new solution is found, print it
        while (model.getSolver().solve()) {
            printGrid();
            nbsol++;
            System.out.println("Solution # " + nbsol + " found.\n");
        }
        System.out.println("Number of solutions found: " + nbsol);

        // Display final solver statistics
        model.getSolver().printStatistics();
    }

    /**
     * Constructs the "Greater Than Sudoku" model with:
     * 1) Row, column, and sub-grid constraints (all different).
     * 2) Additional inequality constraints.
     */
    public void buildGTSudoModel() {
        model = new Model();

        // Create variable arrays
        rows = new IntVar[n][n];
        cols = new IntVar[n][n];
        shapes = new IntVar[n][n];

        // Define each cell variable with domain [1..n] in rows,
        // then mirror it in cols (so rows[i][j] == cols[j][i]).
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rows[i][j] = model.intVar("c_" + i + "_" + j, 1, n, false);
                cols[j][i] = rows[i][j];
            }
        }

        // Build the shapes array (i.e., sub-grid constraints)
        // We slice the board into sub-grids of size s x s
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                for (int k = 0; k < s; k++) {
                    for (int l = 0; l < s; l++) {
                        shapes[j + k * s][i + (l * s)] = rows[l + k * s][i + j * s];
                    }
                }
            }
        }

        // Post the standard Sudoku constraints:
        // all rows different, all columns different, all sub-grids different
        for (int i = 0; i < n; i++) {
            model.allDifferent(rows[i]).post();
            model.allDifferent(cols[i]).post();
            model.allDifferent(shapes[i]).post();
        }

        // Add extra "greater than / less than" constraints
        addInequalityConstraints();
    }

    /**
     * Defines and posts inequality constraints based on two matrices:
     * 1) horizontalConstraints: relationships between adjacent cells in each row
     * 2) verticalConstraints: relationships between adjacent cells in each column
     */
    private void addInequalityConstraints() {

        // Horizontal adjacency constraints, each row has n-1 possible adjacent pairs
        // null indicates no constraint between that pair.
        String[][] horizontalConstraints = {
                {"<","<",null,"<","<",null,">",">"},
                {"<",">",null,">","<",null,">",">"},
                {">",">",null,"<",">",null,"<",">"},
                {">","<",null,">","<",null,">","<"},
                {">",">",null,"<",">",null,">","<"},
                {"<",">",null,"<",">",null,">","<"},
                {">","<",null,">","<",null,"<",">"},
                {"<",">",null,"<",">",null,">","<"},
                {"<",">",null,">","<",null,"<",">"}
        };

        // Vertical adjacency constraints, each column has n-1 adjacent pairs
        // null means no constraint between those cells.
        String[][] verticalConstraints = {
                {">","<",">","<","<",">",">","<",">"},
                {"<",">","<","<","<",">",">","<",">"},
                {null,null,null,null,null,null,null,null,null},
                {">",">",">","<","<",">",">",">","<"},
                {">","<","<",">",">","<",">",">","<"},
                {null,null,null,null,null,null,null,null,null},
                {"<","<",">",">","<",">","<",">",">"},
                {">",">",">","<","<","<",">","<","<"}
        };

        // Apply horizontal (row-wise) inequality constraints
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n - 1; j++) {
                if (horizontalConstraints[i][j] != null) {
                    if (horizontalConstraints[i][j].equals(">")) {
                        // If ">", enforce rows[i][j] > rows[i][j+1]
                        model.arithm(rows[i][j], ">", rows[i][j + 1]).post();
                    } else if (horizontalConstraints[i][j].equals("<")) {
                        // If "<", enforce rows[i][j] < rows[i][j+1]
                        model.arithm(rows[i][j], "<", rows[i][j + 1]).post();
                    }
                }
            }
        }

        // Apply vertical (column-wise) inequality constraints
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n; j++) {
                if (verticalConstraints[i][j] != null) {
                    if (verticalConstraints[i][j].equals(">")) {
                        // If ">", enforce rows[i][j] > rows[i+1][j]
                        model.arithm(rows[i][j], ">", rows[i + 1][j]).post();
                    } else if (verticalConstraints[i][j].equals("<")) {
                        // If "<", enforce rows[i][j] < rows[i+1][j]
                        model.arithm(rows[i][j], "<", rows[i + 1][j]).post();
                    }
                }
            }
        }
    }

    /**
     * Interprets command-line options for instance size or timeout.
     * If an unknown option is encountered, the program exits with an error.
     */
    public static void checkOption(CommandLine line, String option) {

        switch (option) {
            case "inst":
                instance = Integer.parseInt(line.getOptionValue(option));
                break;
            case "timeout":
                timeout = Long.parseLong(line.getOptionValue(option));
                break;
            default: {
                System.err.println("Bad parameter: " + option);
                System.exit(2);
            }
        }
    }

    /**
     * Sets up command-line option parsing, including:
     * - help: show usage/help
     * - instance: size of the Sudoku (optional)
     * - timeout: solver timeout in ms (optional)
     */
    private static Options configParameters() {

        final Option helpFileOption = Option.builder("h")
                .longOpt("help")
                .desc("Display help message")
                .build();

        final Option instOption = Option.builder("i")
                .longOpt("instance")
                .hasArg(true)
                .argName("sudoku instance")
                .desc("sudoku instance size")
                .required(false)
                .build();

        final Option limitOption = Option.builder("t")
                .longOpt("timeout")
                .hasArg(true)
                .argName("timeout in ms")
                .desc("Set the timeout limit to the specified time")
                .required(false)
                .build();

        // Create the options list
        final Options options = new Options();
        options.addOption(instOption);
        options.addOption(limitOption);
        options.addOption(helpFileOption);

        return options;
    }

    /**
     * An optional method to configure the solver's search strategy.
     * Here, 'minDomLBSearch' sets a heuristic to pick the variable
     * with the smallest domain and assign it the lowest value first.
     */
    public void configureSearch() {
        model.getSolver().setSearch(minDomLBSearch(append(rows)));
    }
}
