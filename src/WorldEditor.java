// Kedaar and Jason
// this class connects the selected toolbar tool to actions on the world model
public class WorldEditor {
    // this stores what tool the user currently selected
    private ToolType selectedTool;

    public WorldEditor() {
        this.selectedTool = ToolType.GRASS;
    }

    public void useSelectedTool(WorldModel model, int row, int col) {
        // the switch statement decides what happens when the user clicks a tile
        switch (selectedTool) {
            case GRASS:
                model.setTile(row, col, TileType.GRASS);
                break;
            case WATER:
                model.setTile(row, col, TileType.WATER);
                break;
            case ROCK:
                model.setTile(row, col, TileType.ROCK);
                break;
            case PLANT:
                model.addDefaultPlant(row, col);
                break;
            case HERBIVORE:
                model.addDefaultHerbivore(row, col);
                break;
            case PREDATOR:
                model.addDefaultPredator(row, col);
                break;
            case ERASE_PLANT:
                model.removePlantAt(row, col);
                break;
            case ERASE_ANIMAL:
                model.removeAnimalAt(row, col);
                break;
            default:
                break;
        }
    }

    public ToolType getSelectedTool() {
        return selectedTool;
    }

    public void setSelectedTool(ToolType selectedTool) {
        // ignore null so the editor always has a usable tool
        if (selectedTool != null) {
            this.selectedTool = selectedTool;
        }
    }
}
