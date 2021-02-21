/**
 * Finds a solution to a Mathdoku puzzle
 */
public class Solver {

    // The logical representation of the current puzzle
    private Logic gameLogic;

    // The size of the current puzzle's board
    private int boardSize;

    private int[][] solvedBoard;

    /**
     * Creates a solver for the current game board
     * @param board the logical representation of the current board
     */
    public Solver(Logic board) {

        gameLogic = board;
    }

    /**
     * Solves the a Mathdoku puzzle
     * @return true if the puzzle is solvable, false otherwise
     */
    public boolean solvePuzzle() {

        boardSize = gameLogic.getBoard().getSize();

        if(recursiveSolve()) {

            // Get the solution board values
            setSolvedBoard();

            // Reset the main board that was used
            gameLogic.getBoard().resetBoardValues();

            return true;
        }

        return false;
    }

    /**
     * Sets the current board to the correct values
     */
    public void solveBoard() {

        for(int row = 0; row < boardSize; row++) {
            for(int column = 0; column < boardSize; column++) {

                gameLogic.getBoard().getBoardLayout()[row][column].setValue(solvedBoard[row][column]);
            }
        }
    }

    /**
     * Recursively solves the game board puzzle by backtracking
     * @return true if partial solution is correct, false otherwise
     */
    private boolean recursiveSolve() {

        for(int row = 0; row < boardSize; row++) {
            for(int column = 0; column < boardSize; column++) {

                if(gameLogic.getBoard().getBoardLayout()[row][column].getValue() == 0) {
                    for(int newValue = 1; newValue <= boardSize; newValue++) {

                        gameLogic.getBoard().getBoardLayout()[row][column].setValue(newValue);
                        if(isValid() && recursiveSolve()) {
                            return true;
                        }
                        gameLogic.getBoard().getBoardLayout()[row][column].setValue(0);
                    }

                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if current board state satisfies all game rules
     * @return true if the board doesn't brake any rules, false otherwise
     */
    private boolean isValid() {
        return gameLogic.areRowsCorrect() && gameLogic.areColumnsCorrect() && gameLogic.areCagesCorrect();
    }

    /**
     * Constructs the array to store the solved board values
     */
    private void setSolvedBoard() {

        solvedBoard = new int[boardSize][boardSize];

        for(int row = 0; row < boardSize; row++) {
            for(int column = 0; column <boardSize; column++) {

                solvedBoard[row][column] = gameLogic.getBoard().getBoardLayout()[row][column].getValue();
            }
        }
    }

    /**
     * Getter for solved board
     * @return the solution to the current puzzle
     */
    public int[][] getSolvedBoard() {
        return solvedBoard;
    }

    /**
     * Sets the solution of the board
     * @param solvedBoard the solution to the current puzzle
     */
    public void setSolvedBoard(int[][] solvedBoard) {

        if(solvedBoard != null) {
            boardSize = solvedBoard.length;
        }

        this.solvedBoard = solvedBoard;
    }
}
