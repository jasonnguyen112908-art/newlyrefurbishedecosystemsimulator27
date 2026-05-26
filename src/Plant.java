import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

// this class represents a living plant in the ecosystem
// plants grow, store water, photosynthesize, reproduce, mutate, and can be eaten by herbivores
public class Plant {
    private static int nextId = 1;

    // identity, family, position, and gene data shown in the inspector
    private final int id;
    private final int generation;
    private final EnumMap<PlantGene, Double> genes;
    private EnumMap<PlantGene, Double> founderGeneBaseline;
    private int parentId = -1;
    private int row;
    private int col;
    private String speciesName;

    // life state and resource data that change every tick
    private boolean alive = true;
    private int ageTicks;
    private int reproductionCooldown;
    private double health;
    private double calories;
    private double storedWater;
    private double height;

    // mutation data displayed through the inspector
    private int variationCount;
    private String variationSummary = "No recorded variation";

    public Plant(int row, int col, Map<PlantGene, Double> customGenes, String speciesName, int generation) {
        // set starting identity, position, generation, and genes
        this.id = nextId++;
        this.row = row;
        this.col = col;
        this.speciesName = speciesName;
        this.generation = generation;
        this.genes = PlantGeneDefaults.createDefaultGenes();
        if (customGenes != null) genes.putAll(customGenes);
        PlantGeneDefaults.cleanGenes(genes);
        this.founderGeneBaseline = PlantGeneDefaults.createDefaultGenes();

        // start the plant with health, stored energy, water, and early height
        this.health = maxHealth();
        this.calories = maxCalories() * seedReserveRatio();
        this.storedWater = maxWater() * (0.42 + getGene(PlantGene.SEED_SIZE) * 0.10);
        this.height = Math.min(getGene(PlantGene.MAX_HEIGHT) * 0.28, 0.22 + getGene(PlantGene.SEED_SIZE) * 0.18);
    }

    public void update(WorldModel world) {
        // this is one plant's life cycle for one simulation tick
        if (!alive) return;
        ageTicks++;
        if (reproductionCooldown > 0) reproductionCooldown--;

        absorbWater(world);
        photosynthesize(world);
        payMaintenance(world);

        if (health <= 0) { die(world); return; }
        if (shouldReproduce() && world.countPlantsNear(row, col, 2) < 4) spreadSeeds(world);
        else if (health < maxHealth() * 0.72 && caloriesRatio() > 0.28 && waterRatio() > 0.25) repair();
        else grow();

        if (health <= 0 || calories <= 0 && storedWater <= 0) die(world);
    }

    private void absorbWater(WorldModel world) {
        // plants gain stored water, especially when near water tiles
        double root = getGene(PlantGene.ROOT_DEPTH);
        double storage = getGene(PlantGene.WATER_STORAGE);
        double water = 2.0 + root * 2.1;
        if (nearWater(world, (int) Math.ceil(root + storage * 0.35))) water *= 1.55;
        storedWater = Math.min(maxWater(), storedWater + water);
    }

    private boolean nearWater(WorldModel world, int range) {
        // check nearby tiles for water
        for (int r = row - range; r <= row + range; r++) {
            for (int c = col - range; c <= col + range; c++) {
                if (world.isValidTile(r, c) && world.getTileType(r, c) == TileType.WATER) return true;
            }
        }
        return false;
    }

    private void photosynthesize(WorldModel world) {
        // convert sunlight and plant traits into calories, while spending water
        if (storedWater <= 0) {
            health -= 3.0;
            return;
        }

        int neighbors = world.countPlantsNear(row, col, 1);
        double shadeTolerance = getGene(PlantGene.SHADE_TOLERANCE);
        double crowdPenalty = neighbors * Math.max(0.025, 0.115 - shadeTolerance * 0.040);
        double shadeCost = shadeTolerance * 0.08;
        double light = clamp(world.getSunValue() * (0.48 + heightRatio() * 0.72) - crowdPenalty - shadeCost, 0.0, 1.55);

        double leaf = getGene(PlantGene.LEAF_SIZE);
        double caloriesMade = light * leaf * (7.4 + getGene(PlantGene.GROWTH_RATE) * 2.5);
        double waterCost = leaf * (1.05 + getGene(PlantGene.GROWTH_RATE) * 0.42) / (0.82 + getGene(PlantGene.WATER_STORAGE) * 0.25);

        calories = Math.min(maxCalories(), calories + caloriesMade);
        storedWater = Math.max(0, storedWater - waterCost);
    }

    private void payMaintenance(WorldModel world) {
        // plants spend calories to maintain height, leaves, roots, and defenses
        double cost = 1.7
                + height * 0.42
                + getGene(PlantGene.LEAF_SIZE) * 0.82
                + getGene(PlantGene.ROOT_DEPTH) * 0.42
                + getGene(PlantGene.WOODINESS) * 0.62
                + getGene(PlantGene.TOXICITY) * 0.68
                + getGene(PlantGene.THORNS) * 0.62
                + getGene(PlantGene.WATER_STORAGE) * 0.30
                + getGene(PlantGene.SEED_SIZE) * 0.13;
        calories = Math.max(0, calories - cost);

        if (calories == 0) health -= 3.5;
        if (storedWater == 0) health -= 4.2;
        int closeNeighbors = world.countPlantsNear(row, col, 1);
        int widerNeighbors = world.countPlantsNear(row, col, 2);
        if (closeNeighbors > 3) health -= Math.max(0.15, 1.15 - getGene(PlantGene.SHADE_TOLERANCE) * 0.35);
        if (widerNeighbors > 9) health -= Math.max(0.10, 0.55 - getGene(PlantGene.SHADE_TOLERANCE) * 0.22);
        if (ageTicks > maxAge()) health -= 4.0;
    }

    private void grow() {
        // grow taller when the plant has enough calories and water
        if (height >= getGene(PlantGene.MAX_HEIGHT)) return;

        double wood = getGene(PlantGene.WOODINESS);
        double growth = (getGene(PlantGene.GROWTH_RATE) * 0.36) / (0.82 + wood * 0.35);
        double calorieCost = 4.2 + growth * 3.8 + getGene(PlantGene.LEAF_SIZE) * 1.0 + wood * 1.55 + getGene(PlantGene.THORNS) * 0.42;
        double waterCost = 2.3 + growth * 1.8 + getGene(PlantGene.LEAF_SIZE) * 0.68;

        if (calories >= calorieCost && storedWater >= waterCost) {
            calories -= calorieCost;
            storedWater -= waterCost;
            height = Math.min(getGene(PlantGene.MAX_HEIGHT), height + growth);
            health = Math.min(maxHealth(), health + wood * 0.25);
        }
    }

    private void repair() {
        // spend resources to recover health instead of growing
        calories = Math.max(0, calories - 3.0);
        storedWater = Math.max(0, storedWater - 1.3);
        health = Math.min(maxHealth(), health + 2.0 + getGene(PlantGene.ROOT_DEPTH) * 0.25 + getGene(PlantGene.WOODINESS) * 0.20);
    }

    public boolean canReproduce() {
        return shouldReproduce();
    }

    private boolean shouldReproduce() {
        // check maturity, cooldown, height, resources, and health before seed spreading
        return alive
                && reproductionCooldown <= 0
                && ageTicks >= Math.max(6, (int) Math.round(9.0 - getGene(PlantGene.FERTILITY) * 1.7 + getGene(PlantGene.SEED_SIZE) * 0.9))
                && heightRatio() > 0.42
                && caloriesRatio() > 0.72
                && waterRatio() > 0.50
                && healthRatio() > 0.58;
    }

    public void spreadSeeds(WorldModel world) {
        // create child plants nearby with mutated genes
        int seeds = 1;
        if (getGene(PlantGene.FERTILITY) > 1.18 && caloriesRatio() > 0.86 && Math.random() < 0.22) seeds++;

        for (int i = 0; i < seeds; i++) {
            int range = (int) Math.round(getGene(PlantGene.SEED_SPREAD));
            int[] target = world.findOpenPlantTileNear(row, col, range);
            if (target != null) {
                Plant child = new Plant(target[0], target[1], PlantMutation.createChildGenes(genes), speciesName, generation + 1);
                child.parentId = id;
                child.setFounderGeneBaseline(founderGeneBaseline);
                child.setVariationRecord(genes, "Seed mutation");
                world.addPlant(child);
            }
        }

        double seedSize = getGene(PlantGene.SEED_SIZE);
        calories = Math.max(0, calories - seeds * (28 + seedSize * 18 + getGene(PlantGene.SEED_SPREAD) * 2.7 + getGene(PlantGene.FERTILITY) * 5.0));
        storedWater = Math.max(0, storedWater - seeds * (9 + seedSize * 5.0));
        reproductionCooldown = Math.max(8, (int) Math.round(12.0 + seedSize * 2.4 + getGene(PlantGene.SEED_SPREAD) * 0.60 - getGene(PlantGene.FERTILITY) * 2.6));
    }

    public double beEaten(double requestedCalories, Animal eater, WorldModel world) {
        // remove calories/health when a herbivore eats this plant, and apply plant defenses
        if (!alive || requestedCalories <= 0) return 0;

        double woodyShield = clamp(1.0 - getGene(PlantGene.WOODINESS) * 0.10 - getGene(PlantGene.THORNS) * 0.11, 0.45, 1.0);
        double eaten = Math.min(requestedCalories * woodyShield, calories);
        calories -= eaten;
        health -= eaten * (0.18 + getGene(PlantGene.LEAF_SIZE) * 0.030);

        if (eater != null) {
            double digestion = eater.getGene("digestion");
            double toxinDamage = getGene(PlantGene.TOXICITY) * Math.max(0.25, 1.10 - digestion * 0.25);
            double thornDamage = getGene(PlantGene.THORNS) * Math.max(0.15, 0.70 - eater.getGene("armor") * 0.12);
            if (toxinDamage + thornDamage > 0) eater.takeDamage(toxinDamage + thornDamage, world);
        }

        if (calories <= 0 || health <= 0) alive = false;
        double nutritionPenalty = getGene(PlantGene.TOXICITY) * 0.08 + getGene(PlantGene.WOODINESS) * 0.035;
        return eaten * clamp(1.0 - nutritionPenalty, 0.55, 1.0);
    }

    void setVariationRecord(Map<PlantGene, Double> originalGenes, String label) {
        // store mutation information so the inspector can display it
        variationCount = PlantMutation.countDifferences(originalGenes, genes);
        variationSummary = label + ": " + PlantMutation.summarizeDifferences(originalGenes, genes);
    }

    private void die(WorldModel world) {
        // remove the plant from the world when it dies
        alive = false;
        if (world != null) world.removePlant(this);
    }

    public Map<String, String> getInspectionData() {
        // collect plant data into a map for inspectordialog
        Map<String, String> data = new LinkedHashMap<>();
        data.put("ID", String.valueOf(id));
        data.put("Species", speciesName);
        data.put("Generation", String.valueOf(generation));
        data.put("Parent ID", String.valueOf(parentId));
        data.put("Position", "(" + row + ", " + col + ")");
        data.put("Age", String.valueOf(ageTicks));
        data.put("Health", format(health) + " / " + format(maxHealth()));
        data.put("Calories", format(calories) + " / " + format(maxCalories()));
        data.put("Stored Water", format(storedWater) + " / " + format(maxWater()));
        data.put("Height", format(height) + " / " + format(getGene(PlantGene.MAX_HEIGHT)));
        data.put("Reproduction Ready", String.valueOf(canReproduce()));
        data.put("Variation Count", String.valueOf(variationCount));
        data.put("Variation Summary", variationSummary);
        data.put("Founder Drift", formatPercent(averageFounderDrift()) + " average absolute gene shift");
        for (PlantGene gene : PlantGene.values()) data.put("Gene: " + gene, formatGeneAgainstFounder(gene));
        return data;
    }

    void setFounderGeneBaseline(Map<PlantGene, Double> baseline) {
        // save the founder baseline so later mutations can be compared against it
        if (baseline == null || baseline.isEmpty()) {
            return;
        }
        this.founderGeneBaseline = new EnumMap<>(PlantGene.class);
        this.founderGeneBaseline.putAll(baseline);
    }

    private String formatGeneAgainstFounder(PlantGene gene) {
        // format one gene with its starter value and percent change
        double value = getGene(gene);
        Double founder = founderGeneBaseline.get(gene);
        if (founder == null || Math.abs(founder) < 0.000001) {
            return format(value) + " (starter n/a)";
        }
        double percent = (value - founder) / founder * 100.0;
        return format(value) + " (starter " + format(founder) + ", " + signedPercent(percent) + ")";
    }

    private double averageFounderDrift() {
        // calculate the average gene shift from the founder baseline
        double total = 0;
        int count = 0;
        for (PlantGene gene : PlantGene.values()) {
            Double founder = founderGeneBaseline.get(gene);
            if (founder == null || Math.abs(founder) < 0.000001) {
                continue;
            }
            total += Math.abs((getGene(gene) - founder) / founder);
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    private String formatPercent(double ratio) { return String.format("%.1f%%", ratio * 100.0); }
    private String signedPercent(double percent) { return String.format("%+.1f%%", percent); }

    // these helper methods calculate plant limits, ratios, formatting, and basic getters
    private double seedReserveRatio() { return clamp(0.30 + getGene(PlantGene.SEED_SIZE) * 0.12, 0.25, 0.72); }
    private int maxAge() { return (int) Math.round((18 + getGene(PlantGene.ROOT_DEPTH) * 3.0 + getGene(PlantGene.WOODINESS) * 4.5 - getGene(PlantGene.GROWTH_RATE) * 1.6 - getGene(PlantGene.FERTILITY) * 1.2) * SimulationState.TICKS_PER_YEAR); }
    private double maxHealth() { return 38 + getGene(PlantGene.ROOT_DEPTH) * 6 + getGene(PlantGene.WOODINESS) * 16 + getGene(PlantGene.THORNS) * 4 + getGene(PlantGene.TOXICITY) * 3; }
    private double maxCalories() { return 72 + getGene(PlantGene.LEAF_SIZE) * 42 + getGene(PlantGene.MAX_HEIGHT) * 10 + getGene(PlantGene.WOODINESS) * 12 + getGene(PlantGene.SEED_SIZE) * 8; }
    private double maxWater() { return 38 + getGene(PlantGene.ROOT_DEPTH) * 35 + getGene(PlantGene.WATER_STORAGE) * 42 + getGene(PlantGene.LEAF_SIZE) * 4; }
    private double caloriesRatio() { return safeRatio(calories, maxCalories()); }
    private double waterRatio() { return safeRatio(storedWater, maxWater()); }
    private double healthRatio() { return safeRatio(health, maxHealth()); }
    private double heightRatio() { return safeRatio(height, getGene(PlantGene.MAX_HEIGHT)); }
    private double safeRatio(double value, double max) { return max <= 0 ? 0 : clamp(value / max, 0, 1); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private String format(double value) { return String.format("%.3f", value); }

    public int getId() { return id; }
    public int getGeneration() { return generation; }
    public int getParentId() { return parentId; }
    public String getSpeciesName() { return speciesName; }
    public boolean isAlive() { return alive; }
    public int getAgeTicks() { return ageTicks; }
    public int getReproductionCooldown() { return reproductionCooldown; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public double getHealth() { return health; }
    public double getCalories() { return calories; }
    public double getStoredWater() { return storedWater; }
    public double getHeight() { return height; }
    public double getGene(PlantGene gene) { return genes.get(gene); }
    public Map<PlantGene, Double> getGenes() { return new EnumMap<>(genes); }
}
