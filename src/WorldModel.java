import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// this class is the central model for the simulation
// it connects the grid, organisms, editor tools, time state, and gui notifications
public class WorldModel {
    // these directions are used when looking for open neighboring tiles
    private static final int[][] CARDINAL_DIRECTIONS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    private static final Random RANDOM = new Random();

    // these objects split the model into map data, organism lists, time, notifications, and editing tools
    private final WorldGrid grid;
    private final OrganismStore organisms;
    private final SimulationState simulationState;
    private final ChangeNotifier notifier;
    private final WorldEditor editor;

    public WorldModel(int rows, int cols) {
        // create the main parts of the simulation model
        this.grid = new WorldGrid(rows, cols);
        this.organisms = new OrganismStore();
        this.simulationState = new SimulationState();
        this.notifier = new ChangeNotifier();
        this.editor = new WorldEditor();
    }

    public void addChangeListener(Runnable listener) {
        // allow gui classes to register general update methods
        notifier.addChangeListener(listener);
    }

    public void addTileChangeListener(TileChangeListener listener) {
        // allow gui classes to register tile-specific update methods
        notifier.addTileChangeListener(listener);
    }

    public void updateWorld() {
        // one simulation tick updates time, plants, animals, migration, and the display
        notifier.beginBatch();

        try {
            simulationState.advanceTick();

            // update plants before animals so growth and seed spreading happen first
            for (Plant plant : organisms.getPlantsSnapshot()) {
                if (plant.isAlive()) {
                    plant.update(this);
                }
            }

            // update predators first so hunting pressure affects the ecosystem before herbivores move
            List<Animal> animals = organisms.getAnimalsSnapshot();
            for (Animal animal : animals) {
                if (animal.isAlive() && animal instanceof Predator) {
                    animal.update(this);
                }
            }

            // update herbivores and any other non-predator animals after predators
            for (Animal animal : animals) {
                if (animal.isAlive() && !(animal instanceof Predator)) {
                    animal.update(this);
                }
            }

            // migration keeps the ecosystem from becoming permanently unbalanced
            applyBoundaryMigration();
            notifier.markFullChange();
        } finally {
            // send all saved display changes after the tick finishes
            notifier.endBatch();
        }
    }

    public void useSelectedTool(int row, int col) {
        // pass user clicks from the world panel to the editor tool system
        editor.useSelectedTool(this, row, col);
    }

    public void setTile(int row, int col, TileType type) {
        // safely change terrain and notify the gui when a tile changes
        if (!grid.isValidTile(row, col) || type == null) {
            return;
        }

        Tile tile = grid.getTile(row, col);

        if (tile.getAnimal() != null && type != TileType.GRASS) {
            return;
        }

        if (tile.getTerrain() == type) {
            return;
        }

        tile.setTerrain(type);

        // non-grass terrain removes plants and corpse food from that tile
        if (type != TileType.GRASS) {
            Plant plant = tile.getPlant();

            if (plant != null) {
                organisms.removePlant(plant);
            }

            tile.clearPlant();
            tile.clearCorpseCalories();
        }

        notifier.markTileChanged(row, col);
    }

    public void addDefaultPlant(int row, int col) {
        // create a starter plant at a selected tile if the tile is open
        if (!isOpenForPlant(row, col)) {
            return;
        }

        addPlant(PlantFactory.createRandomFounderPlant(row, col));
    }

    public void addDefaultHerbivore(int row, int col) {
        // create a starter herbivore and place it in the model
        Herbivore herbivore = AnimalFactory.createRandomFounderHerbivore(row, col);
        addAnimal(herbivore);
    }

    public void addDefaultPredator(int row, int col) {
        // create a starter predator and place it in the model
        Predator predator = AnimalFactory.createRandomFounderPredator(row, col);
        addAnimal(predator);
    }

    public boolean addPlant(Plant plant) {
        // add a plant to both the grid and the organism list
        if (plant == null) {
            return false;
        }

        int row = plant.getRow();
        int col = plant.getCol();

        if (!isOpenForPlant(row, col)) {
            return false;
        }

        grid.getTile(row, col).setPlant(plant);
        organisms.addPlant(plant);
        notifier.markTileChanged(row, col);

        return true;
    }

    public void removePlant(Plant plant) {
        // remove a plant from the organism list and its tile
        if (plant == null) {
            return;
        }

        organisms.removePlant(plant);

        int row = plant.getRow();
        int col = plant.getCol();

        if (grid.isValidTile(row, col) && grid.getTile(row, col).getPlant() == plant) {
            grid.getTile(row, col).clearPlant();
            notifier.markTileChanged(row, col);
        }
    }

    public void removePlantAt(int row, int col) {
        // remove a plant from a specific tile, usually from the erase tool
        if (!grid.isValidTile(row, col)) {
            return;
        }

        Tile tile = grid.getTile(row, col);
        Plant plant = tile.getPlant();

        if (plant != null) {
            organisms.removePlant(plant);
            tile.clearPlant();
            notifier.markTileChanged(row, col);
        }
    }

    public double eatPlantAt(int row, int col, double requestedCalories, Animal eater) {
        // let an animal eat a plant and update the tile afterward
        if (!grid.isValidTile(row, col) || requestedCalories <= 0) {
            return 0;
        }

        Tile tile = grid.getTile(row, col);
        Plant plant = tile.getPlant();

        if (plant == null || !plant.isAlive()) {
            return 0;
        }

        double caloriesEaten = plant.beEaten(requestedCalories, eater, this);

        if (!plant.isAlive()) {
            tile.clearPlant();
            organisms.removePlant(plant);
        }

        notifier.markTileChanged(row, col);
        return caloriesEaten;
    }

    public boolean addAnimal(Animal animal) {
        // add an animal to both the grid and the organism list
        if (animal == null) {
            return false;
        }

        int row = animal.getRow();
        int col = animal.getCol();

        if (!canAnimalEnter(animal, row, col)) {
            return false;
        }

        organisms.addAnimal(animal);
        grid.getTile(row, col).setAnimal(animal);
        notifier.markTileChanged(row, col);

        return true;
    }

    public void removeAnimal(Animal animal) {
        // remove an animal from the organism list and its tile
        if (animal == null) {
            return;
        }

        organisms.removeAnimal(animal);

        int row = animal.getRow();
        int col = animal.getCol();

        if (grid.isValidTile(row, col) && grid.getTile(row, col).getAnimal() == animal) {
            grid.getTile(row, col).clearAnimal();
            notifier.markTileChanged(row, col);
        }
    }

    public void removeAnimalAt(int row, int col) {
        // remove an animal from a specific tile, usually from the erase tool
        if (!grid.isValidTile(row, col)) {
            return;
        }

        Tile tile = grid.getTile(row, col);
        Animal animal = tile.getAnimal();

        if (animal != null) {
            organisms.removeAnimal(animal);
            tile.clearAnimal();
            notifier.markTileChanged(row, col);
        }
    }

    public void updateAnimalPosition(Animal animal, int oldRow, int oldCol, int newRow, int newCol) {
        // move an animal visually and logically from one tile to another
        if (animal == null) {
            return;
        }

        if (grid.isValidTile(oldRow, oldCol) && grid.getTile(oldRow, oldCol).getAnimal() == animal) {
            grid.getTile(oldRow, oldCol).clearAnimal();
            notifier.markTileChanged(oldRow, oldCol);
        }

        if (grid.isValidTile(newRow, newCol)) {
            grid.getTile(newRow, newCol).setAnimal(animal);
            notifier.markTileChanged(newRow, newCol);
        }
    }

    public void addCorpse(int row, int col, double calories) {
        // add corpse calories after an animal dies
        if (!grid.isValidTile(row, col) || calories <= 0) {
            return;
        }

        grid.getTile(row, col).addCorpseCalories(calories);
        notifier.markTileChanged(row, col);
    }

    public double eatCorpseAt(int row, int col, double requestedCalories) {
        // let predators scavenge corpse calories from a tile
        if (!grid.isValidTile(row, col) || requestedCalories <= 0) {
            return 0;
        }

        double eaten = grid.getTile(row, col).eatCorpseCalories(requestedCalories);

        if (eaten > 0) {
            notifier.markTileChanged(row, col);
        }

        return eaten;
    }

    public int[] findNearestCorpse(int startRow, int startCol, double visionRange) {
        // search nearby tiles for the closest corpse food
        int maxDistance = (int) Math.ceil(Math.max(0, visionRange));
        int maximumDistanceSquared = maxDistance * maxDistance;
        int[] closest = null;
        int closestDistanceSquared = Integer.MAX_VALUE;

        for (int row = startRow - maxDistance; row <= startRow + maxDistance; row++) {
            for (int col = startCol - maxDistance; col <= startCol + maxDistance; col++) {
                int currentDistanceSquared = distanceSquared(startRow, startCol, row, col);
                Tile tile = grid.getTile(row, col);

                if (currentDistanceSquared <= maximumDistanceSquared
                        && tile != null
                        && tile.hasCorpse()
                        && currentDistanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = currentDistanceSquared;
                    closest = new int[] {row, col};
                }
            }
        }

        return closest;
    }

    public int countPlantsNear(int centerRow, int centerCol, int range) {
        // count nearby plants for crowding, growth, and reproduction decisions
        int count = 0;
        int maximumDistanceSquared = range * range;

        for (int row = centerRow - range; row <= centerRow + range; row++) {
            for (int col = centerCol - range; col <= centerCol + range; col++) {
                if (row == centerRow && col == centerCol) {
                    continue;
                }

                if (distanceSquared(centerRow, centerCol, row, col) <= maximumDistanceSquared
                        && grid.hasLivingPlant(row, col)) {
                    count++;
                }
            }
        }

        return count;
    }

    public int[] findOpenPlantTileNear(int centerRow, int centerCol, int range) {
        return findRandomPlantTile(centerRow, centerCol, range, false);
    }

    public int[] findOpenPlantTileNearWater(int centerRow, int centerCol, int range) {
        return findRandomPlantTile(centerRow, centerCol, range, true);
    }

    private int[] findRandomPlantTile(int centerRow, int centerCol, int range, boolean requireWaterNearby) {
        // collect possible nearby plant tiles, then choose one randomly
        List<int[]> possibleTiles = new ArrayList<>();
        int maximumDistanceSquared = range * range;

        for (int row = centerRow - range; row <= centerRow + range; row++) {
            for (int col = centerCol - range; col <= centerCol + range; col++) {
                if (distanceSquared(centerRow, centerCol, row, col) <= maximumDistanceSquared
                        && isOpenForPlant(row, col)
                        && (!requireWaterNearby || grid.hasWaterNear(row, col, 1))) {
                    possibleTiles.add(new int[] {row, col});
                }
            }
        }

        return chooseRandomTile(possibleTiles);
    }

    public int[] findNearestPlant(int startRow, int startCol, double visionRange) {
        // search nearby tiles for the closest living plant
        int maxDistance = (int) Math.ceil(Math.max(0, visionRange));
        int maximumDistanceSquared = maxDistance * maxDistance;
        int[] closest = null;
        int closestDistanceSquared = Integer.MAX_VALUE;

        for (int row = startRow - maxDistance; row <= startRow + maxDistance; row++) {
            for (int col = startCol - maxDistance; col <= startCol + maxDistance; col++) {
                int currentDistanceSquared = distanceSquared(startRow, startCol, row, col);

                if (currentDistanceSquared <= maximumDistanceSquared
                        && grid.hasLivingPlant(row, col)
                        && currentDistanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = currentDistanceSquared;
                    closest = new int[] {row, col};
                }
            }
        }

        return closest;
    }

    public int[] findNearestWater(int startRow, int startCol, double visionRange) {
        // search nearby tiles for the closest water tile
        int maxDistance = (int) Math.ceil(Math.max(0, visionRange));
        int maximumDistanceSquared = maxDistance * maxDistance;
        int[] closest = null;
        int closestDistanceSquared = Integer.MAX_VALUE;

        for (int row = startRow - maxDistance; row <= startRow + maxDistance; row++) {
            for (int col = startCol - maxDistance; col <= startCol + maxDistance; col++) {
                int currentDistanceSquared = distanceSquared(startRow, startCol, row, col);
                Tile tile = grid.getTile(row, col);

                if (currentDistanceSquared <= maximumDistanceSquared
                        && tile != null
                        && tile.isWater()
                        && currentDistanceSquared < closestDistanceSquared) {
                    closestDistanceSquared = currentDistanceSquared;
                    closest = new int[] {row, col};
                }
            }
        }

        return closest;
    }

    public int[] findOpenAdjacentTile(int row, int col) {
        // find an open neighboring tile for animal reproduction
        List<int[]> openTiles = new ArrayList<>();

        for (int[] direction : CARDINAL_DIRECTIONS) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];

            if (canAnimalEnter(null, newRow, newCol)) {
                openTiles.add(new int[] {newRow, newCol});
            }
        }

        return chooseRandomTile(openTiles);
    }

    public Animal findNearestMate(Animal searcher, double visionRange) {
        // delegate mate searching to the organism store
        if (searcher == null) {
            return null;
        }

        return organisms.findNearestMate(searcher, visionRange);
    }

    public Animal findNearestPrey(Animal hunter, double visionRange) {
        // delegate prey searching to the organism store
        if (hunter == null) {
            return null;
        }

        return organisms.findNearestPrey(hunter, visionRange);
    }


    private void applyBoundaryMigration() {
        // simulate animals entering from outside the map when the ecosystem is unbalanced
        int tick = simulationState.getTickCount();
        int herbivores = countLivingAnimalsByType("Herbivore");
        int predators = countLivingAnimalsByType("Predator");
        int plants = organisms.getPlantsSnapshot().size();

        if (predators == 0 && herbivores > 25) {
            int arrivals = 1;
            for (int i = 0; i < arrivals; i++) {
                int[] tile = randomAnimalTile();
                if (tile != null) addDefaultPredator(tile[0], tile[1]);
            }
        } else if (tick % 8 == 0 && herbivores > 50 && predators < 2) {
            int arrivals = 1;
            for (int i = 0; i < arrivals; i++) {
                int[] tile = randomAnimalTile();
                if (tile != null) addDefaultPredator(tile[0], tile[1]);
            }
        } else if (tick % 6 == 0 && herbivores > 160) {
            int desiredPredators = Math.min(8, Math.max(2, herbivores / 155));
            int arrivals = Math.min(1, desiredPredators - predators);
            for (int i = 0; i < arrivals; i++) {
                int[] tile = randomAnimalTile();
                if (tile != null) addDefaultPredator(tile[0], tile[1]);
            }
        }

        if (tick % 10 == 0 && herbivores < 25 && plants > 2500) {
            for (int i = 0; i < 3; i++) {
                int[] tile = randomAnimalTile();
                if (tile != null) addDefaultHerbivore(tile[0], tile[1]);
            }
        }
    }

    private int[] randomAnimalTile() {
        // find a random open tile where an animal can be placed
        for (int attempt = 0; attempt < 600; attempt++) {
            int row = RANDOM.nextInt(getRows());
            int col = RANDOM.nextInt(getCols());
            if (canAnimalEnter(null, row, col)) return new int[] {row, col};
        }
        return null;
    }

    private boolean isOpenForPlant(int row, int col) {
        return grid.isOpenForPlant(row, col);
    }

    public boolean canAnimalEnter(Animal animal, int row, int col) {
        return grid.canAnimalEnter(animal, row, col);
    }

    public double getMovementCost(int row, int col) {
        return grid.getMovementCost(row, col);
    }

    public String determineSpeciesName(Animal parentOne, Animal parentTwo, Map<String, Double> childGenes) {
        // decide whether a child keeps a parent species name, becomes a hybrid, or becomes a new species
        double distanceFromParentOne = geneDistance(parentOne.getGenes(), childGenes);
        double distanceFromParentTwo = geneDistance(parentTwo.getGenes(), childGenes);
        double averageDistance = (distanceFromParentOne + distanceFromParentTwo) / 2.0;

        if (averageDistance > 0.35) {
            return simulationState.createGeneratedSpeciesName();
        }

        if (parentOne.getSpeciesName().equals(parentTwo.getSpeciesName())) {
            return parentOne.getSpeciesName();
        }

        return parentOne.getSpeciesName() + "-" + parentTwo.getSpeciesName() + " Hybrid";
    }

    private double geneDistance(Map<String, Double> firstGenes, Map<String, Double> secondGenes) {
        // compare two gene maps by average percent difference
        double totalDifference = 0;
        int comparedGenes = 0;

        for (String geneName : firstGenes.keySet()) {
            if (secondGenes.containsKey(geneName)) {
                double firstValue = firstGenes.get(geneName);
                double secondValue = secondGenes.get(geneName);
                double average = (Math.abs(firstValue) + Math.abs(secondValue)) / 2.0;

                if (average > 0) {
                    totalDifference += Math.abs(firstValue - secondValue) / average;
                    comparedGenes++;
                }
            }
        }

        if (comparedGenes == 0) {
            return Double.MAX_VALUE;
        }

        return totalDifference / comparedGenes;
    }

    private int[] chooseRandomTile(List<int[]> possibleTiles) {
        // choose one random coordinate from a list of valid options
        if (possibleTiles.isEmpty()) {
            return null;
        }

        return possibleTiles.get(RANDOM.nextInt(possibleTiles.size()));
    }

    private int distanceSquared(int rowOne, int colOne, int rowTwo, int colTwo) {
        // compare distances without square root for faster range checks
        int rowDifference = rowOne - rowTwo;
        int colDifference = colOne - colTwo;

        return rowDifference * rowDifference + colDifference * colDifference;
    }

    // these methods expose model data to gui panels and organism behavior classes
    public boolean isValidTile(int row, int col) {
        return grid.isValidTile(row, col);
    }

    public Tile getTileObject(int row, int col) {
        return grid.getTile(row, col);
    }

    public TileType getTileType(int row, int col) {
        return grid.getTileType(row, col);
    }

    public Plant getPlant(int row, int col) {
        Tile tile = grid.getTile(row, col);

        if (tile == null) {
            return null;
        }

        return tile.getPlant();
    }

    public List<Plant> getPlantsSnapshot() {
        return organisms.getPlantsSnapshot();
    }

    public List<Animal> getAnimalsSnapshot() {
        return organisms.getAnimalsSnapshot();
    }

    public int countLivingAnimals() {
        return organisms.countLivingAnimals();
    }

    public int countLivingAnimalsByType(String animalType) {
        int count = 0;

        for (Animal animal : organisms.getAnimalsSnapshot()) {
            if (animal.isAlive() && animal.getAnimalType().equals(animalType)) {
                count++;
            }
        }

        return count;
    }

    public double getSunValue() {
        return simulationState.getSunValue();
    }

    public void setSunValue(double sunValue) {
        // changing sun affects plant growth, so the gui should refresh
        if (simulationState.setSunValue(sunValue)) {
            notifier.markFullChange();
        }
    }

    public ToolType getSelectedTool() {
        return editor.getSelectedTool();
    }

    public void setSelectedTool(ToolType selectedTool) {
        editor.setSelectedTool(selectedTool);
    }

    public int getTickCount() {
        return simulationState.getTickCount();
    }

    public double getYear() {
        return simulationState.getYear();
    }

    public String getSeasonName() {
        return simulationState.getSeasonName();
    }

    public int getRows() {
        return grid.getRows();
    }

    public int getCols() {
        return grid.getCols();
    }
}
