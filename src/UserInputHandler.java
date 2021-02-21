import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Handles events that occur in the Graphics class
 */
public class UserInputHandler {

    // Graphics object where events occur
    private Graphics graphics;

    private Logic gameLogic;

    // The stage where the game takes place
    private Stage primaryStage;

    /**
     * Connects the class with the game window
     * @param graphics the Graphics object where events occur
     * @param stage the stage where the game takes place
     */
    public UserInputHandler(Graphics graphics, Stage stage) {

        this.graphics = graphics;
        gameLogic = graphics.getGameLogic();
        primaryStage = stage;
    }

    /**
     * Adds handlers for all the events that occur in the Graphics object
     */
    public void setEventHandlers() {

        screenResizeHandler();
        setCellSelectHandler();
        setKeyboardHandler();
        setButtonHandlers();
        setKeypadHandlers();
        setFontSelectionHandlers();
    }

    /**
     * Adds event handler to insure the correct game window resizability
     */
    private void screenResizeHandler() {

        // If width of the window changes
        graphics.getPrimaryScene().widthProperty().addListener((observable, oldValue, newValue) -> {
            graphics.getRootContainer().setPrefWidth(newValue.doubleValue());
            graphics.drawBoard();
        });

        // If height of the window changes
        graphics.getPrimaryScene().heightProperty().addListener((observable, oldValue, newValue) -> {
            graphics.getRootContainer().setPrefHeight(newValue.doubleValue());
            graphics.drawBoard();
        });

        // Disable maximize button
        primaryStage.maximizedProperty().addListener((observable, oldValue, newValue) -> primaryStage.setMaximized(false));

    }

    /**
     * Adds event handler to allow the selection of a single cell
     */
    private void setCellSelectHandler() {

        // Get the selected cell depending on the mouse location on screen
        graphics.getGameGrid().addEventHandler(MouseEvent.MOUSE_CLICKED, event ->  {
            graphics.selectCell(event.getX(), event.getY());
            graphics.drawBoard();
        });
    }

    /**
     * Adds event handler for entering values into cells
     */
    private void setKeyboardHandler() {
        graphics.getPrimaryScene().setOnKeyPressed(event -> handleKeyPressed(event.getText(), event.getCode()));
    }

    /**
     * Adds appropriate event handlers to action buttons
     */
    private void setButtonHandlers() {

        graphics.getUndoButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            gameLogic.undoAction();
            graphics.drawBoard();
        });

        graphics.getRedoButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            gameLogic.redoAction();
            graphics.drawBoard();
        });

        graphics.getClearButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            clearBoard();
            graphics.drawBoard();
        });

        graphics.getHintButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event ->  {
            graphics.hintAnimation();
            graphics.drawBoard();
        });

        graphics.getSolveButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            gameLogic.getSolver().solveBoard();
            graphics.drawBoard();
        });

        graphics.getLoadFileButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            readFile(primaryStage);
            graphics.drawBoard();
        });

        graphics.getLoadTextButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> readTextInput());

        graphics.getMistakesButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            gameLogic.reverseHintsEnabled();
            graphics.drawBoard();
        });

        setGenerationHandler();
    }

    /**
     * Adds event handlers to handle number entering and deletion via in-game keypad
     */
    private void setKeypadHandlers() {

        // Deletion just sets the cells value to 0, since values less that 1 aren't showed on board
        graphics.getDeleteButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(0));

        graphics.getOneButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(1));
        graphics.getTwoButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(2));
        graphics.getThreeButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(3));
        graphics.getFourButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(4));
        graphics.getFiveButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(5));
        graphics.getSixButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(6));
        graphics.getSevenButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(7));
        graphics.getEightButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(8));
        graphics.getNineButton().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleKeyPressed(9));
    }

    /**
     * Adds event handlers for font selection
     */
    private void setFontSelectionHandlers() {

        graphics.getFontSmall().setOnAction(event -> {
            graphics.setFontSize(Graphics.FontSize.SMALL);
            graphics.getFontSelection().setText("Font: Small");
            graphics.drawBoard();
        });

        graphics.getFontMedium().setOnAction(event -> {
            graphics.setFontSize(Graphics.FontSize.MEDIUM);
            graphics.getFontSelection().setText("Font: Medium");
            graphics.drawBoard();
        });

        graphics.getFontLarge().setOnAction(event -> {
            graphics.setFontSize(Graphics.FontSize.LARGE);
            graphics.getFontSelection().setText("Font: Large");
            graphics.drawBoard();
        });
    }

    /**
     * Handles number entering via GUI number pad provided
     * @param value the value of the button pressed
     */
    private void handleKeyPressed(int value) {

        // Only writes cells, once one is selected
        if(graphics.getSelectedCell() != null) {

            int row = graphics.getSelectedCell().getRow();
            int column = graphics.getSelectedCell().getColumn();

            // If digit was entered and the cell is empty
            if(value != 0 && gameLogic.getBoard().getBoardLayout()[row][column].getValue() == 0) {

                // Add value to sequence of actions
                gameLogic.addNewUserAction(row, column, value);

                // Change the value of the cell
                gameLogic.getBoard().getBoardLayout()[row][column].setValue(value);
            }

            // If it's a backspace and the cell is not empty
            else if(value == 0 && gameLogic.getBoard().getBoardLayout()[row][column].getValue() != 0) {

                // Add value to sequence of actions
                gameLogic.addNewUserAction(row, column, 0);

                // Make cell empty
                gameLogic.getBoard().getBoardLayout()[row][column].setValue(0);
            }

            // If an action is done after undoing, redo isn't possible anymore
            gameLogic.cancelRedo();

            // Redraw the new board
            graphics.drawBoard();
        }
    }

    /**
     * Reads the file, describing the configuration of the board, chosen by the user
     * @param stage the main window of the game
     */
    private void readFile(Stage stage) {

        // User chooses the configuration file
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);

        // If file was not chosen nothing is suppose to happen
        if(file == null) {

        }

        // If the file ends with .txt
        else if(file.isFile() && file.getName().endsWith(".txt")) {

            Scanner scanner = null;

            try {

                // Each List entry describes one cage
                List<String> cages = new ArrayList<>();

                scanner = new Scanner(file);

                // Read all lines of the file
                while(scanner.hasNextLine()) {
                    cages.add(scanner.nextLine());
                }

                initialiseInput(cages);

            } catch (FileNotFoundException e) {
                new Alert(Alert.AlertType.ERROR, "File not found").showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
                gameLogic.deleteLastBoard();
            } finally {

                // Close the scanner
                if(scanner != null) {
                    scanner.close();
                }
            }
        } else {
            new Alert(Alert.AlertType.ERROR, "Only .txt files are permitted").showAndWait();
        }
    }

    /**
     * Reads board configuration from user using a popup window
     */
    private void readTextInput() {

        Stage popupStage = new Stage();

        popupStage.setTitle("Load game board from text");
        popupStage.setWidth(300);
        popupStage.setHeight(250);

        TextArea userInputArea = new TextArea();
        Scene scene = new Scene(userInputArea);

        popupStage.setScene(scene);
        popupStage.show();

        // When user wants to submit
        userInputArea.setOnKeyPressed(event -> {

            if(event.getCode() == KeyCode.ENTER) {
                event.consume();

                // If user wants to add new line instead
                if(event.isShiftDown()) {
                    userInputArea.appendText(System.getProperty("line.separator"));
                } else {
                    createBoardFromText(userInputArea.getText());
                    graphics.drawBoard();
                    popupStage.close();
                }
            }
        });
    }

    /**
     * Creates the board from user input entered by hand
     * @param userInputText user input provided in the popup text window
     */
    private void createBoardFromText(String userInputText) {

        // Each line describing a single cage
        List<String> cages = new ArrayList<>(Arrays.asList(userInputText.split("\n")));

        try {
            initialiseInput(cages);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            gameLogic.deleteLastBoard();
        }
    }

    /**
     * Sets up the game board using the user's input
     * @param inputLines the description of cages provided by the user
     * @throws Exception if the description of the cages break the rules of file configuration
     */
    private void initialiseInput(List<String> inputLines) throws Exception {

        // If the input is empty
        if(inputLines.size() == 0) {
            throw new Exception("Incorrect file configuration: the file is empty");
        }

        // Clear previous user's actions
        gameLogic.deleteLastBoard();
        graphics.setSelectedCell(null);

        List<Integer> cellIndexes;

        // Returns -1 if the board cannot be constructed
        gameLogic.getBoard().setSize(findBoardSize(inputLines));

        if(gameLogic.getBoard().getSize() == -1) {
            gameLogic.getBoard().setSize(6);
            throw new Exception("Incorrect file configuration: the board must be a square");
        }

        gameLogic.getBoard().resizeBoard();

        // For each cage
        for(String cage : inputLines) {

            // Get a list of cell ids that make up the cage
            cellIndexes = deconstructInputLine(cage);

            if(cellIndexes != null) {
                initialiseCage(cage, cellIndexes);
            } else {
                throw new Exception("Incorrect file configuration: cell number must be an integer");
            }
        }

        // Checks if the cages can construct a complete board
        if(!gameLogic.isBoardComplete()) {
            throw new Exception("Incorrect file configuration: the board has too few cells");
        }

        // Disable number pad buttons according to board size
        graphics.disableNumPad();

        // Creates the walls for the game
        gameLogic.setUpWalls();

        // If the game is unsolvable
        if(!gameLogic.getSolver().solvePuzzle()) {
            throw new Exception("Game cannot be solved with the given data");
        }
    }

    /**
     * If possible, tries to construct a cage with the given data
     * @param cage a single user provided line describing ac age
     * @param cells the cells within that line
     * @throws Exception if the cage cannot be constructed with the given data
     */
    private void initialiseCage(String cage, List<Integer> cells) throws Exception {

        // If all the cells connect and no one cell is in two cages at once
        if(gameLogic.isCage(cells) && gameLogic.isCageUnique(cells)) {
            addCageData(cage, cells);
        } else {

            // Remove the current description of the board since it's incorrect
            gameLogic.getClusterCells().clear();
            throw new Exception("Cage creation is impossible with the given data");
        }
    }

    /**
     * Creates a cage with the given data
     * @param cage a text line describing a single cage
     * @param cellIds ids of cells in the cage provided
     */
    private void addCageData(String cage, List<Integer> cellIds) {

        // Coordinates of cell within the board
        int x, y;

        List<Logic.CellPos> cells = new ArrayList<>();

        for(Integer id : cellIds) {
            x = (id - 1) % gameLogic.getBoard().getSize();
            y = (id - x) / gameLogic.getBoard().getSize();

            cells.add(new Logic.CellPos(y, x));
        }

        attachLabel(cage);
        gameLogic.getClusterCells().add(cells);
    }

    /**
     * Attaches a label to a cell
     * @param cage a line describing a single cage
     */
    private void attachLabel(String cage) {

        // The cell that the label will be attached to
        int firstCell;

        // If the cage consists of more that one cell
        if(cage.contains(",")) {
            firstCell = Integer.parseInt(cage.substring(cage.indexOf(" ") + 1, cage.indexOf(",")));
        } else {
            firstCell = Integer.parseInt(cage.substring(cage.indexOf(" ") + 1));
        }

        int x = (firstCell - 1) % gameLogic.getBoard().getSize();
        int y = (firstCell - x) / gameLogic.getBoard().getSize();

        gameLogic.getBoard().getBoardLayout()[y][x].setLabel(getLabel(cage));
    }

    /**
     * Extracts the label from cage description line
     * @param cage a line describing a single cage
     * @return the label of the cage
     */
    private String getLabel(String cage){
        return cage.substring(0, cage.indexOf(" "));
    }

    /**
     * Extracts the list of cell ids from a text description of a cage
     * @param cage the text description of a cage
     * @return the list of cell ids making up the cage
     */
    private List<Integer> deconstructInputLine(String cage) {

        List<Integer> cellIndexes = new ArrayList<>();

        // Remove label
        cage = cage.substring(cage.indexOf(" ") + 1);

        String[] cells = cage.split(",");

        for(String cell : cells) {
            if(!isInteger(cell)) {
                return null;
            } else {
                cellIndexes.add(Integer.parseInt(cell));
            }
        }

        return cellIndexes;
    }

    /**
     * Calculates the appropriate size for the board, given the cage descriptions
     * @param inputLines a list of lines, each describing one cage
     * @return -1 if the board is not possible to construct, board size otherwise
     * @throws Exception if the description of the cages break the rules of file configuration
     */
    private int findBoardSize(List<String> inputLines) throws Exception {

        String[] cells;

        int largestCell = 0;

        for(String cage : inputLines) {

            // Separator between label and cells
            if(!cage.contains(" ")) {
                throw new Exception("Incorrect file configuration: label could not be found");
            }

            // String of all cage cells
            cage = cage.substring(cage.indexOf(" ") + 1);

            // Get each cell
            cells = cage.split(",");

            // Finds largest cell
            for(String cell : cells) {

                // Checking if the cell's an integer
                if(isInteger(cell)) {
                    largestCell = largestCell > Integer.parseInt(cell) ? largestCell : Integer.parseInt(cell);
                } else {
                    throw new Exception("Incorrect file configuration: cell number must be an integer");
                }
            }

        }

        // The square root of the largest cell is suppose to be the size of the board
        if(isPerfectSquare(largestCell)) {
            return (int)Math.sqrt(largestCell);
        }

        return -1;
    }

    /**
     * Checks if the given String in an integer
     * @param number the String to be checked
     * @return true if it is an integer, false otherwise
     */
    private boolean isInteger(String number) {

        if(number == null || number.length() == 0) {
            return false;
        }

        // If each letter is an integer
        for(char c : number.toCharArray()) {
            if(!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a number is a perfect square
     * @param number the number to be checked
     * @return true if the number is a perfect square, false otherwise
     */
    private boolean isPerfectSquare(double number) {

        double squareRoot = Math.sqrt(number);
        return squareRoot - Math.floor(squareRoot) == 0;
    }

    /**
     * Adds event handlers to allow the generation of different size boards
     */
    private void setGenerationHandler() {

        for(MenuItem sizeOption : graphics.getGenerateButton().getItems()) {

            sizeOption.setOnAction(event -> {

                graphics.getGameLogic().getGenerator().generateBoard(Integer.parseInt(sizeOption.getText().charAt(0) + ""));

                graphics.setSelectedCell(null);
                graphics.disableNumPad();

                graphics.drawBoard();
            });
        }
    }

    /**
     * Handles number entering via keyboard once a cell is selected
     * @param value the value of the pressed key
     * @param code the code of the key
     */
    private void handleKeyPressed(String value, KeyCode code) {

        if(graphics.getSelectedCell() != null) {

            int row = graphics.getSelectedCell().getRow();
            int column = graphics.getSelectedCell().getColumn();

            // Only writes cells, once one is selected
            if (graphics.getSelectedCell() != null) {

                // If it's a digit and it's value doesn't exceed the board size, and the selected cell is empty (0 represents empty cells)
                if (code.isDigitKey() && Integer.parseInt(value) <= gameLogic.getBoard().getSize() &&
                        gameLogic.getBoard().getBoardLayout()[row][column].getValue() == 0) {

                    // Add the action done by the user to the action sequence
                    gameLogic.addNewUserAction(row, column, Integer.parseInt(value));

                    // Change the value of the cell
                    gameLogic.getBoard().getBoardLayout()[row][column].setValue(Integer.parseInt(value));
                }

                // If it's a backspace and the cell is not empty
                else if (code == KeyCode.BACK_SPACE && gameLogic.getBoard().getBoardLayout()[row][column].getValue() != 0) {

                    // Add the action done by the user to the action sequence
                    gameLogic.addNewUserAction(row, column, 0);

                    // Make cell empty
                    gameLogic.getBoard().getBoardLayout()[row][column].setValue(0);
                }

                // If an action is done after undoing, redo isn't possible anymore
                gameLogic.cancelRedo();

                // Redraw the new board
                graphics.drawBoard();
            }
        }
    }

    /**
     * Clears the values of all cells
     */
    private void clearBoard() {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to clear the board?" +
                " This process is irreversible.", ButtonType.YES, ButtonType.NO);

        alert.showAndWait();

        // Clear board if YES is pressed, close the dialog box otherwise
        if (alert.getResult() == ButtonType.YES) {

            gameLogic.getBoard().resetBoardValues();

            // The clearing of a board is irreversible so the history of previous actions is deleted
            gameLogic.clearUserActions();
        }
        else if (alert.getResult() == ButtonType.NO) {
            alert.close();
        }

    }
}
