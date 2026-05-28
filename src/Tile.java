// Kedaar and Jason
// this class represents one square in the ecosystem grid
public class Tile {
    // terrain is the base type of the tile, such as grass, water, or rock
    private TileType terrain;

    // a tile can hold one plant and one animal reference
    private Plant plant;
    private Animal animal;

    // corpse calories are leftover food after an animal dies
    private double corpseCalories;

    public Tile(TileType terrain) {
        setTerrain(terrain);
        this.plant = null;
        this.animal = null;
        this.corpseCalories = 0;
    }

    public TileType getTerrain() {
        return terrain;
    }

    public void setTerrain(TileType terrain) {
        // terrain should never be null because every tile needs a base type
        if (terrain == null) {
            throw new IllegalArgumentException("Terrain cannot be null");
        }

        this.terrain = terrain;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public void clearPlant() {
        plant = null;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public void clearAnimal() {
        animal = null;
    }

    public double getCorpseCalories() {
        return corpseCalories;
    }

    public void addCorpseCalories(double calories) {
        // ignore invalid corpse food values
        if (calories <= 0) {
            return;
        }

        corpseCalories += calories;
    }

    public void clearCorpseCalories() {
        corpseCalories = 0;
    }

    public double eatCorpseCalories(double requestedCalories) {
        // no food can be eaten if the request is invalid or there is no corpse food
        if (requestedCalories <= 0 || corpseCalories <= 0) {
            return 0;
        }

        // only eat as much as exists on the tile
        double eaten = Math.min(requestedCalories, corpseCalories);
        corpseCalories -= eaten;

        return eaten;
    }

    public boolean hasLivingPlant() {
        return plant != null && plant.isAlive();
    }

    public boolean hasLivingAnimal() {
        return animal != null && animal.isAlive();
    }

    public boolean hasCorpse() {
        return corpseCalories > 0;
    }

    public boolean isGrass() {
        return terrain == TileType.GRASS;
    }

    public boolean isWater() {
        return terrain == TileType.WATER;
    }

    public boolean isRock() {
        return terrain == TileType.ROCK;
    }

    public boolean isOpenForPlant() {
        // plants can only grow on empty grass tiles
        return isGrass() && plant == null && animal == null;
    }

    public boolean isOpenForAnimal(Animal animalTryingToEnter) {
        // animals can only move onto grass tiles
        if (!isGrass()) {
            return false;
        }

        // the tile is open if it has no animal or if the same animal is already there
        return animal == null || animal == animalTryingToEnter;
    }
}
