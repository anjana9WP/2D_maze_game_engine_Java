package edu.curtin.assignment2.api;

import java.util.List;

public interface GameAPI {
    // Player movement callback
    void registerMoveCallback(Callback moveCallback);

    // Item acquisition callback
    void registerItemCallback(Callback itemCallback);

    // Menu/button selection callback
    void registerMenuCallback(String optionName, Callback menuCallback);

    // Query and modify game information
    int[] getPlayerLocation();
    void setPlayerLocation(int row, int col);
    List<String> getPlayerInventory();
    String getLastAcquiredItem();
    int[] getGridSize();
    String getGridSquareContents(int row, int col);
    boolean isGridSquareVisible(int row, int col);
    void setGridSquareVisible(int row, int col, boolean visible);

    // Add menu option
    void addMenuOption(String optionName);

    // Append message to sidebar
    void appendToSidebar(String message);
    
    // Add this new method
    void updateUI();
    void setMultipleGridSquaresVisible(List<int[]> positions);

    // Add this new method
    String getTranslation(String key);
}
