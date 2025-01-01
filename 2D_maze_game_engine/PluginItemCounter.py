script !{
class PluginItemCounter:
    def __init__(self, api):
        self.api = api
        self.items_acquired = 0
        self.obstacles_traversed = 0
        self.special_item_added = False
        self.api.registerMoveCallback(self.on_move)
        self.api.registerItemCallback(self.on_item_acquired)
    def on_move(self):
        player_loc = self.api.getPlayerLocation()
        content = self.api.getGridSquareContents(player_loc[0], player_loc[1])
        if content == "Obstacle":
            self.obstacles_traversed += 1
            self.check_and_add_special_item()
    def on_item_acquired(self):
        self.items_acquired += 1
        self.check_and_add_special_item()
    def check_and_add_special_item(self):
        if not self.special_item_added and (self.items_acquired + self.obstacles_traversed) >= 5:
            inventory = self.api.getPlayerInventory()
            inventory.append("PLUGIN ITEM")
            self.api.appendToSidebar("Special PLUGIN ITEM added to inventory!")
            self.special_item_added = True
            # Trigger an update of the game state
            current_location = self.api.getPlayerLocation()
            self.api.setPlayerLocation(current_location[0], current_location[1])
            # Update the UI
            self.api.updateUI()
plugin_item_counter = PluginItemCounter(api)
}