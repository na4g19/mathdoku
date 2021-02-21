import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;



import java.util.List;
import java.util.Random;

/**
 * Main class responsible for displaying the visuals of the game
 */
public class Graphics extends Application {

    /**
     * Font size for labels and values in the board
     */
    public enum FontSize {SMALL, MEDIUM, LARGE}

    // Handles logical operations of the game
    private Logic gameLogic = new Logic();

    // The size of the game board grid in pixels
    private int boardSize;

    // The cell that is next to be written to
    private Logic.CellPos selectedCell = null;

    // Holds the game board in the bottom layer, animations on top
    private StackPane rootContainer = new StackPane();

    // Entire game board
    private VBox gameBoard = new VBox();

    private HBox topButtons = new HBox();
    private HBox mainBoard = new HBox();
    private HBox bottomButtons = new HBox();

    private GridPane numberPad = new GridPane();

    // Mathdoku grid
    private Canvas gameGrid = new Canvas();
    private GraphicsContext boardGraphics = gameGrid.getGraphicsContext2D();

    // Main scene of the game
    private Scene primaryScene;

    // Font size of values and labels
    private FontSize fontSize = FontSize.MEDIUM;

    // The size of a single white cell that makes up the grid
    private int cellSize;

    // The width of a wall between two regular cells
    private int regularWall = 2;

    // The width of the wall that surrounds the grid
    private int outsideWall = 3;

    // The width of a wall that surrounds a cage
    private int cageWall = 6;

    // Buttons for main actions
    private Button undoButton = new Button("Undo");
    private Button redoButton = new Button("Redo");
    private Button clearButton = new Button("Clear");
    private Button hintButton = new Button("Hint");
    private Button loadFileButton = new Button("Load from file");
    private Button loadTextButton = new Button("Load from text");
    private Button mistakesButton = new Button("Show mistakes");
    private Button solveButton = new Button("Solve");

    // Buttons for number pad
    private Button oneButton = new Button("1");
    private Button twoButton = new Button("2");
    private Button threeButton = new Button("3");
    private Button fourButton = new Button("4");
    private Button fiveButton = new Button("5");
    private Button sixButton = new Button("6");
    private Button sevenButton = new Button("7");
    private Button eightButton = new Button("8");
    private Button nineButton = new Button("9");
    private Button deleteButton = new Button("<-");

    private SplitMenuButton fontSelection = new SplitMenuButton();
    private SplitMenuButton generateButton = new SplitMenuButton();

    private MenuItem fontSmall = new MenuItem("Small");
    private MenuItem fontMedium = new MenuItem("Medium");
    private MenuItem fontLarge = new MenuItem("Large");

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Mathdoku");

        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        setUpComponents();

        fontSelection.getItems().addAll(fontSmall, fontMedium, fontLarge);

        for(int option = 2; option <= 8; option++) {
            generateButton.getItems().add(new MenuItem(option + "x" + option));
        }

        numberPad.add(oneButton, 0, 0, 1, 1);
        numberPad.add(twoButton, 1, 0, 1, 1);
        numberPad.add(threeButton, 2, 0, 1, 1);
        numberPad.add(fourButton, 0, 1, 1, 1);
        numberPad.add(fiveButton, 1, 1, 1, 1);
        numberPad.add(sixButton, 2, 1, 1, 1);
        numberPad.add(sevenButton, 0, 2, 1, 1);
        numberPad.add(eightButton, 1, 2, 1, 1);
        numberPad.add(nineButton, 2, 2, 1, 1);
        numberPad.add(deleteButton, 0, 3, 3, 1);

        topButtons.getChildren().addAll(undoButton, redoButton, clearButton, mistakesButton, hintButton, solveButton);
        mainBoard.getChildren().addAll(gameGrid, numberPad);
        bottomButtons.getChildren().addAll(loadFileButton, loadTextButton, generateButton, fontSelection);

        gameBoard.getChildren().addAll(topButtons, mainBoard, bottomButtons);

        rootContainer.getChildren().add(gameBoard);

        primaryScene = new Scene(rootContainer);

        // Sets up event handlers for the stage
        UserInputHandler userInputHandler = new UserInputHandler(this, primaryStage);
        userInputHandler.setEventHandlers();

        primaryStage.setScene(primaryScene);
        primaryStage.show();

        drawBoard();
    }

    /**
     * Briefly reveals the correct value of one of the cells
     */
    public void hintAnimation() {

        // Get the hint cell
        getGameLogic().hintAction();

        int row = getGameLogic().getHintCell().getRow();
        int column = getGameLogic().getHintCell().getColumn();

        Logic.Cell cell = gameLogic.getBoard().getBoardLayout()[row][column];

        // If the cell has a left cage wall, it's starting X position will be increase by regularWall worth of pixels
        int drawFromX = cell.isCageLeft() ?
                getCellStart(column, column + 1) : getCellStart(column);

        // If the cell has a top cage wall, it's starting Y position will be increase by regularWall worth of pixels
        int drawFromY = cell.isCageTop() ?
                getCellStart(row, row + 1) : getCellStart(row);

        int drawDistanceX = setDistance(cell.isCageLeft(), cell.isCageRight());
        int drawDistanceY = setDistance(cell.isCageTop(), cell.isCageBottom());

        // Canvas will be displayed over the hint cell
        Canvas canvas = new Canvas(drawDistanceX, drawDistanceY);

        canvas.setTranslateX(mainBoard.getLayoutX() + drawFromX);
        canvas.setTranslateY(mainBoard.getLayoutY() + drawFromY);

        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.setFill(Color.PINK);
        graphicsContext.fillRect(0, 0, drawDistanceX, drawDistanceY);

        drawHintValue(row, column, graphicsContext);
        drawHintLabel(row, column, graphicsContext);

        // Make hint appear and fade out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(3000), canvas);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        rootContainer.getChildren().add(canvas);

        // Once the animation finishes, removes the hint canvas
        fadeOut.setOnFinished(event -> rootContainer.getChildren().remove(canvas));

        fadeOut.play();
    }

    /**
     * Draws the correct value of the cell on the hint canvas
     * @param row the row of the correct cell
     * @param column the column of the correct cell
     * @param graphicsContext the graphics context of the hint canvas
     */
    private void drawHintValue(int row, int column, GraphicsContext graphicsContext) {

        Font valueFont = Font.font("Verdana", FontWeight.BOLD, determineValueSize());
        graphicsContext.setFont(valueFont);

        FontMetrics fontMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(valueFont);
        graphicsContext.setFill(Color.BLACK);

        double positionX, positionY;

        // FIXME: 21/02/2021 computeStringWidth removed in new version of javafx
        positionX = ( (double) cellSize - fontMetrics.computeStringWidth(
                gameLogic.getSolver().getSolvedBoard()[row][column] + "")) / 2;

        positionY = ( (double) cellSize + fontMetrics.getLineHeight()) / 2;

        graphicsContext.fillText(gameLogic.getSolver().getSolvedBoard()[row][column] + "",
                positionX, positionY);
    }

    /**
     * Draws the correct label of the cell on the hint canvas
     * @param row the row of the correct cell
     * @param column the column of the correct cell
     * @param graphicsContext the graphics context of the hint canvas
     */
    private void drawHintLabel(int row, int column, GraphicsContext graphicsContext) {

        Font labelFont = new Font(determineLabelSize());
        graphicsContext.setFont(labelFont);

        FontMetrics fontMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(labelFont);

        double offset = (double) cellSize / 20;

        graphicsContext.fillText(gameLogic.getBoard().getBoardLayout()[row][column].getLabel(),
                offset, fontMetrics.getLineHeight() / 2 + offset);
    }

    /**
     * Plays an animation once winning conditions are reached
     */
    private void playWinAnimation() {

        rootContainer.setDisable(true);

        int fireworkAmount = 20;
        int frames = 30;
        int frameSize = 256;

        Image image = new Image("/Firework.png");
        ImageView[][] fireworkAnimation = new ImageView[fireworkAmount][frames];

        ColorAdjust colorAdjust;
        Random random = new Random();

        for(int count = 0; count < fireworkAmount; count++) {

            // Each firework gets a random color
            colorAdjust = new ColorAdjust(Math.random() * 2 - 1, Math.random() * 2 - 1, 0.1, 1);

            // Generate random position for each firework
            int posX = random.nextInt( (int) rootContainer.getWidth() - frameSize);
            int posY = random.nextInt( (int) rootContainer.getHeight() - frameSize);

            for (int row = 0; row < 5; row++) {
                for (int column = 0; column < 6; column++) {

                    fireworkAnimation[count][row * 6 + column] = new ImageView(image);

                    // Apply color and position
                    fireworkAnimation[count][row * 6 + column].setEffect(colorAdjust);
                    fireworkAnimation[count][row * 6 + column].setTranslateX(posX);
                    fireworkAnimation[count][row * 6 + column].setTranslateY(posY);

                    fireworkAnimation[count][row * 6 + column].setViewport(
                            new Rectangle2D(column * frameSize, row * frameSize, frameSize, frameSize));
                }
            }
        }

        Timeline timeline = new Timeline();
        timeline.setCycleCount(2);

        for (ImageView[] firework : fireworkAnimation) {

            int iterations = 0;

            for(ImageView frame : firework) {

                timeline.getKeyFrames().add(new KeyFrame(
                        Duration.millis(iterations * 75),
                        (ActionEvent event) -> rootContainer.getChildren().add(frame)));

                timeline.getKeyFrames().add(new KeyFrame(
                        Duration.millis((iterations + 1) * 75),
                        (ActionEvent event) -> rootContainer.getChildren().remove(frame)));

                iterations++;
            }
        }

        // Once animation is finished, return game to default state
        timeline.setOnFinished(event -> {
            rootContainer.setDisable(false);
            gameLogic.deleteLastBoard();
            drawBoard();
        });

        timeline.play();
    }

    /**
     * Sets up the GUI components
     */
    private void setUpComponents() {

        rootContainer.setAlignment(Pos.TOP_LEFT);

        rootContainer.setPrefWidth(900);
        rootContainer.setPrefHeight(600);

        fontSelection.setText("Font: Medium");
        generateButton.setText("Generate Puzzle");

        topButtons.prefWidthProperty().bind(rootContainer.widthProperty());
        topButtons.prefHeightProperty().bind(rootContainer.heightProperty().divide(10).subtract(1));

        bottomButtons.prefWidthProperty().bind(rootContainer.widthProperty());
        bottomButtons.prefHeightProperty().bind(rootContainer.heightProperty().divide(10).subtract(1));

        mainBoard.prefWidthProperty().bind(rootContainer.widthProperty());
        mainBoard.prefHeightProperty().bind(rootContainer.heightProperty().divide(10).multiply(8).subtract(1));

        gameGrid.setWidth(Math.min(mainBoard.getWidth() / 10 * 6 - 1, mainBoard.getHeight()));
        gameGrid.setHeight(gameGrid.getWidth());

        numberPad.prefWidthProperty().bind(mainBoard.widthProperty().subtract(gameGrid.getWidth()));
        numberPad.prefHeightProperty().bind(mainBoard.heightProperty());

        gameBoard.prefWidthProperty().bind(rootContainer.widthProperty());
        gameBoard.prefHeightProperty().bind(rootContainer.heightProperty());

        setTopButtonSize(undoButton);
        setTopButtonSize(redoButton);
        setTopButtonSize(clearButton);
        setTopButtonSize(hintButton);
        setTopButtonSize(mistakesButton);
        setTopButtonSize(solveButton);

        setNumPadButton(oneButton);
        setNumPadButton(twoButton);
        setNumPadButton(threeButton);
        setNumPadButton(fourButton);
        setNumPadButton(fiveButton);
        setNumPadButton(sixButton);
        setNumPadButton(sevenButton);
        setNumPadButton(eightButton);
        setNumPadButton(nineButton);

        deleteButton.prefWidthProperty().bind(oneButton.widthProperty()
                .add(oneButton.widthProperty()).add(oneButton.widthProperty()));
        deleteButton.prefHeightProperty().bind(mainBoard.heightProperty().divide(4).subtract(1));

        setBottomButtonSize(loadFileButton);
        setBottomButtonSize(loadTextButton);
        setBottomButtonSize(fontSelection);
        setBottomButtonSize(generateButton);

        cellSize = (int) (gameGrid.getWidth() - (gameLogic.getBoard().getSize() - 1) *
                regularWall - 2 * outsideWall) / gameLogic.getBoard().getSize();
    }

    /**
     * Sets the preferences for a single button in the top section
     * @param button the Button to be set up
     */
    private void setTopButtonSize(Button button) {

        int buttons = 6;

        // -1 pixel offset so that it wouldn't wrap
        button.prefWidthProperty().bind(topButtons.widthProperty().divide(buttons).subtract(1));
        button.prefHeightProperty().bind(topButtons.heightProperty());
    }

    /**
     * Sets the preferences for a single button in the bottom section
     * @param button the Button to be set up
     */
    private void setBottomButtonSize(ButtonBase button) {

        int buttons = 4;

        // -1 pixel offset so that it wouldn't wrap
        button.prefWidthProperty().bind(bottomButtons.widthProperty().divide(buttons).subtract(1));
        button.prefHeightProperty().bind(bottomButtons.heightProperty());
    }

    /**
     * Sets the preferences for a single button in the num pad section
     * @param button the Button to be set up
     */
    private void setNumPadButton(Button button) {

        int rowButtons = 3;
        int columnButtons = 4;

        // -1 pixel offset so that it wouldn't wrap
        button.prefWidthProperty().bind(mainBoard.widthProperty().divide(rowButtons).subtract(1));
        button.prefHeightProperty().bind(mainBoard.heightProperty().divide(columnButtons).subtract(1));
    }

    /**
     * Disables / enables number pad buttons depending on the board size
     */
    public void disableNumPad() {

        nineButton.setDisable(gameLogic.getBoard().getSize() < 9);
        eightButton.setDisable(gameLogic.getBoard().getSize() < 8);
        sevenButton.setDisable(gameLogic.getBoard().getSize() < 7);
        sixButton.setDisable(gameLogic.getBoard().getSize() < 6);
        fiveButton.setDisable(gameLogic.getBoard().getSize() < 5);
        fourButton.setDisable(gameLogic.getBoard().getSize() < 4);
        threeButton.setDisable(gameLogic.getBoard().getSize() < 3);
    }

    /**
     * Sets new size for components
     */
    private void resizeComponents() {

        mainBoard.setMinHeight(rootContainer.getHeight() / 10 * 8 - 1);

        gameGrid.setWidth(mainBoard.getHeight());
        gameGrid.setHeight(gameGrid.getWidth());

        // Resize wall size depending on canvas size
        if(gameGrid.getWidth() < 800) {
            regularWall = 1;
            outsideWall = 2;
            cageWall = 3;
        } else {
            regularWall = 2;
            outsideWall = 3;
            cageWall = 6;
        }

        // Resize the cell size depending on canvas size
        cellSize = (int)(gameGrid.getWidth() - (gameLogic.getBoard().getSize() - 1) *
                regularWall - 2 * outsideWall) / gameLogic.getBoard().getSize();
    }

    /**
     * Draws the board of the game
     */
    public void drawBoard() {

        resizeComponents();

        // Clears the canvas
        boardGraphics.clearRect(0, 0, gameGrid.getWidth(), gameGrid.getHeight());

        /*
           Calculates the new size of the board on canvas, since canvas is not always pixel
           precise due to division to calculate cell size
         */
        boardSize = outsideWall * 2 + gameLogic.getBoard().getSize() * cellSize +
                (gameLogic.getBoard().getSize() - 1) * regularWall;

        // Disables / enables undo and redo buttons
        updateButtonStatus();

        // Draws the cells
        drawCells();

        // Checks if winning condition is reached, if not - finds mistakes
        if(gameLogic.isWinReached()) {
            playWinAnimation();
        }

        highlightMistakes();

        // Draws the selected cell
        drawSelected();

        // Fills the corners
        drawCorners();

        // Draws the labels for each cell
        drawLabels();

        // Draws the values for each cell
        drawValues();
    }

    /**
     * Disables / enables action buttons
     */
    private void updateButtonStatus() {
        undoButton.setDisable(!gameLogic.isUndoPossible());
        redoButton.setDisable(!gameLogic.isRedoPossible());
        hintButton.setDisable(gameLogic.getClusterCells().isEmpty());
        solveButton.setDisable(gameLogic.getSolver().getSolvedBoard() == null);
    }

    /**
     * Draws the cells of the board
     */
    private void drawCells() {

        boardGraphics.setFill(Color.BLACK);
        boardGraphics.fillRect(0, 0, boardSize, boardSize);
        boardGraphics.setFill(Color.WHITE);

        // Start position of cell
        int drawFromX;
        int drawFromY;

        // The size of the cell
        int drawDistanceX;
        int drawDistanceY;

        Logic.Cell cell;

        /*
            Draws the cells of the board following these guidelines:
            If cell has no cage walls on left or right, the cell's size will be equal to cellSize;
            if it has one cage wall on either side, it's size = cellSize - regularWall;
            if 2 cage walls, it's size = cellSize - regularWall * 2.
         */
        for(int i = 0; i < gameLogic.getBoard().getSize(); i++) {

            drawFromX = outsideWall;
            drawFromY = getCellStart(i);

            for(int j = 0; j < gameLogic.getBoard().getSize(); j++) {

                cell = gameLogic.getBoard().getBoardLayout()[i][j];

                // If cell has a top cell wall, starting position Y is higher by regularWall pixels
                drawFromY += cell.isCageTop() ? regularWall : 0;

                // Updates draw distance for a cell according to the type of walls it is surrounded by
                drawDistanceX = setDistance(cell.isCageRight(), cell.isCageLeft());
                drawDistanceY = setDistance(cell.isCageTop(), cell.isCageBottom());

                // Draws a single cell
                boardGraphics.fillRect(drawFromX, drawFromY, drawDistanceX, drawDistanceY);

                // Skip a number of pixels depending on the wall type of the right
                drawFromX += cell.isCageRight() ? drawDistanceX + cageWall : drawDistanceX + regularWall;

                // Reset Y coordinate for the next cell
                drawFromY -= gameLogic.getBoard().getBoardLayout()[i][j].isCageTop() ? regularWall : 0;

            }
        }
    }

    /**
     * Calculates the starting pixel of the cell
     * @param cell the number(position) of the cell
     * @return the staring pixel of the cell provided
     */
    private int getCellStart(int cell) {
        return outsideWall + cell * (cellSize + regularWall);
    }

    /**
     * Calculates the starting pixel of the cell
     * @param cell the number(position) of the cell
     * @param wall the amount of regularWalls until the staring cell
     * @return the staring pixel of next cell provided
     */
    private int getCellStart(int cell, int wall) {
        return outsideWall + cell * cellSize + wall * regularWall;
    }

    /**
     * Calculates the distance that needs to be drawn to create a cell
     * @param side1 one side of the cell
     * @param side2 parallel side to the other
     * @return the distance to be drawn
     */
    private int setDistance(boolean side1, boolean side2) {

        int distance = cellSize;

        // if there is a wall on either side, decrease the draw distance, to make cage wall thicker
        if(side1) {
            distance -= regularWall;
        }
        if(side2) {
            distance -= regularWall;
        }
        return distance;
    }

    /**
     * Draws the cell that is currently chosen
     */
    private void drawSelected() {

        if(selectedCell != null) {
            highlightCell(selectedCell.getRow(), selectedCell.getColumn(), Color.GREEN);
        }
    }

    /**
     * Highlights a single cell
     * @param row the row of the cell (Y coordinate)
     * @param column the column of the cell (X coordinate)
     * @param color the color the cell will be highlighted in
     */
    private void highlightCell(int row, int column, Color color) {

        boardGraphics.setFill(color);

        Logic.Cell cell = gameLogic.getBoard().getBoardLayout()[row][column];

        // If the cell has a left cage wall, it's starting X position will be increase by regularWall worth of pixels
        int drawFromX = cell.isCageLeft() ?
                getCellStart(column, column + 1) : getCellStart(column);

        // If the cell has a top cage wall, it's starting Y position will be increase by regularWall worth of pixels
        int drawFromY = cell.isCageTop() ?
                getCellStart(row, row + 1) : getCellStart(row);

        int drawDistanceX = setDistance(cell.isCageLeft(), cell.isCageRight());
        int drawDistanceY = setDistance(cell.isCageTop(), cell.isCageBottom());

        boardGraphics.fillRect(drawFromX, drawFromY, drawDistanceX, drawDistanceY);
    }

    /**
     * Draws the corners of cage
     */
    private void drawCorners() {

        Logic.Cell[][] cells = gameLogic.getBoard().getBoardLayout();

        boardGraphics.setFill(Color.BLACK);

        // Draws bottom right corners
        for(int i = 0; i < gameLogic.getBoard().getSize() - 1; i++) {
            for(int j = 0; j < gameLogic.getBoard().getSize() - 1; j++) {

                // Draw corner if cell has right and bottom cage walls, while diagonal cell from it doesn't have left and top as cage walls
                if(cells[i][j].isCageRight() && cells[i][j].isCageBottom() &&
                        !cells[i + 1][j + 1].isCageLeft() && !cells[i + 1][j + 1].isCageTop()) {

                    // Draws a regularWall X regularWall size rectangle as a corner
                    boardGraphics.fillRect(getCellStart(j + 1), getCellStart(i + 1),
                            regularWall, regularWall);
                }
            }
        }

        // Draws bottom left corners
        for(int i = 0; i < gameLogic.getBoard().getSize() - 1; i++) {
            for(int j = 1; j < gameLogic.getBoard().getSize(); j++) {

                // Draw corner if cell has left and bottom cage walls, while diagonal cell from it doesn't have right and top as cage walls
                if(cells[i][j].isCageLeft() && cells[i][j].isCageBottom() &&
                        !cells[i + 1][j - 1].isCageRight() && !cells[i + 1][j - 1].isCageTop()) {

                    boardGraphics.fillRect(getCellStart(j, j - 2), getCellStart(i + 1),
                            regularWall, regularWall);
                }
            }
        }

        // Draws top right corners
        for(int i = 1; i < gameLogic.getBoard().getSize(); i++) {
            for(int j = 0; j < gameLogic.getBoard().getSize() - 1; j++) {

                // Draw corner if cell has right and top cage walls, while diagonal cell from it doesn't have left and bottom as cage walls
                if(cells[i][j].isCageRight() && cells[i][j].isCageTop() &&
                        !cells[i - 1][j + 1].isCageLeft() && !cells[i - 1][j + 1].isCageBottom()) {

                    boardGraphics.fillRect(getCellStart(j + 1), getCellStart(i, i - 2),
                            regularWall, regularWall);
                }
            }
        }

        // Draws top left corners
        for(int i = 1; i < gameLogic.getBoard().getSize(); i++) {
            for(int j = 1; j < gameLogic.getBoard().getSize(); j++) {

                // Draw corner if cell has left and top cage walls, while diagonal cell from it doesn't have right and bottom as cage walls
                if(cells[i][j].isCageLeft() && cells[i][j].isCageTop() &&
                        !cells[i - 1][j - 1].isCageRight() && !cells[i - 1][j - 1].isCageBottom()) {

                    boardGraphics.fillRect(getCellStart(j, j - 2), getCellStart(i, i - 2),
                            regularWall, regularWall);
                }
            }
        }
    }

    /**
     * Draws the labels for each cell
     */
    private void drawLabels() {

        Font labelFont = new Font(determineLabelSize());
        boardGraphics.setFont(labelFont);

        FontMetrics fontMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(labelFont);

        Logic.Cell cell;
        double offset = (double)cellSize / 20;

        for(int i = 0; i < gameLogic.getBoard().getSize(); i++) {
            for(int j = 0; j < gameLogic.getBoard().getSize(); j++) {

                cell = gameLogic.getBoard().getBoardLayout()[i][j];

                // For each non empty label
                if(!cell.getLabel().isEmpty()) {

                    // Draws label at top left corner with some offset from top and left
                    boardGraphics.fillText(cell.getLabel(), getCellStart(j) + offset,
                            fontMetrics.getLineHeight() / 2 + getCellStart(i) + offset);
                }
            }
        }
    }

    /**
     * Draws the values for each cell
     */
    private void drawValues() {

        Font valueFont = Font.font("Verdana", FontWeight.BOLD, determineValueSize());
        boardGraphics.setFont(valueFont);

        FontMetrics fontMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(valueFont);

        Logic.Cell cell;
        double positionX, positionY;

        for(int i = 0; i < gameLogic.getBoard().getSize(); i++) {
            for(int j = 0; j < gameLogic.getBoard().getSize(); j++) {

                cell = gameLogic.getBoard().getBoardLayout()[i][j];

                // For each non empty cell
                if(cell.getValue() != 0) {

                    // FIXME: 21/02/2021 computeStringWidth removed in new version of javafx
                    // draw from x = position inside the cell + choosing the right cell
                    positionX = ( (double) cellSize - fontMetrics.computeStringWidth(
                            cell.getValue() + "")) / 2 + getCellStart(j);

                    // draw from y = position inside the cell + choosing the right cell
                    positionY = ( (double) cellSize + fontMetrics.getLineHeight()) / 2 + getCellStart(i);

                    boardGraphics.fillText(cell.getValue() + "", positionX, positionY);
                }
            }
        }
    }

    /**
     * Calculates the appropriate size for cells' labels
     * @return the size of the label in pixels
     */
    private int determineLabelSize() {

        switch(fontSize) {
            case SMALL:
                return cellSize / 6;
            case MEDIUM:
                return cellSize / 5;
            case LARGE:
                return cellSize / 4;

            // By default font size is Medium
            default:
                return cellSize / 5;
        }
    }

    /**
     * Calculates the appropriate size for cells' values
     * @return the size of the value in pixels
     */
    private int determineValueSize() {

        switch(fontSize) {
            case SMALL:
                return cellSize / 9 * 3;
            case MEDIUM:
                return cellSize / 9 * 4;
            case LARGE:
                return cellSize / 9 * 5;

            // By default font size is Medium
            default:
                return cellSize / 9 * 4;
        }
    }

    /**
     * Highlights all mistakes inside the game board
     */
    private void highlightMistakes() {

        if(gameLogic.isHintsEnabled()) {
            highlightRows(gameLogic.getIncorrectRows());
            highlightColumns(gameLogic.getIncorrectColumns());
            highlightCages(gameLogic.getIncorrectCages());
        }
    }

    /**
     * Highlights all the cells inside a row
     * @param rows the rows of cells to be highlighted
     */
    private void highlightRows(List<Integer> rows) {

        for(int i = 0; i < rows.size(); i++) {
            for(int j = 0; j < gameLogic.getBoard().getSize(); j++) {

                highlightCell(rows.get(i), j, Color.FIREBRICK);
            }
        }
    }

    /**
     * Highlights all the cells inside a column
     * @param columns the columns of cells to be highlighted
     */
    private void highlightColumns(List<Integer> columns) {

        for(int i = 0; i < columns.size(); i++) {
            for(int j = 0; j < gameLogic.getBoard().getSize(); j++) {

                highlightCell(j, columns.get(i), Color.FIREBRICK);
            }
        }
    }

    /**
     * Highlights all the cells inside a cage
     * @param clusterCells a cage of cells inside a board to be highlighted
     */
    private void highlightCages(List<List<Logic.CellPos> > clusterCells) {

        for(List<Logic.CellPos> cage : clusterCells) {
            for(Logic.CellPos cell : cage) {

                highlightCell(cell.getRow(), cell.getColumn(), Color.FIREBRICK);
            }
        }
    }

    /**
     * Determines which cell is selected
     * @param posX mouse coordinate X
     * @param posY mouse coordinate Y
     */
    public void selectCell(double posX, double posY) {

        /*
        Because the number of cells times cell size is less than the size of the board (due to outside walls),
        some offsetting is needed
         */
        if(posX >= outsideWall) {

            // if the right outside wall is clicked
            if(posX + outsideWall >= gameGrid.getWidth()) {
                posX -= outsideWall;
            }
            posX -= outsideWall;
        }

        if(posY >= outsideWall) {

            // if bottom outside wall is clicked
            if(posY + outsideWall >= gameGrid.getHeight()) {
                posY -= outsideWall;
            }
            posY -= outsideWall;
        }

        if( (int) (posY / (cellSize + regularWall)) < gameLogic.getBoard().getSize() &&
                (int) (posX / (cellSize + regularWall)) < gameLogic.getBoard().getSize()) {

            selectedCell = new Logic.CellPos( (int) (posY / (cellSize + regularWall)),
                    (int) (posX / (cellSize + regularWall)));

        }
    }

    /**
     * Get the coordinates of the selected cell
     * @return the coordinates of the selected cell
     */
    public Logic.CellPos getSelectedCell() {
        return selectedCell;
    }

    /**
     * Set the value of the selected cell
     * @param selectedCell the coordinates of the selected cell
     */
    public void setSelectedCell(Logic.CellPos selectedCell) {
        this.selectedCell = selectedCell;
    }

    /**
     * Returns the possible font size options
     * @return font size options
     */
    public SplitMenuButton getFontSelection() {
        return fontSelection;
    }

    /**
     * Returns small font size
     * @return small font size
     */
    public MenuItem getFontSmall() {
        return fontSmall;
    }

    /**
     * Returns medium font size
     * @return medium font size
     */
    public MenuItem getFontMedium() {
        return fontMedium;
    }

    /**
     * Returns large font size
     * @return large font size
     */
    public MenuItem getFontLarge() {
        return fontLarge;
    }

    /**
     * Returns the button that generates new puzzles
     * @return the new puzzle generation button
     */
    public SplitMenuButton getGenerateButton() {
        return generateButton;
    }

    /**
     * Returns the root container
     * @return the root container
     */
    public StackPane getRootContainer() {
        return rootContainer;
    }

    /**
     * Returns the button that displays a hint
     * @return the button that displays a hint
     */
    public Button getHintButton() {
        return hintButton;
    }

    /**
     * Returns the button that enters the value '1'
     * @return the button that enter the value '1'
     */
    public Button getOneButton() {
        return oneButton;
    }

    /**
     * Returns the button that enters the value '2'
     * @return the button that enter the value '2'
     */
    public Button getTwoButton() {
        return twoButton;
    }

    /**
     * Returns the button that enters the value '3'
     * @return the button that enter the value '3'
     */
    public Button getThreeButton() {
        return threeButton;
    }

    /**
     * Returns the button that enters the value '4'
     * @return the button that enter the value '4'
     */
    public Button getFourButton() {
        return fourButton;
    }

    /**
     * Returns the button that enters the value '5'
     * @return the button that enter the value '5'
     */
    public Button getFiveButton() {
        return fiveButton;
    }

    /**
     * Returns the button that enters the value '6'
     * @return the button that enter the value '6'
     */
    public Button getSixButton() {
        return sixButton;
    }

    /**
     * Returns the button that enters the value '7'
     * @return the button that enter the value '7'
     */
    public Button getSevenButton() {
        return sevenButton;
    }

    /**
     * Returns the button that enters the value '8'
     * @return the button that enter the value '8'
     */
    public Button getEightButton() {
        return eightButton;
    }

    /**
     * Returns the button that enters the value '9'
     * @return the button that enter the value '9'
     */
    public Button getNineButton() {
        return nineButton;
    }

    /**
     * Returns the button that deletes a cell value
     * @return the button that deletes a cell value
     */
    public Button getDeleteButton() {
        return deleteButton;
    }

    /**
     * Returns the button that undoes the last user action
     * @return the button that undoes the last user action
     */
    public Button getUndoButton() {
        return undoButton;
    }

    /**
     * Returns the button that redoes the last undone action
     * @return the button that redoes the last undone action
     */
    public Button getRedoButton() {
        return redoButton;
    }

    /**
     * Returns the button that clears the values from game board
     * @return the button that clears the values from game board
     */
    public Button getClearButton() {
        return clearButton;
    }

    /**
     * Returns the button that solves the current puzzle
     * @return the button that solves the current puzzle
     */
    public Button getSolveButton() {
        return solveButton;
    }

    /**
     * Returns the button that loads a new puzzle from file
     * @return the button that loads a new puzzle from file
     */
    public Button getLoadFileButton() {
        return loadFileButton;
    }

    /**
     * Returns the button that loads a new puzzle from text
     * @return the button that loads a new puzzle from text
     */
    public Button getLoadTextButton() {
        return loadTextButton;
    }

    /**
     * Returns the button that displays mistakes in the game board
     * @return the button that displays mistakes in the game board
     */
    public Button getMistakesButton() {
        return mistakesButton;
    }

    /**
     * Returns the logical representation of the current puzzle
     * @return the logical representation of the current puzzle
     */
    public Logic getGameLogic() {
        return gameLogic;
    }

    /**
     * Returns the main scene of the game
     * @return the main scene of the game
     */
    public Scene getPrimaryScene() {
        return primaryScene;
    }

    /**
     * Returns the grid of the game
     * @return the grid of the game
     */
    public Canvas getGameGrid() {
        return gameGrid;
    }

    /**
     * Sets the font size for the game
     * @param fontSize the new font size to be used
     */
    public void setFontSize(FontSize fontSize) {
        this.fontSize = fontSize;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
