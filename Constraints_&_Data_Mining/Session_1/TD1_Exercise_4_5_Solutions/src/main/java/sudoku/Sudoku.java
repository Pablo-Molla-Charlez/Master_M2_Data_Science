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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * This class solves Sudoku puzzles using the Choco constraint solver.
 * It allows two main approaches:
 * 1) Reading a Sudoku puzzle from an external file
 *   (in our case, we used matrix4.txt, matrix9.txt, matrix16.txt). you can create other files as input matrix data.
 *
 * 2) Using a puzzle defined at runtime (e.g., the user chooses the size,
 *    and the puzzle is generated or left empty for solving).
 */
public class Sudoku {

    /**
     * n: The size of the Sudoku (e.g., 4, 9, 16, etc.).
     */
    static int n;
    /**
     * s: The square root of n (used for sub-grids, e.g., for a 9x9 Sudoku, s = 3).
     */
    static int s;
    /**
     * flag: Used to determine if the puzzle is loaded from file (flag=1)
     *       or if a puzzle is to be defined in code (flag=0).
     */
    static int flag;
    /**
     * instance: Stores the user-selected size of the Sudoku (read from user input or command line).
     */
    private static int instance;
    /**
     * timeout: Sets a time limit (in milliseconds) for the solver to find solutions.
     */
    private static long timeout = 3600000; // one hour

    /**
     * These 2D arrays hold the integer variables for the Sudoku board:
     * rows[i][j] represents the cell in row i, column j.
     * cols[i][j] references the same variables but grouped by columns.
     * shapes[i][j] groups variables according to Sudoku sub-grids.
     */
    IntVar[][] rows, cols, shapes;

    /**
     * The Choco solver model.
     */
    Model model;

    /**
     * A mapping from integer values > 9 to characters (A-Z).
     * This is useful for printing solutions of Sudoku sizes bigger than 9 (e.g., 16x16).
     */
    private static final Map<Integer, Character> valToChar = new HashMap<>();
    static {
        // For values from 10 to 35, map them to letters A-Z (and beyond if needed).
        for (int i = 10; i <= 35; i++ ){
            char c = (char) ('A' + (i - 10));
            valToChar.put(i, c);
        }
    }

    /**
     * Main entry point. It parses command-line arguments, shows help if requested,
     * asks the user for Sudoku size, determines whether to load from file,
     * and then starts the solver.
     */
    public static void main(String[] args) throws ParseException, IOException {

        // Parse command line options
        final Options options = configParameters();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine line = parser.parse(options, args);

        // Check if help mode is requested
        boolean helpMode = line.hasOption("help");
        if (helpMode) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sudoku", options, true);
            System.exit(0);
        }

        // Prompt user to input the size of Sudoku (e.g., 4, 9, 16).
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the size of the Sudoku instance (e.g., 4, 9, 16):");
        instance = scanner.nextInt();

        // Ask user if they want to load the matrix from an external file.
        Scanner scannerInstance = new Scanner(System.in);
        System.out.println("Do you want to load the matrix from an external file? (1 for yes, 0 for no):");
        flag = scannerInstance.nextInt();

        // Check arguments and set up configuration based on user input.
        for (Option opt : line.getOptions()) {
            checkOption(line, opt.getLongOpt());
        }

        // n is the size of the puzzle, s is the size of the sub-grid (sqrt(n)).
        n = instance;
        s = (int) Math.sqrt(n);

        // Execute the solver.
        new Sudoku().solve();
    }

    /**
     * Solve method: builds the model, then iterates through each solution found by the solver.
     * Prints each solution and keeps count of the solutions found.
     */
    public void solve() throws IOException {

        buildModel();
        int nbsol = 0;

        // While the solver can still find solutions, print them.
        while (model.getSolver().solve()) {
            printGrid();
            nbsol++;
            System.out.println("Solution # " + nbsol + " found.\n");
        }
        System.out.println("Number of solutions found: " + nbsol);
        model.getSolver().printStatistics();
    }

    /**
     * Builds the Sudoku model for the Choco solver.
     * 1) Creates the variable arrays (rows, cols, shapes).
     * 2) If flag == 1, reads the puzzle from an external file.
     *    Otherwise, initializes an empty puzzle (which means it will generate all the solutions).
     * 3) Links the shapes array to represent Sudoku sub-grids.
     * 4) Posts all-different constraints for each row, column, and sub-grid.
     */
    public void buildModel() throws IOException {
        model = new Model();

        // Initialize 2D arrays for rows, columns, and sub-grids.
        rows = new IntVar[n][n];
        cols = new IntVar[n][n];
        shapes = new IntVar[n][n];

        // If the user chooses to load puzzle from file:
        if (flag == 1) {
            System.out.println("Please enter the file absolute path for the Sudoku puzzle:");
            Scanner scanner = new Scanner(System.in);
            String filePath = scanner.nextLine();
            int[][] puzzle = readMatrixFromFile(filePath);
            equalOperation(n, puzzle);
        } else {
            // Otherwise, just define all variables (no initial constraints).
            equalOperation(n);
        }

        // Fill 'shapes' to capture the Sudoku sub-grids.
        // For each sub-grid block of size s x s:
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                for (int k = 0; k < s; k++) {
                    for (int l = 0; l < s; l++) {
                        // shapes[j + k*s][i + (l*s)] references the same variables
                        // as rows[l + k*s][i + j*s], effectively grouping them by sub-grid.
                        shapes[j + k * s][i + (l * s)] = rows[l + k * s][i + j * s];
                    }
                }
            }
        }

        // Add the AllDifferent constraints for each row, column, and sub-grid.
        for (int i = 0; i < n; i++) {
            model.allDifferent(rows[i]).post();   // All cells in row i must be distinct
            model.allDifferent(cols[i]).post();   // All cells in column i must be distinct
            model.allDifferent(shapes[i]).post(); // All cells in sub-grid i must be distinct
        }
    }

    /**
     * Prints the Sudoku grid in a stylized manner.
     * Supports printing numbers greater than 9 as letters (A-Z, etc.) for large puzzles.
     */
    public void printGrid() {

        // These strings are used to create a decorative border around rows and columns.
        // The code checks if n > 9 to handle larger Sudoku boards (like 16x16).
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

        // If the puzzle is bigger than 9, adjust the borders to accommodate larger cells.
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

        // Print each row of the Sudoku.
        for (int i = 0; i < n; i++) {

            // Print the decorative border for each row.
            for (int line = 0; line < n; line++) {
                if (line == 0) {
                    System.out.print(i == 0 ? a : b);
                } else if (line == n - 1) {
                    System.out.print(i == 0 ? c : d);
                } else {
                    System.out.print(i == 0 ? e : f);
                }
            }
            System.out.println("");

            // Print the values of each cell in row i.
            System.out.print("│ ");
            for (int j = 0; j < n; j++) {
                if (n < 36){
                    // If the cell value is > 9, print the corresponding letter.
                    if (rows[i][j].getValue() > 9) {
                        System.out.print(esp + valToChar.get(rows[i][j].getValue()) + " │ ");
                    } else {
                        // Otherwise, just print the number.
                        System.out.print(esp + rows[i][j].getValue() + " │ ");
                    }
                } else if(rows[i][j].getValue() > 9){
                    // For puzzles larger than 36, print integer directly.
                    System.out.print(rows[i][j].getValue() + " │ ");
                } else {
                    System.out.print(esp + rows[i][j].getValue() + " │ ");
                }
            }

            // If this is the last row, print the bottom border.
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
     * equalOperation (overload 1): Creates the row and column variables
     * and enforces constraints based on the provided puzzle array.
     * If puzzle[i][j] != 0, it sets the value of that cell to the puzzle value.
     */
    public void equalOperation(int n, int[][] puzzle) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Create the variable for each cell with domain [1..n].
                rows[i][j] = model.intVar("c_" + i + "_" + j, 1, n, false);
                // If the puzzle has a pre-filled value at [i][j], constrain it to that value.
                if (puzzle[i][j] != 0) {
                    model.arithm(rows[i][j], "=", puzzle[i][j]).post();
                }
                // Set columns reference to the same variable.
                cols[j][i] = rows[i][j];
            }
        }
    }

    /**
     * equalOperation (overload 2): Creates the row and column variables
     * with no initial constraints (for an empty puzzle).
     */
    public void equalOperation(int n) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rows[i][j] = model.intVar("c_" + i + "_" + j, 1, n, false);
                cols[j][i] = rows[i][j];
            }
        }
    }

    /**
     * Checks command line options (such as 'instance' and 'timeout')
     * and updates corresponding fields if valid.
     */
    public static void checkOption(CommandLine line, String option) {
        switch (option) {
            case "instance":
                instance = Integer.parseInt(line.getOptionValue(option));
                break;
            case "timeout":
                timeout = Long.parseLong(line.getOptionValue(option));
                break;
            default:
                System.err.println("Bad parameter: " + option);
                System.exit(2);
        }
    }

    /**
     * Sets up the command line options for parsing.
     * - help: show help message
     * - instance: specify the Sudoku instance size
     * - timeout: set a solver timeout
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
                .desc("Sudoku instance size")
                .required(false)
                .build();

        final Option limitOption = Option.builder("t")
                .longOpt("timeout")
                .hasArg(true)
                .argName("timeout in ms")
                .desc("Set the timeout limit to the specified time")
                .required(false)
                .build();

        final Options options = new Options();
        options.addOption(instOption);
        options.addOption(limitOption);
        options.addOption(helpFileOption);

        return options;
    }

    /**
     * Reads a puzzle matrix from a file. Each line in the file represents one row of the Sudoku,
     * with numbers separated by whitespace. A zero represents an empty cell.
     */
    public static int[][] readMatrixFromFile(String filePath) throws IOException {
        List<int[]> matrixList = new ArrayList<>();

        File file = new File(filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            // Read the puzzle line by line.
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()){
                    // Skip empty lines
                    continue;
                }
                String[] values = line.split("\\s+");
                int[] row = new int[values.length];
                for (int i = 0; i < values.length; i++) {
                    row[i] = Integer.parseInt(values[i]);
                }
                matrixList.add(row);
            }
        }

        // Convert the list of rows into a 2D array.
        int[][] matrix = new int[matrixList.size()][];
        for (int i = 0; i < matrixList.size(); i++) {
            matrix[i] = matrixList.get(i);
        }

        return matrix;
    }
}
