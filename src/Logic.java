import java.util.*;

/**
 * Logical representation of the structure of the game board. Ensures the correct implementation of
 * Mathdoku game rules
 */
public class Logic {

    // Logical representation of a Mathdoku board
    private Board board = new Board(6);

    private Solver solver = new Solver(this);

    private Generator generator = new Generator(this);

    // Tracks if undo button was pressed to assure the correct work of redo action
    private boolean isUndoPressed = false;

    // Determines if cells will be highlighted when mistake is detected
    private boolean hintsEnabled = false;

    // All the cages that make up the board
    private ArrayList<List<CellPos>> clusterCells = new ArrayList<>();

    // All the values entered into the board in correct order
    private List<CellValue> actionSequence = new LinkedList<>();

    /*
     Position in <code>actionSequence</code> to track the last modified cell.
     Used with undo and redo to reset board to a previous state
      */
    private int currentAction = -1;

    // Rows, columns and cages that brake the game rules
    private ArrayList<Integer> incorrectRows = new ArrayList<>();
    private ArrayList<Integer> incorrectColumns = new ArrayList<>();
    private ArrayList<List<CellPos>> incorrectCages = new ArrayList<>();

    // The cell that will be revealed to the user as a hint
    private CellPos hintCell = new CellPos(-1, -1);

    /**
     * Generates a the coordinates of a cell that is incorrect or empty
     */
    public void hintAction() {

        Random random = new Random();

        // If cell hasn't been generated yet, generate it
        if(hintCell.row == -1) {
            hintCell.row = random.nextInt(board.size);
            hintCell.column = random.nextInt(board.size);
        }

        // If the hint has been used by the user, generate a new one
        if(board.boardLayout[hintCell.row][hintCell.column].value ==
                solver.getSolvedBoard()[hintCell.row][hintCell.column]) {

            do {
                hintCell.row = random.nextInt(board.size);
                hintCell.column = random.nextInt(board.size);

            } while(board.boardLayout[hintCell.row][hintCell.column].value ==
                    solver.getSolvedBoard()[hintCell.row][hintCell.column]);
        }
    }

    /**
     * Return the board to the state it was before undo'ing the last action
     */
    public void redoAction() {

        // The undone user's action becomes the current one
        currentAction++;

        int row = actionSequence.get(currentAction).cell.row;
        int column = actionSequence.get(currentAction).cell.column;

        // Return the cell to it's undone value
        board.boardLayout[row][column].value = actionSequence.get(currentAction).value;

        // If redo is no longer possible
        if(!isRedoPossible()) {

            // All undone actions have been redone, so undone is no longer pressed
            setUndoPressed(false);
        }
    }

    /**
     * Return the board to the state it was before the last action
     */
    public void undoAction() {

        int row = actionSequence.get(currentAction).cell.row;
        int column = actionSequence.get(currentAction).cell.column;
        int value = actionSequence.get(currentAction).value;

        if(value != 0) {

            // The value entered to the last cell is deleted
            board.boardLayout[row][column].value = 0;
        } else {

            // If the last action was deletion, find the value the cell had previously
            for(int index = currentAction; index >= 0; index--) {

                if(actionSequence.get(index).cell.row == row && actionSequence.get(index).cell.column == column) {

                    board.boardLayout[row][column].value = actionSequence.get(index).value;
                }
            }
        }

        // Redo becomes possible after undoing
        setUndoPressed(true);

        // The previous user's action becomes the current one
        currentAction--;
    }

    /**
     * Detects if a winning condition has been reached
     */
    public boolean isWinReached() {

        /*
        If all the rows, columns and cages are correct, and the board is full.
        First three methods have to execute regardless because they construct the
        arrays arrays of mistakes in the board.
         */
        return areRowsCorrect() & areColumnsCorrect() & areCagesCorrect() && boardIsFull();

    }

    /**
     * Finds all the mistakes inside rows in a board
     * @return True if all rows are correct, false otherwise
     */
    public boolean areRowsCorrect() {

        boolean returnValue = true;

        // Clears previous mistakes
        incorrectRows.clear();

        // For each row in board
        for(int rows = 0; rows < board.size; rows++) {

            // Breaks to here if a mistake is found inside a row
            nextRow:

            // Compare values inside a row
            for (int i = 0; i < board.size - 1; i++) {
                for (int j = i + 1; j < board.size; j++) {

                    // If the cells aren't empty and two values in a row are the same
                    if (board.boardLayout[rows][i].value != 0 &&
                            board.boardLayout[rows][i].value == board.boardLayout[rows][j].value) {

                        // Only track mistakes if it's enabled
                        if(hintsEnabled) {

                            // Row is marked as incorrect
                            incorrectRows.add(rows);

                            // Notes a mistake exists
                            returnValue = false;

                            // Goes to next row
                            break nextRow;
                        } else {

                            // If hints aren't enabled, but mistake exists
                            return false;
                        }
                    }
                }
            }
        }

        // Will return true if no mistakes were found
        return returnValue;
    }

    /**
     * Finds all the mistakes inside columns in a board
     * @return True if all columns are correct, false otherwise
     */
    public boolean areColumnsCorrect() {

        boolean returnValue = true;

        // Clears previous mistakes
        incorrectColumns.clear();

        // For each column in board
        for(int columns = 0; columns < board.size; columns++) {

            // Breaks to here if a mistake is found inside a column
            nextColumn:

            // Compare values inside a column
            for (int i = 0; i < board.size - 1; i++) {
                for (int j = i + 1; j < board.size; j++) {

                    // If the cells aren't empty and two values in a column are the same
                    if (board.boardLayout[i][columns].value != 0 &&
                            board.boardLayout[i][columns].value == board.boardLayout[j][columns].value) {

                        // Only track mistakes if it's enabled
                        if(hintsEnabled) {

                            // Column is marked as incorrect
                            incorrectColumns.add(columns);

                            // Notes a mistake exists
                            returnValue = false;

                            // Goes to next column
                            break nextColumn;
                        } else {

                            // If hints aren't enabled, but mistake exists
                            return false;
                        }
                    }
                }
            }
        }

        // Will return true if no mistakes were found
        return returnValue;
    }

    /**
     * Finds all the mistakes inside cages in a board
     * @return True if all cages are correct, false otherwise
     */
    public boolean areCagesCorrect() {

        boolean returnValue = true;

        // Clears previous mistakes
        incorrectCages.clear();

        // For every cage in a board
        for(List<CellPos> clusters : clusterCells) {

            String label;

            // For every cell in a cage
            for(CellPos cell : clusters) {

                label = board.boardLayout[cell.row][cell.column].label;

                // If the cell doesn't have a label, skip it
                if(label.isEmpty()) {
                    continue;
                }

                // The value a cage has to reach
                int target;

                // If the last character of the label doesn't define an operation
                if(Character.isDigit(label.charAt(label.length() - 1))) {

                    // Then the whole label is the target number
                    target = Integer.parseInt(label);
                } else {
                    target = Integer.parseInt(label.substring(0, label.length() - 1));
                }

                // The cage is incorrect if it's full, but doesn't satisfy it's label's condition
                if(isCageFull(clusters) && !isCageCorrect(clusters, label.charAt(label.length() - 1), target)) {

                    // Only track mistakes if it's enabled
                    if(hintsEnabled) {

                        // Cage is marked as incorrect
                        incorrectCages.add(clusters);
                    }

                    // Notes a mistake exists
                    returnValue = false;
                }
            }
        }

        // Will return true if no mistakes were found
        return returnValue;
    }

    /**
     * Checks whether the cage has any empty cells inside it
     * @param clusterCells a cage in the board
     * @return True if there are no empty cells inside the cage, false otherwise
     */
    private boolean isCageFull(List<CellPos> clusterCells) {

        for(CellPos cell : clusterCells) {

            if(board.boardLayout[cell.row][cell.column].value == 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether a target value can be reached by applying an operation on it's values
     * @param clusterCells a cage in the board
     * @param operation the operation to be applied to the cage cells
     * @param target the value to be achieved
     * @return True if the target can be reached, false otherwise
     */
    private boolean isCageCorrect(List<CellPos> clusterCells, char operation, int target) {

        // If no operation is declared
        if(Character.isDigit(operation)) {
            return isNoOperationCorrect(clusterCells, target);
        }

        switch(operation) {

            case '+' :
                return isSumCorrect(clusterCells, target);

            case '-' :
                return isSubtractionCorrect(clusterCells, target);

            case 'x' :
                return isMultiplicationCorrect(clusterCells, target);

            case '÷' :
                return isDivisionCorrect(clusterCells, target);
        }

        return false;
    }

    /**
     * Checks if cage with no operation is correct
     * @param clusterCells a cage in the board
     * @param target the value that has to be the same as the cell's value
     * @return true if the target is equal to the cell's value, false otherwise
     */
    private boolean isNoOperationCorrect(List<CellPos> clusterCells, int target) {

        return clusterCells.size() == 1 &&
                board.boardLayout[clusterCells.get(0).row][clusterCells.get(0).column].value == target;
    }

    /**
     * Checks if addition inside a cage is correct
     * @param clusterCells a cage in the board
     * @param target the value to be achieved by addition
     * @return True if the target can be reached, false otherwise
     */
    private boolean isSumCorrect(List<CellPos> clusterCells, int target) {

        int sum = 0;

        for(CellPos point : clusterCells) {
            sum += board.boardLayout[point.row][point.column].value;
        }

        return sum == target;
    }

    /**
     * Checks if subtraction inside a cage is correct
     * @param clusterCells a cage in the board
     * @param target the value to be achieved by subtraction
     * @return True if the target can be reached, false otherwise
     */
    private boolean isSubtractionCorrect(List<CellPos> clusterCells, int target) {

        int sum = 0;

        // Get the sum of the cage
        for(CellPos point : clusterCells) {
            sum += board.boardLayout[point.row][point.column].value;
        }

        // Subtract on of the values from the sum
        for(CellPos point : clusterCells) {

            if(Math.abs(sum - 2 * board.boardLayout[point.row][point.column].value) == Math.abs(target)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if multiplication inside a cage is correct
     * @param clusterCells a cage in the board
     * @param target the value to be achieved by multiplication
     * @return True if the target can be reached, false otherwise
     */
    private boolean isMultiplicationCorrect(List<CellPos> clusterCells, int target) {

        int sum = 1;

        for(CellPos point : clusterCells) {
            sum *= board.boardLayout[point.row][point.column].value;
        }

        return sum == target;
    }

    /**
     * Checks if division inside a cage is correct
     * @param clusterCells a cage in the board
     * @param target the value to be achieved by division
     * @return True if the target can be reached, false otherwise
     */
    private boolean isDivisionCorrect(List<CellPos> clusterCells, int target) {

        if(target == 0) {
            return false;
        }

        List<CellPos> sortedList = new ArrayList<>(clusterCells);
        Collections.sort(sortedList, new Comparator<CellPos>() {
            @Override
            public int compare(CellPos cellPos1, CellPos cellPos2) {
                return board.boardLayout[cellPos1.row][cellPos1.column].value >
                        board.boardLayout[cellPos2.row][cellPos2.column].value ? 1 : -1;
            }
        });

        CellPos index = sortedList.get(sortedList.size() - 1);

        // The largest element must be possible to reach by multiplying target by the other cell values
        int largest = board.boardLayout[index.row][index.column].value;

        for(int cell = 0; cell < sortedList.size() - 1; cell++) {
            target *= board.boardLayout[sortedList.get(cell).row][sortedList.get(cell).column].value;
        }

        return target == largest;
    }

    /**
     * Checks if all the cells in a board have values
     * @return True if all the cells in a board have values, false otherwise
     */
    private boolean boardIsFull() {

        for(int i = 0; i < board.size; i++) {
            for(int j = 0; j < board.size; j++) {

                if(board.boardLayout[i][j].value == 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if the cage data makes a complete board (all cells are used)
     * @return true if the board is complete, false otherwise
     */
    public boolean isBoardComplete() {

        for(int y = 0; y < board.size; y++) {
            for(int x = 0; x < board.size; x++) {

                // If one of the cells is not in a cage, the board is not complete
                if(!board.boardLayout[y][x].isInCage) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if any one cell is in more that one cage at once
     * @param cells the list of cells defining a cage to be checked
     * @return true if the cell hasn't been included in a cell before, false otherwise
     */
    public boolean isCageUnique(List<Integer> cells) {


        // Coordinates of the cell within the board
        int x, y;

        for(Integer id : cells) {

            // Get coordinates of the cell
            x = (id - 1) % board.size;
            y = (id - x) / board.size;

            // If cell is not in cage yet, add it
            if(!board.boardLayout[y][x].isInCage) {
                board.boardLayout[y][x].isInCage = true;
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given cells can form a cage
     * @param cellIds the ids of cells that should form a cage
     * @return true if the cells can form a cage, false otherwise
     */
    public boolean isCage(List<Integer> cellIds) {

        // Coordinates of a cell within a board
        int x, y;

        // All the cells that are neighbors with the given cells
        List<Integer> neighbors = new ArrayList<>();

        // One cell is always a cage
        if(cellIds.size() == 1) {
            return true;
        }

        // Add neighbors
        for(Integer id : cellIds) {

            x = (id - 1) % board.size;
            y = (id - x) / board.size;

            // not the first row
            if(y != 0) {
                neighbors.add(id - board.size);
            }

            // not the bottom row
            if(y != board.size - 1) {
                neighbors.add(id + board.size);
            }

            // not the right-most column
            if(x != board.size - 1) {
                neighbors.add(id + 1);
            }

            // not the left-most column
            if(x != 0) {
                neighbors.add(id - 1);
            }
        }

        // If its a cage. all the cage cells should have been included as a neighbor of another
        for(Integer id : cellIds) {
            if(!neighbors.contains(id)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a cell exists within the given cage
     * @param cage the cage to be checked for the given cell
     * @param requiredCell the cell to be searched within the cage
     * @return true if the cell exists in the cage, false otherwise
     */
    private boolean containsCell(List<CellPos> cage, CellPos requiredCell) {

        for(CellPos cell : cage) {

            if(requiredCell.row == cell.row && requiredCell.column == cell.column) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the wall values for each cell
     */
    public void setUpWalls() {

        for(List<CellPos> cage : clusterCells) {
            for(CellPos cell : cage) {

                // If the top cell is not in the same cage
                if(cell.row != 0 && !containsCell(cage, new CellPos(cell.row - 1, cell.column))) {

                    // There is a cage above the cell
                    board.boardLayout[cell.row][cell.column].isCageTop = true;

                    // There is a cage below the above cell
                    board.boardLayout[cell.row - 1][cell.column].isCageBottom = true;
                }


                // If the bottom cell is not in the same cage
                if(cell.row != board.size - 1 && !containsCell(cage, new CellPos(cell.row + 1, cell.column))) {

                    // There is a cage below the cell
                    board.boardLayout[cell.row][cell.column].isCageBottom = true;

                    // There is a cage above the below cell
                    board.boardLayout[cell.row + 1][cell.column].isCageTop = true;
                }


                // If the left cell is not in the same cage
                if(cell.column != 0 && !containsCell(cage, new CellPos(cell.row, cell.column - 1))) {

                    // There is a cage to the left of the cell
                    board.boardLayout[cell.row][cell.column].isCageLeft = true;
                    board.boardLayout[cell.row][cell.column - 1].isCageRight = true;
                }


                // If the right cell is not in the same cage
                if(cell.column != board.size - 1 && !containsCell(cage, new CellPos(cell.row, cell.column + 1))) {

                    // There is a cage to the right of the cell
                    board.boardLayout[cell.row][cell.column].isCageRight = true;
                    board.boardLayout[cell.row][cell.column + 1].isCageLeft = true;
                }
            }
        }
    }

    /**
     * Deletes all the data of the last puzzle
     */
    public void deleteLastBoard() {

        int size = getBoard().getSize();

        for(int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {

                board.boardLayout[row][column].value = 0;
                board.boardLayout[row][column].label = "";
                board.resetCellWalls(row, column);
                board.boardLayout[row][column].isInCage = false;
            }
        }

        getClusterCells().clear();
        clearUserActions();

        getSolver().setSolvedBoard(null);

        hintCell.row = -1;
        hintCell.column = -1;
    }

    /**
     * Reverses the value of the parameter that decides if hints are enabled
     */
    public void reverseHintsEnabled() {
        this.hintsEnabled = !hintsEnabled;
    }

    /**
     * Checks if an undo action is possible
     * @return true if undo can be achieved, false otherwise
     */
    public boolean isUndoPossible() {
        return currentAction != -1;
    }

    /**
     * Checks if a redo action is possible
     * @return true if redo can be achieved, false otherwise
     */
    public boolean isRedoPossible() {
        return isUndoPressed && currentAction < actionSequence.size() - 1;
    }

    /**
     * Adds a new cell modification done by the user
     * @param row row of the modified cell
     * @param column column of the modified cell
     * @param value the new value entered into the cell
     */
    public void addNewUserAction(int row, int column, int value) {

        currentAction++;
        actionSequence.add(currentAction, new CellValue(new CellPos(row, column), value));
    }

    /**
     * Clears the sequence of user's actions
     */
    public void clearUserActions() {
        actionSequence.clear();
        currentAction = -1;
    }

    /**
     * Makes redo no longer possible to achieve
     */
    public void cancelRedo() {

        // If redo was possible
        if(isUndoPressed) {

            // Make it no longer possible
            isUndoPressed = false;

            // Remove all the values that can no longer be reached using redo
            removeUnreachableActions(currentAction);
        }
    }

    /**
     * Deletes user actions that no longer can be reached using undo / redo.
     * Ignores a specified number of elements at the beginning of the List, deletes the rest
     * @param index the index of the first element to be deleted
     */
    private void removeUnreachableActions(int index) {

        while(actionSequence.size() - 1 > index) {
            actionSequence.remove(actionSequence.size() - 1);
        }
    }

    /**
     * Returns the Solver object that's responsible for finding a solution to the puzzle
     * @return the puzzle solver
     */
    public Solver getSolver() {
        return solver;
    }

    /**
     * Return the Generator object, that's responsible for generating new puzzles
     * @return the puzzle generator
     */
    public Generator getGenerator() {
        return generator;
    }

    /**
     * Get the cages that make up the game board
     * @return the ArrayList of all cages that makes up the game board
     */
    public ArrayList<List<CellPos>> getClusterCells() {
        return clusterCells;
    }

    /**
     * Get the incorrect rows within a game board
     * @return the ArrayList of all incorrect rows within a game board
     */
    public ArrayList<Integer> getIncorrectRows() {
        return incorrectRows;
    }

    /**
     * Get the incorrect columns within a game board
     * @return the ArrayList of all incorrect columns within a game board
     */
    public ArrayList<Integer> getIncorrectColumns() {
        return incorrectColumns;
    }

    /**
     * Get the incorrect cages within a game board
     * @return the ArrayList of all incorrect cages within a game board
     */
    public ArrayList<List<CellPos>> getIncorrectCages() {
        return incorrectCages;
    }

    /**
     * Returns the coordinates of the cell to be hinted to the user
     * @return the coordinates of the cell to be hinted to the user
     */
    public CellPos getHintCell() {
        return hintCell;
    }

    /**
     * Set the cages that make up the game board
     * @param clusterCells the new cage values of the board
     */
    public void setClusterCells(ArrayList<List<CellPos>> clusterCells) {
        this.clusterCells = clusterCells;
    }

    /**
     * Get the the parameter that decides if hints are enabled
     * @return true if hints are enabled, false otherwise
     */
    public boolean isHintsEnabled() {
        return hintsEnabled;
    }

    /**
     * Set the value for the variable that tracks if undo button has been pressed
     * @param isUndoPressed true if undo button has been pressed, false otherwise
     */
    public void setUndoPressed(boolean isUndoPressed) {
        this.isUndoPressed = isUndoPressed;
    }

    /**
     * Get the board of the game
     * @return the board of the game
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Class defines the position of a cell in the board
     */
    public static class CellPos {

        private int row, column;

        /**
         * @param row Y coordinate in a 2D space
         * @param column X coordinate in a 2D space
         */
        CellPos(int row, int column) {
            this.row = row;
            this.column = column;
        }

        /**
         * Get the row of the cell
         * @return the row of the cell
         */
        public int getRow() {
            return row;
        }

        /**
         * Get the column of the cell
         * @return the column of the cell
         */
        public int getColumn() {
            return column;
        }
    }

    /**
     * Class that connects a cell with it's value
     */
    private class CellValue {

        private CellPos cell;

        private int value;

        /**
         * @param cell the coordinates of the cell
         * @param value the value of the cell
         */
        public CellValue(CellPos cell, int value) {
            this.cell = cell;
            this.value = value;
        }
    }

    /**
     * Class defines a single cell of the board
     */
    public static class Cell {

        // The value to be reached by the cage the cell is in. By default a cell has no label
        private String label = "";

        // Value of the cell. By default initialised as unreachable
        private int value = 0;

        // Used to check if the cell is already in cage (so it wouldn't be added to another)
        private boolean isInCage = false;

        // The type of walls around the cell. If it's true, there is a cage in the appropriate direction
        private boolean isCageBottom;
        private boolean isCageTop;
        private boolean isCageLeft;
        private boolean isCageRight;

        /**
         * Get the label of the cell
         * @return the label of the cell
         */
        public String getLabel() {
            return label;
        }

        /**
         * Set the value of the label of the cell
         * @param label the value of to be set to the label of the cell
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * Get the value of the cell
         * @return the value of the cell
         */
        public int getValue() {
            return value;
        }

        /**
         * Set the value of the cell
         * @param value the new value to be assigned to the cell
         */
        public void setValue(int value) {
            this.value = value;
        }

        /**
         * Get if there is a cage wall below the cell
         * @return true if there is a cage wall below the cell, false otherwise
         */
        public boolean isCageBottom() {
            return isCageBottom;
        }

        /**
         * Get if there is a cage wall above the cell
         * @return true if there is a cage wall above the cell, false otherwise
         */
        public boolean isCageTop() {
            return isCageTop;
        }

        /**
         * Get if there is a cage wall to the left of the cell
         * @return true if there is a cage wall to the left of the cell, false otherwise
         */
        public boolean isCageLeft() {
            return isCageLeft;
        }

        /**
         * Get if there is a cage wall to the right of the cell
         * @return true if there is a cage wall to the right of the cell, false otherwise
         */
        public boolean isCageRight() {
            return isCageRight;
        }
    }

    /**
     * Class representing the board of the game
     */
    public static class Board {

        // The number of rows / columns in the board
        private int size;

        // The cell configuration of the board
        private Cell[][] boardLayout;

        /**
         * @param size number of rows(columns) in the game board
         */
        public Board(int size) {

            this.size = size;
            boardLayout = new Cell[size][size];

            for(int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    boardLayout[i][j] = new Logic.Cell();
                }
            }
        }

        /**
         * Change the size of the board layout
         */
        public void resizeBoard() {

            boardLayout = new Cell[size][size];

            for(int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    boardLayout[i][j] = new Logic.Cell();
                }
            }
        }

        /**
         * Resets the walls around the cell to be non-cage
         * @param row the row of the cell
         * @param column the column of the cell
         */
        public void resetCellWalls(int row, int column) {

            boardLayout[row][column].isCageTop = false;
            boardLayout[row][column].isCageBottom = false;
            boardLayout[row][column].isCageLeft = false;
            boardLayout[row][column].isCageRight = false;
        }

        /**
         * Resets the values of the cells of the board to zero
         */
        public void resetBoardValues() {

            for(int row = 0; row < size; row++) {
                for(int column = 0; column < size; column++) {

                    boardLayout[row][column].value = 0;
                }
            }
        }

        /**
         * Get the size of the board (number rof rows / columns)
         * @return the size of the board
         */
        public int getSize() {
            return size;
        }

        /**
         * Set the size of the board
         * @param size the size of the board to be set (number of rows / columns)
         */
        public void setSize(int size) {
            this.size = size;
        }

        /**
         * Get the configuration of the board
         * @return the configuration of the board
         */
        public Cell[][] getBoardLayout() {
            return boardLayout;
        }
    }
}
