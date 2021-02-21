import java.util.*;

/**
 * Generates a Mathdoku puzzle
 */
public class Generator {

    // The logical representation of the current puzzle
    private Logic gameLogic;

    private int[][] generatedBoard;

    // Tracks if a cell is already included in a cage
    private boolean[][] isInCage;

    // The size of the board to be generated
    private int boardSize;

    private Random random;

    /**
     * Constructs a generator for the current board
     * @param board the logical representation of the current board
     */
    public Generator(Logic board) {

        gameLogic = board;
        random = new Random();
    }

    /**
     * Generates a Mathdoku puzzle of the specified board size
     * @param boardSize the number of rows/columns for the generated puzzle to have
     */
    public void generateBoard(int boardSize) {

        this.boardSize = boardSize;
        generatedBoard = new int[boardSize][boardSize];
        isInCage = new boolean[boardSize][boardSize];

        gameLogic.getBoard().setSize(boardSize);
        gameLogic.getBoard().resizeBoard();

        // Deletes the last game board
        gameLogic.deleteLastBoard();

        generateLatinSquare();
        randomiseBoard();

        // Creates the cages
        gameLogic.setClusterCells(generateCages());
        gameLogic.setUpWalls();

        // Sets the solution to generated board
        gameLogic.getSolver().setSolvedBoard(generatedBoard);
    }

    /**
     * Generates a latin square the size of the board (numbers in rows and columns don't repeat)
     */
    private void generateLatinSquare() {

        for (int row = 0; row < boardSize; row++) {
            for (int column = 0; column < boardSize; column++) {

                generatedBoard[row][column] = (row + column) % boardSize + 1;
            }
        }
    }

    /**
     * Randomises the rows and columns of the latin square to create a random board
     */
    private void randomiseBoard() {

        int index;
        int temporary;

        // Randomise rows
        for(int row = boardSize - 1; row > 0; row--) {

            index = random.nextInt(row + 1);

            // Swap rows
            for(int increment = 0; increment < boardSize; increment++) {

                temporary = generatedBoard[row][increment];
                generatedBoard[row][increment] = generatedBoard[index][increment];
                generatedBoard[index][increment] = temporary;
            }
        }

        // Randomise columns
        for(int column = boardSize - 1; column > 0; column--) {

            index = random.nextInt(column + 1);

            // Swap columns
            for(int increment = 0; increment < boardSize; increment++) {

                temporary = generatedBoard[increment][column];
                generatedBoard[increment][column] = generatedBoard[increment][index];
                generatedBoard[increment][index] = temporary;
            }
        }
    }

    /**
     * Randomly generates cages for the board
     * @return the cluster cells of generated cages
     */
    private ArrayList<List<Logic.CellPos>> generateCages() {

        ArrayList<List<Logic.CellPos>> clusterCells = new ArrayList<>();

        for (int row = 0; row < boardSize; row++) {
            for (int column = 0; column < boardSize; column++) {

                if(!isInCage[row][column]) {
                    clusterCells.add(generateCage(row, column));
                }
            }
        }

        // FIXME: 4/26/2020 make solution unique

        return clusterCells;
    }

    /**
     * Randomly generates a single cage
     * @param row the row of the starting cell of the cage
     * @param column the column of the starting cell of the cage
     * @return the cluster cells of the generated cage
     */
    private List<Logic.CellPos> generateCage(int row, int column) {

        // Label will be attached to the first cell
        int firstCellRow = row;
        int firstCellColumn = column;

        // The cluster cells of the final cage
        List<Logic.CellPos> cage = new ArrayList<>();

        // Helper data structure to ensure a more random board
        LinkedList<Logic.CellPos> neighbors = new LinkedList<>();

        // Generate the size of the cage, maximum size being the number of rows/columns in the board
        int cageSize = generateCageSize(Math.min(boardSize, findClusterSize(row, column)));

        isInCage[row][column] = true;

        cage.add(new Logic.CellPos(row, column));
        neighbors.addLast(new Logic.CellPos(row, column));

        // Adds a cells from random directions into the cluster until the cage size is reached
        while(cage.size() < cageSize) {

            // The order in which the nearby cells will be added to the cage. Values between 0-3
            int[] addSequence = constructRandomSequence();

            row = neighbors.getFirst().getRow();
            column = neighbors.getFirst().getColumn();

            // Add in each direction
            for(int index = 0; index < addSequence.length; index++) {

                if (cage.size() == cageSize) {
                    break;
                }

                switch (addSequence[index]) {

                    case 0:
                        addTop(neighbors, cage, row, column);
                        break;

                    case 1:
                        addRight(neighbors, cage, row, column);
                        break;

                    case 2:
                        addBottom(neighbors, cage, row, column);
                        break;

                    case 3:
                        addLeft(neighbors, cage, row, column);
                }
            }

            neighbors.addLast(neighbors.removeFirst());
        }

        gameLogic.getBoard().getBoardLayout()[firstCellRow][firstCellColumn].setLabel(generateLabel(cage));

        return cage;
    }

    /**
     * Randomly generates the size of the cage
     * @param maximumSize the maximum size the cage can be
     * @return the generated size of the board
     */
    private int generateCageSize(int maximumSize) {

        /*
        The chances for each size of the board:
        1 1-5 (5%)
        2 6 - 20 (15%)
        3 21 - 45 (25%)
        4 46 - 70 (25%)
        5 71 - 86 (15%)
        6 86 - 94 (9%)
        7 95 - 97 (3%)
        8 98 - 100 (3%)
         */
        int oneChance = 5;
        int twoChance = 20;
        int threeChance = 45;
        int fourChance = 70;
        int fiveChance = 86;
        int sixChance = 94;
        int sevenChance = 97;
        int eightChance = 100;

        while(true) {

            int chances = random.nextInt(100) + 1;

            if (chances <= oneChance) {
                return 1;
            } else if (chances <= twoChance && maximumSize > 1) {
                return 2;
            } else if (chances <= threeChance && maximumSize > 2) {
                return 3;
            } else if (chances <= fourChance && maximumSize > 3) {
                return 4;
            } else if (chances <= fiveChance && maximumSize > 4) {
                return 5;
            } else if (chances <= sixChance && maximumSize > 5) {
                return 6;
            } else if (chances <= sevenChance && maximumSize > 6) {
                return 7;
            } else if (chances <= eightChance && maximumSize > 7) {
                return 8;
            }
        }
    }

    /**
     * Find the maximum number of cluster cells from a specified cell
     * @param row the row of the staring cell
     * @param column the column of the staring cell
     * @return the maximum cluster size
     */
    private int findClusterSize(int row, int column) {

        int clusterSize = 1;

        boolean[][] visited = new boolean[boardSize][boardSize];

        for(int index = 0; index < isInCage.length; index++) {
            visited[index] = Arrays.copyOf(isInCage[index], isInCage[index].length);
        }

        LinkedList<Logic.CellPos> clusterNodes = new LinkedList<>();
        clusterNodes.addLast(new Logic.CellPos(row, column));
        visited[row][column] = true;

        while(!clusterNodes.isEmpty()) {

            row = clusterNodes.getFirst().getRow();
            column = clusterNodes.getFirst().getColumn();

            if(row != 0 && !visited[row - 1][column]) {
                clusterNodes.addLast(new Logic.CellPos(row - 1, column));
                visited[row - 1][column] = true;
                clusterSize++;
            }

            if(row != boardSize - 1 && !visited[row + 1][column]) {
                clusterNodes.addLast(new Logic.CellPos(row + 1, column));
                visited[row + 1][column] = true;
                clusterSize++;
            }

            if(column != 0 && !visited[row][column - 1]) {
                clusterNodes.addLast(new Logic.CellPos(row, column - 1));
                visited[row][column - 1] = true;
                clusterSize++;
            }

            if(column != boardSize - 1 && !visited[row][column + 1]) {
                clusterNodes.addLast(new Logic.CellPos(row, column + 1));
                visited[row][column + 1] = true;
                clusterSize++;
            }

            clusterNodes.removeFirst();
        }

        return clusterSize;
    }

    /**
     * Adds the top element to the cage
     * @param neighbors the elements in the cage
     * @param cage the cage the element will be added to
     * @param row the row of the element to be added
     * @param column the column of the element to be added
     */
    private void addTop(LinkedList<Logic.CellPos> neighbors, List<Logic.CellPos> cage, int row, int column) {

        if(random.nextBoolean() && row != 0 && !isInCage[row - 1][column]) {
            cage.add(new Logic.CellPos(row - 1, column));
            neighbors.addLast(new Logic.CellPos(row - 1, column));
            isInCage[row - 1][column] = true;
        }
    }

    /**
     * Adds the right element to the cage
     * @param neighbors the elements in the cage
     * @param cage the cage the element will be added to
     * @param row the row of the element to be added
     * @param column the column of the element to be added
     */
    private void addRight(LinkedList<Logic.CellPos> neighbors, List<Logic.CellPos> cage, int row, int column) {

        if(random.nextBoolean() && column != boardSize - 1 && !isInCage[row][column + 1]) {
            cage.add(new Logic.CellPos(row, column + 1));
            neighbors.addLast(new Logic.CellPos(row, column + 1));
            isInCage[row][column + 1] = true;
        }
    }

    /**
     * Adds the bottom element to the cage
     * @param neighbors the elements in the cage
     * @param cage the cage the element will be added to
     * @param row the row of the element to be added
     * @param column the column of the element to be added
     */
    private void addBottom(LinkedList<Logic.CellPos> neighbors, List<Logic.CellPos> cage, int row, int column) {

        if(random.nextBoolean() && row != boardSize - 1 && !isInCage[row + 1][column]) {
            cage.add(new Logic.CellPos(row + 1, column));
            neighbors.addLast(new Logic.CellPos(row + 1, column));
            isInCage[row + 1][column] = true;
        }
    }

    /**
     * Adds the left element to the cage
     * @param neighbors the elements in the cage
     * @param cage the cage the element will be added to
     * @param row the row of the element to be added
     * @param column the column of the element to be added
     */
    private void addLeft(LinkedList<Logic.CellPos> neighbors, List<Logic.CellPos> cage, int row, int column) {

        if(random.nextBoolean() && column != 0 && !isInCage[row][column - 1]) {
            cage.add(new Logic.CellPos(row, column - 1));
            neighbors.addLast(new Logic.CellPos(row, column - 1));
            isInCage[row][column - 1] = true;
        }
    }

    /**
     * Generates a random label for the cage
     * @param cage the cage for which the label will be generated
     * @return the generated label
     */
    private String generateLabel(List<Logic.CellPos> cage) {

        // Initialised to be invalid
        int target = -1;

        //
        /*
        Initialised to null character.
        Can have values :
        + means sum
        - means subtraction
        * means multiplication
        ÷ means division
        N means no operator
         */
        char operation = '\u0000';

        // While the target is invalid
        while(target == -1) {
            operation = generateOperation(cage);
            target = calculateTarget(cage, operation);
        }

        // If no operation is defined, only return the target
        return operation == 'N' ? "" + target : "" + target + operation;
    }

    /**
     * Generates a random operation for the specified cage
     * @param cage the cage for which the operation will be generated
     * @return the generated operation
     */
    private char generateOperation(List<Logic.CellPos> cage) {

        final int POSSIBLE_OPERATIONS = 4;
        int randomChar = random.nextInt(POSSIBLE_OPERATIONS);

        // If the cage consists of one cell no operation can be used
        if(cage.size() == 1) {
            return 'N';
        }

        switch(randomChar) {
            case 0:
                return '+';
            case 1:
                return '-';
            case 2:
                return 'x';
            default:
                return '÷';
        }
    }

    /**
     * Calculates the target for the specified cage using specified operation
     * @param cage the cage for which the target should be calculated
     * @param operation the operation to be used on the cage
     * @return -1 if the target cannot be calculated using the operation, the target otherwise
     */
    private int calculateTarget(List<Logic.CellPos> cage, char operation) {

        // If the cage consists of one cell
        if(operation == 'N') {
            return generatedBoard[cage.get(0).getRow()][cage.get(0).getColumn()];
        }

        switch(operation) {

            case '+':
                return getSumTarget(cage);
            case '-':
                return getSubtractionTarget(cage);
            case 'x':
                return getMultiplicationTarget(cage);
            default:
                return getDivisionTarget(cage);
        }
    }

    /**
     * Calculates the sum of the cage values
     * @param cage the cage for which the sum will be calculated
     * @return the sum of the cage values
     */
    private int getSumTarget(List<Logic.CellPos> cage) {

        int returnValue = 0;

        for(Logic.CellPos cell : cage) {
            returnValue += generatedBoard[cell.getRow()][cell.getColumn()];
        }

        return returnValue;
    }

    /**
     * Calculates the subtraction of the cage values
     * @param cage the cage for which the subtraction will be calculated
     * @return -1 if the target would be negative, the target otherwise
     */
    private int getSubtractionTarget(List<Logic.CellPos> cage) {

        int sum = getSumTarget(cage);

        int largest = 0;

        for(int increment = 0; increment < cage.size(); increment++) {

            if(generatedBoard[cage.get(increment).getRow()][cage.get(increment).getColumn()] > largest) {
                largest = generatedBoard[cage.get(increment).getRow()][cage.get(increment).getColumn()];
            }
        }

        //The game rules don't allow negative targets, so the largest value minus all the others must be positive
        return largest - (sum - largest) >= 0 ? largest - (sum - largest) : -1;
    }

    /**
     * Calculates the multiplication of the cage values
     * @param cage the cage for which the multiplication will be calculated
     * @return the multiplication of the cage values
     */
    private int getMultiplicationTarget(List<Logic.CellPos> cage) {

        int returnValue = 1;

        for(Logic.CellPos cell : cage) {
            returnValue *= generatedBoard[cell.getRow()][cell.getColumn()];
        }

        return returnValue;
    }

    /**
     * Calculates the division of the cage values
     * @param cage the cage for which the division will be calculated
     * @return -1 if division is not achievable, the target otherwise
     */
    private int getDivisionTarget(List<Logic.CellPos> cage) {

        List<Integer> cellValues = new ArrayList<>();

        for(int index = 0; index < cage.size(); index++) {
            cellValues.add(generatedBoard[cage.get(index).getRow()][cage.get(index).getColumn()]);
        }

        // The cage values in descending order
        Collections.sort(cellValues);
        Collections.reverse(cellValues);

        int target = cellValues.get(0);

        for(int index = 1; index < cellValues.size(); index++) {
            target /= cellValues.get(index);
        }

        int returnValue = target;

        for(int index = 1; index < cellValues.size(); index++) {
            target *= cellValues.get(index);
        }

        return target == cellValues.get(0) ? returnValue : -1;
    }

    /**
     * Generates a random sequence of integers between 0 - 3
     * @return the generated sequence
     */
    private int[] constructRandomSequence() {

        int[] returnSequence = new int[4];

        for(int increment = 0; increment < 4; increment++) {
            returnSequence[increment] = increment;
        }

        for(int decrement = 3; decrement > 0; decrement--) {
            int index = random.nextInt(decrement + 1);

            int temp = returnSequence[decrement];
            returnSequence[decrement] = returnSequence[index];
            returnSequence[index] = temp;
        }

        return returnSequence;
    }
}
