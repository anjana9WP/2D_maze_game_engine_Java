package edu.curtin.gameplugins;

import edu.curtin.assignment2.api.GameAPI;
import edu.curtin.assignment2.api.Callback;
import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;

public class Reveal implements GameAPI {
    private static final Logger LOGGER = Logger.getLogger(Reveal.class.getName());
    private GameAPI api;
    private boolean revealed = false;

    public Reveal(GameAPI api) {
        this.api = api;
        api.registerItemCallback(this::checkForMap);
        LOGGER.info("Reveal plugin initialized and item callback registered");
    }

    private void checkForMap() {
        if (!revealed) {
            String lastAcquiredItem = api.getLastAcquiredItem();
            LOGGER.info("Checking for map. Last acquired item: " + lastAcquiredItem);
            if (lastAcquiredItem != null && lastAcquiredItem.toLowerCase().contains("map")) {
                LOGGER.info("Map found. Revealing goal and items.");
                revealGoalAndItems();
                revealed = true;
                api.appendToSidebar("Map revealed the goal and remaining items!");
                api.updateUI(); // Ensure the UI is updated after revealing
                LOGGER.info("Reveal process completed.");
            }
        }
    }
    
    private void revealGoalAndItems() {
        int[] gridSize = api.getGridSize();
        List<int[]> positionsToReveal = new ArrayList<>();

        for (int row = 0; row < gridSize[0]; row++) {
            for (int col = 0; col < gridSize[1]; col++) {
                String contents = api.getGridSquareContents(row, col);
                if (contents != null && (contents.equals("Goal") || contents.startsWith("Item:"))) {
                    positionsToReveal.add(new int[]{row, col});
                    LOGGER.info("Revealed " + contents + " at (" + row + "," + col + ")");
                }
            }
        }

        api.setMultipleGridSquaresVisible(positionsToReveal);
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
