package edu.curtin.assignment2.teleport;

import edu.curtin.assignment2.api.GameAPI;
import edu.curtin.assignment2.api.Callback;
import java.util.List;
import java.util.Random;

public class Teleport implements GameAPI {
    private GameAPI api;
    private boolean teleportUsed = false;
    private Random random = new Random();

    public Teleport(GameAPI api) {
        this.api = api;
        String teleportText = api.getTranslation("teleport");
        api.registerMenuCallback(teleportText, this::teleportPlayer);
    }

    private void teleportPlayer() {
        if (!teleportUsed) {
            int[] gridSize = api.getGridSize();
            int newRow = random.nextInt(gridSize[0]);
            int newCol = random.nextInt(gridSize[1]);
            api.setPlayerLocation(newRow, newCol);
            teleportUsed = true;
            api.appendToSidebar("Player teleported to (" + newRow + ", " + newCol + ")");
            // No need to call updateUI() here if setPlayerLocation() already updates the UI
            
            // Check if the game has ended after teleportation
            int[] playerLocation = api.getPlayerLocation();
            if (playerLocation[0] == newRow && playerLocation[1] == newCol) {
                // The game hasn't ended, so we can continue
            } else {
                // The game has ended (likely because the player reached the goal)
                teleportUsed = false; // Reset teleport usage for potential future games
                api.appendToSidebar("Game ended after teleportation.");
            }
        } else {
            api.appendToSidebar("Teleport has already been used!");
        }
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
