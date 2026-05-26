// this class stores the actual 2d grid of tile objects
public class WorldGrid {
    private final int rows;
    private final int cols;
    private final Tile[][] tiles;

    public WorldGrid(int rows, int cols) {

        this.rows = rows;
        this.cols = cols;
        this.tiles = new Tile[rows][cols];

        // start the map as a full grass world before anything else is added
        createBlankMap();
    }

    private void createBlankMap() {
        // fill every row and column with a grass tile
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                tiles[row][col] = new Tile(TileType.GRASS);
            }
        }
    }

    public boolean isValidTile(int row, int col) {
        // a coordinate is valid only if it is inside the grid boundaries
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public Tile getTile(int row, int col) {
        // return null for invalid tiles so callers can safely check before using it
        if (!isValidTile(row, col)) {
            return null;
        }

        return tiles[row][col];
    }

    public TileType getTileType(int row, int col) {
        Tile tile = getTile(row, col);

        if (tile == null) {
            return null;
        }

        return tile.getTerrain();
    }

    public boolean hasLivingPlant(int row, int col) {
        Tile tile = getTile(row, col);
        return tile != null && tile.hasLivingPlant();
    }

    public boolean hasWaterNear(int centerRow, int centerCol, int range) {
        // check the square area around a tile to see if any nearby tile is water
        for (int row = centerRow - range; row <= centerRow + range; row++) {
            for (int col = centerCol - range; col <= centerCol + range; col++) {
                Tile tile = getTile(row, col);

                if (tile != null && tile.isWater()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isOpenForPlant(int row, int col) {
        Tile tile = getTile(row, col);
        return tile != null && tile.isOpenForPlant();
    }

    public boolean canAnimalEnter(Animal animal, int row, int col) {
        Tile tile = getTile(row, col);
        return tile != null && tile.isOpenForAnimal(animal);
    }

    public double getMovementCost(int row, int col) {
        Tile tile = getTile(row, col);

        // invalid tiles have a huge movement cost so animals avoid them
        if (tile == null) {
            return Double.MAX_VALUE;
        }

        // different terrain types can make movement easier or harder
        switch (tile.getTerrain()) {
            case GRASS:
                return 1.0;
            case WATER:
                return 3.0;
            case ROCK:
            default:
                return 5.0;
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
}
