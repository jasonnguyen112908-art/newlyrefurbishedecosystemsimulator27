// Kedaar's class
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

// this abstract class holds the shared data and behavior for all animals
// herbivores and predators both inherit movement, biology, reproduction, and inspection logic from here
public abstract class Animal {
    protected static final Random RANDOM = new Random();
    private static final int[][] DIRECTIONS = {{-1,0},{1,0},{0,-1},{0,1}};
    private static int nextId = 1;

    public enum Sex { MALE, FEMALE }

    // identity, family, and position data used by the simulation and inspector
    protected final int id;
    protected final int generation;
    protected int parentOneId = -1;
    protected int parentTwoId = -1;
    protected int row;
    protected int col;
    protected String speciesName;
    protected Sex sex;

    // life state and resource data that change each tick
    protected boolean alive = true;
    protected int ageTicks;
    protected int reproductionCooldown;
    protected double calories;
    protected double hydration;
    protected double energy;
    protected double health;
    protected double rememberedDanger;
    protected double frustration;

    // gene variation data shown in the inspector
    protected int variationCount;
    protected String variationSummary = "No recorded variation";
    protected final Genome genome;
    protected final Map<String, Double> genes;
    protected Map<String, Double> founderGeneBaseline;

    public Animal(int row, int col, Map<String, Double> customGenes, String speciesName, int generation) {
        // set starting identity, location, generation, and inherited genes
        this.id = nextId++;
        this.row = row;
        this.col = col;
        this.speciesName = speciesName;
        this.generation = generation;
        this.sex = RANDOM.nextBoolean() ? Sex.MALE : Sex.FEMALE;
        this.genome = Genome.fromAnimalGenes(customGenes);
        this.genes = genome.backingMap();
        this.founderGeneBaseline = Genome.createDefaultAnimalGenome().toMap();

        // start animals with partial but healthy resource levels
        this.health = maxHealth();
        this.calories = maxCalories() * 0.78;
        this.hydration = maxHydration() * 0.82;
        this.energy = maxEnergy() * 0.78;
    }

    public void update(WorldModel world) {
        // this is one animal's life cycle for one simulation tick
        if (!alive) {
            return;
        }
        ageTicks++;
        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }
        applyPassiveBiology(world);
        if (alive) {
            decideAction(world);
        }
        if (!alive) {
            die(world);
        }
    }

    // subclasses must define what kind of action they take and what type of animal they are
    protected abstract void decideAction(WorldModel world);
    public abstract boolean canEatAnimal(Animal other);
    protected abstract Animal createChild(int row, int col, Map<String, Double> genes, String species, int generation);
    public abstract String getAnimalType();

    protected void applyPassiveBiology(WorldModel world) {
        // passive biology handles hunger, thirst, aging, healing, and death risk
        calories = Math.max(0, calories - AnimalGeneEffects.calorieBurn(this));
        hydration = Math.max(0, hydration - AnimalGeneEffects.hydrationLoss(this));
        if (calories == 0) health -= this instanceof Predator ? 1.15 + getGene("metabolism") * 0.35 : 3.5 + getGene("metabolism") * 0.8;
        if (hydration == 0) health -= this instanceof Predator ? 2.10 + getGene("size") * 0.40 : 5.0 + getGene("size") * 0.9;
        if (ageTicks > AnimalGeneEffects.maxAge(this)) health -= this instanceof Predator ? 2.4 : 5.5;
        if (caloriesRatio() > 0.28 && hydrationRatio() > 0.28) {
            energy = Math.min(maxEnergy(), energy + AnimalGeneEffects.energyRecovery(this));
        }
        if (caloriesRatio() > 0.35 && hydrationRatio() > 0.35 && energyRatio() > 0.40) {
            health = Math.min(maxHealth(), health + AnimalGeneEffects.healing(this));
        }
        rememberedDanger = clamp(rememberedDanger * 0.985, 0, 1);
        frustration = clamp(frustration * 0.98, 0, 1);
        alive = health > 0;
    }

    public boolean moveTo(int newRow, int newCol, WorldModel world) {
        // move to a new tile only if the tile is open and the animal can pay the cost
        if (!alive || world == null || !world.canAnimalEnter(this, newRow, newCol)) {
            return false;
        }
        double energyCost = AnimalGeneEffects.moveEnergyCost(this, world, newRow, newCol);
        double calorieCost = AnimalGeneEffects.moveCalorieCost(this);
        if (energy < energyCost || calories < calorieCost) {
            return false;
        }
        int oldRow = row;
        int oldCol = col;
        energy -= energyCost;
        calories -= calorieCost;
        row = newRow;
        col = newCol;
        world.updateAnimalPosition(this, oldRow, oldCol, row, col);
        return true;
    }

    public boolean moveToward(int targetRow, int targetCol, WorldModel world) {
        // move one step closer to a target position
        int dr = Integer.compare(targetRow, row);
        int dc = Integer.compare(targetCol, col);
        if (Math.abs(targetRow - row) >= Math.abs(targetCol - col)) {
            return (dr != 0 && moveTo(row + dr, col, world)) || (dc != 0 && moveTo(row, col + dc, world));
        }
        return (dc != 0 && moveTo(row, col + dc, world)) || (dr != 0 && moveTo(row + dr, col, world));
    }

    public boolean moveRandomly(WorldModel world) {
        // try the four cardinal directions in a random order
        int start = RANDOM.nextInt(DIRECTIONS.length);
        for (int i = 0; i < DIRECTIONS.length; i++) {
            int[] d = DIRECTIONS[(start + i) % DIRECTIONS.length];
            if (moveTo(row + d[0], col + d[1], world)) {
                return true;
            }
        }
        return false;
    }

    protected boolean moveTowardWithSpeed(int targetRow, int targetCol, WorldModel world) {
        // faster animals can move multiple steps toward a target in one action
        boolean moved = false;
        for (int i = 0; i < AnimalGeneEffects.movementSteps(this, false); i++) {
            if (!moveToward(targetRow, targetCol, world)) break;
            moved = true;
        }
        return moved;
    }

    protected boolean moveAwayWithSpeed(int targetRow, int targetCol, WorldModel world) {
        // fleeing animals try to move away from a dangerous target
        boolean moved = false;
        for (int i = 0; i < AnimalGeneEffects.movementSteps(this, true); i++) {
            int dr = Integer.compare(row, targetRow);
            int dc = Integer.compare(col, targetCol);
            if (!((Math.abs(dr) >= Math.abs(dc) && dr != 0 && moveTo(row + dr, col, world))
                    || (dc != 0 && moveTo(row, col + dc, world))
                    || (dr != 0 && moveTo(row + dr, col, world))
                    || moveRandomly(world))) {
                break;
            }
            moved = true;
        }
        return moved;
    }

    public void drink(double amount) {
        // add hydration without going above the maximum
        hydration = Math.min(maxHydration(), hydration + Math.max(0, amount));
    }

    public void gainCalories(double amount) {
        // add calories without going above the maximum
        calories = Math.min(maxCalories(), calories + Math.max(0, amount));
    }

    protected void rest() {
        // resting restores energy and can heal animals with enough resources
        energy = Math.min(maxEnergy(), energy + AnimalGeneEffects.energyRecovery(this) * 1.7);
        if (caloriesRatio() > 0.30 && hydrationRatio() > 0.30) {
            health = Math.min(maxHealth(), health + AnimalGeneEffects.healing(this) * 1.5);
        }
    }

    protected boolean payActionCost(double energyCost, double calorieCost) {
        // actions like attacking require enough energy and calories
        if (energy < energyCost || calories < calorieCost) {
            return false;
        }
        energy -= energyCost;
        calories -= calorieCost;
        return true;
    }

    public void takeDamage(double damage, WorldModel world) {
        // damage lowers health and can increase remembered danger
        if (!alive || damage <= 0) return;
        double actual = AnimalGeneEffects.defendedDamage(this, damage);
        health -= actual;
        rememberedDanger = clamp(rememberedDanger + actual / Math.max(1, maxHealth()), 0, 1);
        if (health <= 0) die(world);
    }

    protected void die(WorldModel world) {
        // remove the animal and leave corpse calories behind
        if (!alive && world == null) return;
        alive = false;
        if (world != null) {
            world.removeAnimal(this);
            world.addCorpse(row, col, AnimalGeneEffects.corpseCalories(this));
        }
    }

    public boolean isReproductivelyReady() {
        // check age, cooldown, resources, and health before reproduction
        return alive
                && ageTicks >= AnimalGeneEffects.reproductionAge(this)
                && reproductionCooldown <= 0
                && caloriesRatio() > 0.56 - getGene("fertility") * 0.04
                && hydrationRatio() > 0.42
                && energyRatio() > 0.46 - getGene("fertility") * 0.035
                && healthRatio() > 0.50;
    }

    public boolean canReproduceWith(Animal mate) {
        // both animals must be ready and genetically compatible
        return isReproductivelyReady() && mate != null && mate.isReproductivelyReady() && AnimalGeneEffects.canMate(this, mate);
    }

    public Animal reproduceWith(Animal mate, WorldModel world) {
        // create one or more children near the parent by mixing parent genes
        if (world == null || !canReproduceWith(mate)) return null;

        int plannedLitter = chooseLitterSize(mate);
        Animal firstChild = null;
        int born = 0;

        for (int i = 0; i < plannedLitter; i++) {
            int[] tile = world.findOpenAdjacentTile(row, col);
            if (tile == null) break;

            Map<String, Double> childGenes = Genome.createMixedMutatedGenome(genome, mate.genome, RANDOM).toMap();
            Animal child = createChild(tile[0], tile[1], childGenes, world.determineSpeciesName(this, mate, childGenes), Math.max(generation, mate.generation) + 1);
            child.parentOneId = id;
            child.parentTwoId = mate.id;
            child.setFounderGeneBaseline(getFounderGeneBaseline());
            child.applyParentalProvision(AnimalGeneEffects.childProvisionRatio(this, mate, plannedLitter));

            if (world.addAnimal(child)) {
                born++;
                if (firstChild == null) firstChild = child;
            }
        }

        if (born > 0) {
            payReproductionCost(born);
            mate.payReproductionCost(born);
        }

        return firstChild;
    }

    private int chooseLitterSize(Animal mate) {
        // litter size is based on both parents and capped for predators
        double expected = (getGene("litterSize") + mate.getGene("litterSize")) / 2.0;
        int babies = 1;
        double secondChance = clamp(expected - 1.0, 0, 1);
        double thirdChance = clamp(expected - 2.0, 0, 1) * 0.55;
        if (RANDOM.nextDouble() < secondChance) babies++;
        if (RANDOM.nextDouble() < thirdChance) babies++;
        if (this instanceof Predator) babies = Math.min(babies, 2);
        return Math.max(1, babies);
    }

    protected void applyParentalProvision(double reserveRatio) {
        // newborns start with resources based on parental care
        calories = maxCalories() * clamp(reserveRatio, 0.15, 0.90);
        hydration = maxHydration() * clamp(reserveRatio + 0.08, 0.20, 0.95);
        energy = maxEnergy() * clamp(reserveRatio + 0.03, 0.15, 0.90);
        health = maxHealth() * clamp(0.72 + reserveRatio * 0.25, 0.55, 1.0);
    }

    private void payReproductionCost(int childrenBorn) {
        // reproduction uses parent calories and energy, then starts a cooldown
        double litterMultiplier = 0.75 + childrenBorn * 0.25;
        calories = Math.max(0, calories - AnimalGeneEffects.reproductionCostCalories(this) * litterMultiplier);
        energy = Math.max(0, energy - AnimalGeneEffects.reproductionCostEnergy(this) * litterMultiplier);
        reproductionCooldown = AnimalGeneEffects.reproductionCooldown(this) + Math.max(0, childrenBorn - 1);
    }

    void setVariationRecord(Map<String, Double> originalGenes, String label) {
        // store mutation information so the inspector can display it
        variationCount = AnimalMutation.countDifferences(originalGenes, genes);
        variationSummary = label + ": " + AnimalMutation.summarizeDifferences(originalGenes, genes);
    }

    public Map<String, String> getInspectionData() {
        // collect animal data into a map for inspectordialog
        Map<String, String> data = new LinkedHashMap<>();
        data.put("ID", String.valueOf(id));
        data.put("Animal Type", getAnimalType());
        data.put("Species", speciesName);
        data.put("Sex", sex.toString());
        data.put("Generation", String.valueOf(generation));
        data.put("Parents", parentOneId + ", " + parentTwoId);
        data.put("Position", "(" + row + ", " + col + ")");
        data.put("Age", String.valueOf(ageTicks));
        data.put("Health", format(health) + " / " + format(maxHealth()));
        data.put("Calories", format(calories) + " / " + format(maxCalories()));
        data.put("Hydration", format(hydration) + " / " + format(maxHydration()));
        data.put("Energy", format(energy) + " / " + format(maxEnergy()));
        data.put("Detection Range", format(AnimalGeneEffects.detectionRange(this, "food")));
        data.put("Movement Steps", String.valueOf(AnimalGeneEffects.movementSteps(this, false)));
        data.put("Variation Count", String.valueOf(variationCount));
        data.put("Variation Summary", variationSummary);
        data.put("Founder Drift", formatPercent(averageFounderDrift()) + " average absolute gene shift");
        for (String gene : genes.keySet()) {
            data.put("Gene: " + gene, formatGeneAgainstFounder(gene));
        }
        return data;
    }

    void setFounderGeneBaseline(Map<String, Double> baseline) {
        // save the founder baseline so later mutations can be compared against it
        if (baseline == null || baseline.isEmpty()) {
            return;
        }
        this.founderGeneBaseline = new LinkedHashMap<>(baseline);
    }

    Map<String, Double> getFounderGeneBaseline() {
        return new LinkedHashMap<>(founderGeneBaseline);
    }

    private String formatGeneAgainstFounder(String gene) {
        // format one gene with its starter value and percent change
        double value = genes.get(gene);
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
        for (String gene : genes.keySet()) {
            Double founder = founderGeneBaseline.get(gene);
            if (founder == null || Math.abs(founder) < 0.000001) {
                continue;
            }
            total += Math.abs((genes.get(gene) - founder) / founder);
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    protected String formatPercent(double ratio) { return String.format("%.1f%%", ratio * 100.0); }
    protected String signedPercent(double percent) { return String.format("%+.1f%%", percent); }

    // these helper methods support movement, genetics, ratios, formatting, and getters
    public double distanceTo(Animal other) { return other == null ? Double.MAX_VALUE : distanceTo(other.row, other.col); }
    public double distanceTo(int r, int c) { int dr = row - r; int dc = col - c; return Math.sqrt(dr * dr + dc * dc); }
    public double geneticDistanceTo(Animal other) { return other == null ? Double.MAX_VALUE : genome.distanceTo(other.genome); }
    protected double caloriesRatio() { return safeRatio(calories, maxCalories()); }
    protected double hydrationRatio() { return safeRatio(hydration, maxHydration()); }
    protected double energyRatio() { return safeRatio(energy, maxEnergy()); }
    protected double healthRatio() { return safeRatio(health, maxHealth()); }
    protected double maxHealth() { return AnimalGeneEffects.maxHealth(this); }
    protected double maxCalories() { return AnimalGeneEffects.maxCalories(this); }
    protected double maxHydration() { return AnimalGeneEffects.maxHydration(this); }
    protected double maxEnergy() { return AnimalGeneEffects.maxEnergy(this); }
    protected double safeRatio(double value, double max) { return max <= 0 ? 0 : clamp(value / max, 0, 1); }
    protected double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    protected String format(double value) { return String.format("%.3f", value); }

    public int getId() { return id; }
    public int getGeneration() { return generation; }
    public int getParentOneId() { return parentOneId; }
    public int getParentTwoId() { return parentTwoId; }
    public String getSpeciesName() { return speciesName; }
    public Sex getSex() { return sex; }
    public boolean isAlive() { return alive; }
    public int getAgeTicks() { return ageTicks; }
    public int getReproductionCooldown() { return reproductionCooldown; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public double getCalories() { return calories; }
    public double getHydration() { return hydration; }
    public double getEnergy() { return energy; }
    public double getHealth() { return health; }
    public double getGene(String geneName) { return genome.get(geneName); }
    public Map<String, Double> getGenes() { return genome.toMap(); }
}
