package edu.curtin.gameplugins.Penalty;

import edu.curtin.assignment2.api.GameAPI;
import edu.curtin.assignment2.api.Callback;
import java.util.List;


public class Penalty implements GameAPI {
    private GameAPI api;
    private long lastMoveTime;

    public Penalty(GameAPI api) {
        this.api = api;
        this.lastMoveTime = System.currentTimeMillis();
        api.registerMoveCallback(this::checkMoveTime);
        api.appendToSidebar("Penalty Plugin initialized");
    }

    private void checkMoveTime() {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastMoveTime;

        api.appendToSidebar("Time since last move: " + timeDifference + " ms");

        if (timeDifference > 5000) { // 5 seconds in milliseconds
            createPenaltyObstacle();
        }

        lastMoveTime = currentTime;
    }

    private void createPenaltyObstacle() {
        int[] playerLocation = api.getPlayerLocation();
        int[] gridSize = api.getGridSize();
        int row = playerLocation[0];
        int col = playerLocation[1];

        // Try to place the obstacle in an adjacent, empty cell
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Up, Down, Left, Right
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < gridSize[0] && newCol >= 0 && newCol < gridSize[1]) {
                if (api.getGridSquareContents(newRow, newCol) == null) {
                    // Mark the square as visible (assuming this makes it an obstacle)
                    api.setGridSquareVisible(newRow, newCol, true);
                    api.appendToSidebar("Penalty obstacle created at (" + newRow + ", " + newCol + ")");
                    return;
                }
            }
        }

        api.appendToSidebar("No space for penalty obstacle!");
    }

    // Implement other GameAPI methods by delegating to the api object
    @Override
    public void registerMoveCallback(Callback moveCallback) {
        api.registerMoveCallback(moveCallback);
    }

    @Override
    public void registerItemCallback(Callback itemCallback) {
        api.registerItemCallback(itemCallback);
    }

    @Override
    public void registerMenuCallback(String optionName, Callback menuCallback) {
        api.registerMenuCallback(optionName, menuCallback);
    }

    @Override
    public int[] getPlayerLocation() {
        return api.getPlayerLocation();
    }

    @Override
    public void setPlayerLocation(int row, int col) {
        api.setPlayerLocation(row, col);
    }

    @Override
    public List<String> getPlayerInventory() {
        return api.getPlayerInventory();
    }

    @Override
    public String getLastAcquiredItem() {
        return api.getLastAcquiredItem();
    }

    @Override
    public int[] getGridSize() {
        return api.getGridSize();
    }

    @Override
    public String getGridSquareContents(int row, int col) {
        return api.getGridSquareContents(row, col);
    }

    @Override
    public boolean isGridSquareVisible(int row, int col) {
        return api.isGridSquareVisible(row, col);
    }

    @Override
    public void setGridSquareVisible(int row, int col, boolean visible) {
        api.setGridSquareVisible(row, col, visible);
    }

    @Override
    public void addMenuOption(String optionName) {
        // This method is called by the core game, so we don't need to implement it
    }

    @Override
    public void appendToSidebar(String text) {
        api.appendToSidebar(text);
    }

    @Override
    public void updateUI() {
        api.updateUI();
    }

    @Override
    public void setMultipleGridSquaresVisible(List<int[]> positions) {
        api.setMultipleGridSquaresVisible(positions);
    }

    @Override
    public String getTranslation(String key) {
        return api.getTranslation(key);
    }
}
