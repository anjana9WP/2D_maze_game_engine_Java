package edu.curtin.saed.assignment2;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.util.List;
import java.util.ArrayList;
import java.io.FileReader;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import java.util.ResourceBundle;
import java.util.Locale;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.MissingResourceException;
import java.text.MessageFormat;
import java.lang.reflect.Constructor;
import edu.curtin.assignment2.api.GameAPI;
import edu.curtin.assignment2.api.Callback;
import java.util.Map;
import java.util.HashMap;
import javafx.application.Platform;
import java.lang.reflect.InvocationTargetException;
import org.python.util.PythonInterpreter;
import org.python.core.*;


public class App extends Application implements GameAPI
{
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private boolean[][] visible;  // To track which squares are visible
    private GridAreaIcon[][] gridIcons; // To store the icons at each grid position
    private int rows = 10;
    private int cols = 10;
    private Image hiddenImage;  // Single instance of hidden.png
    private TextField inputField;  // Field for text input
    private int playerRow = 2;
    private int playerCol = 2;
    private GridArea area;  // Reference to the GridArea
    private TextArea sidebarTextArea;  // Add this field to reference the sidebar TextArea
    private boolean gameEnded = false;  // Add this new field
    private MazeParser.MazeData mazeData;  // Add this field
    private List<String> inventory = new ArrayList<>();
    private TextArea inventoryTextArea;
    private boolean cheatMode = false;
    private ResourceBundle messages;
    private LocalDate currentDate;
    private Label dateLabel;
    private TextField languageField;
    private Button cheatBtn;
    private Button applyLanguageBtn;
    private Stage stage;
    private List<Object> plugins = new ArrayList<>();
    private String lastAcquiredItem;
    private Map<String, Callback> menuCallbacks = new HashMap<>();
    private List<Callback> itemCallbacks = new ArrayList<>();
    private int moveCount = 0;
    private ScriptHandler scriptHandler;
    private List<Callback> moveCallbacks = new ArrayList<>();
    private Button teleportBtn;

    public static void main(String[] args)
    {
        launch();
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void start(Stage stage) throws Exception
    {
        try {
            this.stage = stage;  // Store the stage for later use
            // Initialize locale and ResourceBundle at the beginning
            Locale locale = Locale.getDefault();  // Or use a specific locale like new Locale("en", "US")
            messages = ResourceBundle.getBundle("messages", locale);
            
            currentDate = LocalDate.now();
            dateLabel = new Label(formatDate(currentDate));
            
            // Get the input file name from command line arguments or use a default
            List<String> args = getParameters().getRaw();
            String inputFile = args.isEmpty() ? "testinput-0.utf8.map" : args.get(0);

            // Check if the file has a valid extension
            if (!inputFile.endsWith(".utf8.map") && !inputFile.endsWith(".utf16.map") && !inputFile.endsWith(".utf32.map")) {
                throw new IllegalArgumentException("Invalid file extension. Must be .utf8.map, .utf16.map, or .utf32.map");
            }

            // Use the JavaCC-generated parser to read and parse the file
            try (FileReader reader = new FileReader(inputFile)) {
                MazeParser parser = new MazeParser(reader);
                try {
                    this.mazeData = parser.parse();  // Parse the maze data
                } catch (ParseException e) {
                    LOGGER.log(Level.SEVERE, () -> "Error parsing maze file: " + e.getMessage());
                    return;
                }
            }

            // Update grid size based on parsed data
            rows = mazeData.rows;
            cols = mazeData.cols;

            // Initialize visibility and gridIcons arrays
            visible = new boolean[rows][cols];
            gridIcons = new GridAreaIcon[rows][cols];

            // Load images with null checks and error handling
            hiddenImage = loadImage("hidden.png");
            Image playerImage = loadImage("player.png");
            Image goalImage = loadImage("goal.png");
            Image obstacleImage = loadImage("obstacle.png");
            Image itemImage = loadImage("item.png");

            // Set up the grid
            area = new GridArea(rows, cols);
            area.setStyle("-fx-background-color: #006000;");

            // Add player to the grid
            playerRow = mazeData.startRow;
            playerCol = mazeData.startCol;
            if (isValidPosition(playerRow, playerCol)) {
                gridIcons[playerRow][playerCol] = new GridAreaIcon(playerRow, playerCol, 0.0, 1.0, playerImage, "Player");
            } else {
                LOGGER.log(Level.SEVERE, () -> "Invalid player start position: (" + playerRow + ", " + playerCol + ")");
            }

            // Add goal to the grid
            if (isValidPosition(mazeData.goalRow, mazeData.goalCol)) {
                gridIcons[mazeData.goalRow][mazeData.goalCol] = new GridAreaIcon(mazeData.goalRow, mazeData.goalCol, 0.0, 1.0, goalImage, "Goal");
            } else {
                LOGGER.log(Level.SEVERE, () -> "Invalid goal position: (" + mazeData.goalRow + ", " + mazeData.goalCol + ")");
            }

            // Add obstacles to the grid
            for (MazeParser.Obstacle obstacle : mazeData.obstacles) {
                for (int[] loc : obstacle.locations) {
                    if (isValidPosition(loc[0], loc[1])) {
                        gridIcons[loc[0]][loc[1]] = new GridAreaIcon(loc[0], loc[1], 0.0, 1.0, obstacleImage, "Obstacle");
                    } else {
                        LOGGER.log(Level.WARNING, () -> "Invalid obstacle position: (" + loc[0] + ", " + loc[1] + ")");
                    }
                }
            }

            // Add items to the grid
            for (MazeParser.Item item : mazeData.items) {
                for (int[] loc : item.locations) {
                    if (isValidPosition(loc[0], loc[1])) {
                        gridIcons[loc[0]][loc[1]] = new GridAreaIcon(loc[0], loc[1], 0.0, 1.0, itemImage, item.name);
                    } else {
                        LOGGER.log(Level.WARNING, () -> "Invalid item position: (" + loc[0] + ", " + loc[1] + ")");
                    }
                }
            }

            // Make initial squares visible
            makeInitialVisible(playerRow, playerCol);

            // Render the grid
            renderGrid(area);

            // Set up buttons and text
            cheatBtn = new Button(messages.getString("cheat"));
            teleportBtn = new Button(messages.getString("teleport"));

            // Set button actions
            cheatBtn.setOnAction(e -> toggleCheatMode());
            teleportBtn.setOnAction(e -> executeTeleport());

            // Create HBox for buttons
            HBox buttonBox = new HBox(10);
            buttonBox.getChildren().addAll(
                cheatBtn,
                teleportBtn
            );

            var textArea = new TextArea();
            textArea.appendText("Sidebar\n");
            textArea.appendText("Text\n");

            // Create the input field for movement commands
            inputField = new TextField();
            inputField.setPromptText("Enter W, A, S, D");
            inputField.setPrefWidth(150);
            inputField.setOnAction(e -> handleInput());  // Handle input when Enter is pressed

            cheatBtn = new Button(messages.getString("cheat"));
            cheatBtn.setOnAction(e -> toggleCheatMode());

            // Create toolbar with various UI elements
            var toolbar = new ToolBar();
            toolbar.getItems().addAll(
                new Separator(),
                new Separator(), 
                new Label(messages.getString("move")), 
                inputField,
                new Separator(),
                cheatBtn,
                new Separator(),
                teleportBtn
            );

            // Create and set up sidebar TextArea
            sidebarTextArea = new TextArea();
            sidebarTextArea.setEditable(false);
            updateSidebar(mazeData);

            // Create inventory display
            inventoryTextArea = new TextArea();
            inventoryTextArea.setEditable(false);
            inventoryTextArea.setPrefHeight(200);

            // Create a VBox to hold both sidebar and inventory
            VBox rightPanel = new VBox(10);
            rightPanel.getChildren().addAll(sidebarTextArea, inventoryTextArea);

            // Create split pane for main layout
            var splitPane = new SplitPane();
            splitPane.getItems().addAll(area, rightPanel);
            splitPane.setDividerPositions(0.75);

            stage.setTitle("2D Maze Game");
            var contentPane = new BorderPane();
            contentPane.setTop(toolbar);
            contentPane.setCenter(splitPane);

            // Create and set the scene
            var scene = new Scene(contentPane, 1200, 1000);
            stage.setScene(scene);
            stage.show();

            updateInventoryDisplay();

            // Create language input field
            languageField = new TextField();
            languageField.setPromptText("Enter IETF language tag");
            languageField.setPrefWidth(150);

            // Create language apply button
            applyLanguageBtn = new Button("Apply");
            applyLanguageBtn.setOnAction(e -> changeLanguage(languageField.getText()));

            // Add language change elements to toolbar
            toolbar.getItems().addAll(
                new Separator(),
                dateLabel,
                new Separator(),
                new Label(messages.getString("language")),
                languageField,
                applyLanguageBtn
            );

            updateUIText();

            loadPlugins();

            scriptHandler = new ScriptHandler(this);
            loadAndRunScripts();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, () -> "Unexpected error in start method: " + e.getMessage());
            throw e;
        }
    }

    // Method to make the player's square and adjacent squares visible
    public void makeInitialVisible(int row, int col)
    {
        visible[row][col] = true;  // Player's position is visible

        if (row > 0) { visible[row - 1][col] = true; } // Up
        if (row < rows - 1) { visible[row + 1][col] = true; } // Down
        if (col > 0) { visible[row][col - 1] = true; } // Left
        if (col < cols - 1) { visible[row][col + 1] = true; } // Right
    }

    // Method to render the grid with visible and hidden squares
    private void renderGrid(GridArea area) {
        LOGGER.info("renderGrid called");
        List<GridAreaIcon> newIcons = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (visible[i][j] || cheatMode) {
                    if (gridIcons[i][j] != null) {
                        newIcons.add(gridIcons[i][j]);
                        logVisibleIcon(i, j, gridIcons[i][j].getCaption());
                    }
                    // We don't add anything for empty visible squares
                } else {
                    newIcons.add(new GridAreaIcon(i, j, 0.0, 1.0, hiddenImage, "Hidden"));
                    final int row = i, col = j;
                    LOGGER.fine(() -> "Added hidden icon at (" + row + "," + col + ")");
                }
            }
        }

        LOGGER.info(() -> "Updating GridArea with " + newIcons.size() + " icons");
        area.updateIcons(newIcons);
    }


    private Image createDefaultImage() {
        WritableImage img = new WritableImage(32, 32);
        PixelWriter pw = img.getPixelWriter();
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                pw.setColor(x, y, Color.GRAY);
            }
        }
        return img;
    }

    // Method to handle input
    private void handleInput() {
        String input = inputField.getText().toLowerCase().trim();
        if (input.length() == 1 && "wasd".contains(input)) {
            movePlayer(input.charAt(0));
            inputField.clear();  // Clear the input field after processing
        } else {
            System.out.println("Invalid input. Please enter W, A, S, or D.");
        }
    }

    private void movePlayer(char direction) {
        if (gameEnded) {
            appendToSidebar(messages.getString("gameEnded"));
            return;
        }

        int newRow = playerRow;
        int newCol = playerCol;

        switch (Character.toLowerCase(direction)) {
            case 'w': newCol--; break;
            case 's': newCol++; break;
            case 'a': newRow--; break;
            case 'd': newRow++; break;
            default:
                appendToSidebar(messages.getString("moveInstructions"));
                return;
        }

        try {
            movePlayerToNewPosition(newRow, newCol);
            
            // Trigger move callbacks after a successful move
            for (Callback callback : moveCallbacks) {
                callback.execute();
            }
        } catch (IllegalMoveException e) {
            LOGGER.log(Level.WARNING, "Invalid move attempted", e);
            appendToSidebar(e.getMessage());
        }
    }

    private void movePlayerToNewPosition(int newRow, int newCol) throws IllegalMoveException {
        if (!isValidMove(newRow, newCol)) {
            throw new IllegalMoveException(messages.getString("cantMove"));
        }

        MazeParser.Obstacle obstacle = findObstacle(newRow, newCol);
        if (obstacle != null) {
            if (hasRequiredItems(obstacle)) {
                performMove(newRow, newCol);
            } else {
                displayRequiredItems(obstacle);
            }
        } else {
            performMove(newRow, newCol);
        }
    }

    private static class IllegalMoveException extends Exception {
        public IllegalMoveException(String message) {
            super(message);
        }
    }

    private void performMove(int newRow, int newCol) {
        // Check if there's an item in the new position
        if (gridIcons[newRow][newCol] != null && !gridIcons[newRow][newCol].getCaption().equals("Goal")) {
            String itemName = gridIcons[newRow][newCol].getCaption();
            inventory.add(itemName);
            lastAcquiredItem = itemName;  // Set the lastAcquiredItem
            appendToSidebar(MessageFormat.format(messages.getString("itemPickedUp"), itemName));
            gridIcons[newRow][newCol] = null;  // Remove the item from the grid

            // Execute all registered item callbacks
            for (Callback callback : itemCallbacks) {
                callback.execute();
            }
        }

        // Update player position
        gridIcons[playerRow][playerCol] = null;  // Remove player from old position
        playerRow = newRow;
        playerCol = newCol;
        gridIcons[playerRow][playerCol] = new GridAreaIcon(
            playerRow, playerCol, 0.0, 1.0,
            new Image(App.class.getClassLoader().getResourceAsStream("player.png")),
            "Player"
        );

        updateVisibility();
        renderGrid(area);
        updateInventoryDisplay();

        // Increment move count and update date
        moveCount++;
        currentDate = currentDate.plusDays(1);
        updateDateDisplay();

        // Check if player reached the goal
        if (playerRow == mazeData.goalRow && playerCol == mazeData.goalCol) {
            endGame();
        }
    }

    private void updateDateDisplay() {
        dateLabel.setText(formatDate(currentDate));
    }

    private boolean isValidMove(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void updateVisibility() {
        // Reset visibility
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                visible[i][j] = false;
            }
        }

        // Make player's position and adjacent squares visible
        makeInitialVisible(playerRow, playerCol);
    }

    // Add this new method to update the sidebar content
    private void updateSidebar(MazeParser.MazeData mazeData) {
        sidebarTextArea.clear();
        sidebarTextArea.appendText(messages.getString("gameInformation") + "\n\n");
        sidebarTextArea.appendText(MessageFormat.format(messages.getString("goalLocation"), mazeData.goalRow, mazeData.goalCol) + "\n\n");
        sidebarTextArea.appendText(messages.getString("winInstructions") + "\n\n");
        sidebarTextArea.appendText(messages.getString("moveInstructions") + "\n\n");

        sidebarTextArea.appendText(messages.getString("plugins") + "\n");
        for (String plugin : mazeData.plugins) {
            sidebarTextArea.appendText("- " + plugin + "\n");
        }

        sidebarTextArea.appendText("\n" + messages.getString("scripts") + "\n");
        for (String script : mazeData.scripts) {
            sidebarTextArea.appendText("- " + script + "\n");
        }
    }

    private Image loadImage(String imageName) {
        InputStream is = App.class.getClassLoader().getResourceAsStream(imageName);
        if (is == null) {
            LOGGER.log(Level.WARNING, () -> "Cannot find image: " + imageName + ". Using default image.");
            return createDefaultImage();
        }
        return new Image(is);
    }

    private boolean isValidPosition(int row, int col) {
        boolean isValid = row >= 0 && row < rows && col >= 0 && col < cols;
        if (!isValid) {
            LOGGER.log(Level.WARNING, () -> "Invalid position: (" + row + ", " + col + ")");
        }
        return isValid;
    }

    private MazeParser.Obstacle findObstacle(int row, int col) {
        for (MazeParser.Obstacle obstacle : mazeData.obstacles) {
            for (int[] loc : obstacle.locations) {
                if (loc[0] == row && loc[1] == col) {
                    return obstacle;
                }
            }
        }
        return null;
    }

    private boolean hasRequiredItems(MazeParser.Obstacle obstacle) {
        return obstacle.requiredItems.stream()
            .allMatch(requiredItem -> inventory.stream()
                .anyMatch(inventoryItem -> 
                    Normalizer.normalize(requiredItem, Normalizer.Form.NFKC)
                        .equals(Normalizer.normalize(inventoryItem, Normalizer.Form.NFKC))));
    }

    private void updateInventoryDisplay() {
        inventoryTextArea.clear();
        inventoryTextArea.appendText(messages.getString("inventory") + "\n");
        for (String item : inventory) {
            inventoryTextArea.appendText("- " + item + "\n");
        }
    }

    private void toggleCheatMode() {
        cheatMode = !cheatMode;
        renderGrid(area);
    }

    private String formatDate(LocalDate date) {
        String pattern = messages.getString("dateFormat");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault());
        return date.format(formatter);
    }

    private void changeLanguage(String languageTag) {
        try {
            Locale newLocale = Locale.forLanguageTag(languageTag);
            messages = ResourceBundle.getBundle("messages", newLocale);
            Locale.setDefault(newLocale); // Set the default locale
            updateUIText();
            updateDateDisplay(); // Add this line to update the date display
        } catch (MissingResourceException e) {
            LOGGER.log(Level.SEVERE, () -> "Error loading resource bundle: " + e.getMessage());
            // Show an error message to the user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Language Error");
            alert.setHeaderText("Invalid Language Tag");
            alert.setContentText("The specified language is not supported or the resource bundle is missing.");
            alert.showAndWait();
        }
    }

    private void updateUIText() {
        if (messages == null) {
            LOGGER.log(Level.WARNING, () -> "ResourceBundle is null");
            return;
        }

        try {
            stage.setTitle(messages.getString("gameTitle"));
            cheatBtn.setText(messages.getString("cheat"));
            applyLanguageBtn.setText(messages.getString("apply"));
            teleportBtn.setText(messages.getString("teleport")); // This line should now work
            dateLabel.setText(formatDate(currentDate));
            // ... other UI updates ...

            updateSidebar(mazeData);
            updateInventoryDisplay();
            renderGrid(area);
        } catch (MissingResourceException e) {
            LOGGER.log(Level.SEVERE, () -> "Missing resource key: " + e.getKey());
        }
    }

    private void displayRequiredItems(MazeParser.Obstacle obstacle) {
        String requiredItems = String.join(", ", obstacle.requiredItems);
        String message = MessageFormat.format(messages.getString("requiredItems"), requiredItems);
        appendToSidebar(messages.getString("cantMove"));
        appendToSidebar(message);
    }

    @Override
    public void appendToSidebar(String message) {
        sidebarTextArea.appendText("\n" + message);
        sidebarTextArea.setScrollTop(Double.MAX_VALUE);
    }

    private void loadPlugins() {
        for (String pluginClassName : mazeData.plugins) {
            try {
                Class<?> pluginClass = Class.forName(pluginClassName);
                if (GameAPI.class.isAssignableFrom(pluginClass)) {
                    Constructor<?> constructor = pluginClass.getConstructor(GameAPI.class);
                    Object plugin = constructor.newInstance(this);
                    plugins.add(plugin);
                    LOGGER.info(() -> "Successfully loaded plugin: " + pluginClassName);
                } else {
                    LOGGER.warning(() -> "Plugin class does not implement GameAPI: " + pluginClassName);
                }
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | 
                     IllegalAccessException | InvocationTargetException e) {
                LOGGER.log(Level.SEVERE, () -> "Error loading plugin: " + pluginClassName + ": " + e.getMessage());
            }
        }
    }

    // Implement GameAPI methods
    @Override
    public void registerMoveCallback(Callback moveCallback) {
        moveCallbacks.add(moveCallback);
    }

    @Override
    public void registerItemCallback(Callback itemCallback) {
        itemCallbacks.add(itemCallback);
    }

    @Override
    public void registerMenuCallback(String optionName, Callback menuCallback) {
        menuCallbacks.put(optionName, menuCallback);
    }

    @Override
    public int[] getPlayerLocation() {
        return new int[]{playerRow, playerCol};
    }

    @Override
    public void setPlayerLocation(int row, int col) {
        if (isValidPosition(row, col)) {
            // Remove player from old position
            gridIcons[playerRow][playerCol] = null;

            // Update player position
            playerRow = row;
            playerCol = col;

            // Add player to new position
            gridIcons[playerRow][playerCol] = new GridAreaIcon(
                playerRow, playerCol, 0.0, 1.0,
                new Image(App.class.getClassLoader().getResourceAsStream("player.png")),
                "Player"
            );

            updateVisibility();
            renderGrid(area);
            
            // Update sidebar with new player location
            appendToSidebar(MessageFormat.format(messages.getString("playerMoved"), row, col));

            // Check if the player has reached the goal
            checkGoalReached();
        } else {
            LOGGER.log(Level.WARNING, () -> "Attempted to move player to invalid position: (" + row + ", " + col + ")");
        }
    }

    // ... implement other GameAPI methods ...

    @Override
    public void addMenuOption(String optionName) {
        // This method is called by plugins, but we've already added the button in start()
        // So we don't need to do anything here for now
    }

    @Override
    public String getGridSquareContents(int row, int col) {
        if (gridIcons[row][col] != null) {
            return gridIcons[row][col].getCaption();
        }
        return null;
    }

    @Override
    public String getLastAcquiredItem() {
        return lastAcquiredItem;
    }

    @Override
    public boolean isGridSquareVisible(int row, int col) {
        return visible[row][col] || cheatMode;
    }

    @Override
    public List<String> getPlayerInventory() {
        return new ArrayList<>(inventory);
    }

    @Override
    public int[] getGridSize() {
        return new int[]{rows, cols};
    }

    @Override
    public void setGridSquareVisible(int row, int col, boolean visible) {
        if (isValidPosition(row, col)) {
            LOGGER.info(() -> "Setting grid square visibility for (" + row + "," + col + ") to " + visible);
            this.visible[row][col] = visible;
            renderGrid(area); // Directly render the grid after setting visibility
        } else {
            LOGGER.warning(() -> "Attempted to set visibility for invalid position: (" + row + "," + col + ")");
        }
    }


    @Override
    public void updateUI() {
        LOGGER.info("updateUI called");
        Platform.runLater(() -> {
            renderGrid(area); // Ensure the grid is rendered on the JavaFX application thread
            area.requestLayout();
        });
    }


    private void checkGoalReached() {
        if (playerRow == mazeData.goalRow && playerCol == mazeData.goalCol) {
            appendToSidebar(messages.getString("congratulations"));
            gameEnded = true;
            appendToSidebar(messages.getString("gameEnded"));
        }
    }

    @Override
    public void setMultipleGridSquaresVisible(List<int[]> positions) {
        LOGGER.info(() -> "setMultipleGridSquaresVisible called with " + positions.size() + " positions");
        boolean anyChanges = false;

        for (int[] pos : positions) {
            if (isValidPosition(pos[0], pos[1])) {
                // Set the visibility to true for each grid square and log the change
                if (!this.visible[pos[0]][pos[1]]) {
                    this.visible[pos[0]][pos[1]] = true;
                    anyChanges = true;
                    LOGGER.info(() -> "Set position (" + pos[0] + "," + pos[1] + ") to visible");
                }
            } else {
                LOGGER.warning(() -> "Attempted to set visibility for invalid position: (" + pos[0] + "," + pos[1] + ")");
            }
        }

        if (anyChanges) {
            LOGGER.info("Changes made. Updating UI.");
            updateUI();
        } else {
            LOGGER.info("No changes made to visibility.");
        }
    }

    private void logVisibleIcon(int i, int j, String caption) {
        LOGGER.info(() -> "Added visible icon at (" + i + "," + j + "): " + caption);
    }

    private void endGame() {
        appendToSidebar(messages.getString("congratulations"));
        gameEnded = true;
        String endMessage = MessageFormat.format(messages.getString("gameEndedWithMoves"), moveCount);
        appendToSidebar(endMessage);
        
        // Display a dialog with the game end information
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(messages.getString("gameOverTitle"));
            alert.setHeaderText(messages.getString("congratulations"));
            alert.setContentText(endMessage);
            alert.showAndWait();
        });
    }

    private void loadAndRunScripts() {
        for (String scriptContent : mazeData.scripts) {
            scriptHandler.runScript(scriptContent);
            // Add the script content to the sidebar for display
            appendToSidebar("Script loaded:\n" + scriptContent);
        }
    }

    private class ScriptHandler {
        private final GameAPI api;
        private final PythonInterpreter interpreter;

        public ScriptHandler(GameAPI api) {
            this.api = api;
            this.interpreter = new PythonInterpreter();
            interpreter.set("api", this.api);  // Use the api field here
        }

        public void runScript(String pythonScript) {
            try {
                // Replace \n with actual newlines
                pythonScript = pythonScript.replace("\\n", "\n");
                interpreter.exec(pythonScript);
                LOGGER.info(() -> "Successfully executed script using API: " + this.api.getClass().getSimpleName());
            } catch (PyException e) {
                LOGGER.log(Level.SEVERE, () -> "Error executing script with API " + this.api.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void executeTeleport() {
        String teleportText = messages.getString("teleport");
        Callback teleportCallback = menuCallbacks.get(teleportText);
        if (teleportCallback != null) {
            teleportCallback.execute();
            updateUI(); // Ensure the UI is updated after teleportation
        } else {
            LOGGER.warning("No teleport callback registered");
            appendToSidebar(messages.getString("teleportNotAvailable"));
        }
    }

    @Override
    public String getTranslation(String key) {
        return messages.getString(key);
    }

}
