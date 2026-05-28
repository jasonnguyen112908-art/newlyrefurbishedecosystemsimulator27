// mainly created by Kedaar and Jason
import java.util.LinkedHashMap;
import java.util.Map;

// this class defines predator behavior
// predators seek water, reproduce, scavenge corpses, hunt herbivores, and patrol the world
public class Predator extends Animal {
    // these fields track hunting history for the inspector
    private Animal lastPrey;
    private int successfulAttacks;
    private int failedAttacks;

    public Predator(int row, int col, Map<String, Double> customGenes, String speciesName, int generation) {
        super(row, col, customGenes, speciesName, generation);
    }

    @Override
    protected void decideAction(WorldModel world) {
        // choose actions by priority: water, rest, reproduction, scavenging, hunting, then patrolling
        if (hydrationRatio() < 0.40 && seekWater(world)) return;
        if (energyRatio() < 0.22 || healthRatio() < 0.38) { rest(); return; }
        if (tryReproduce(world)) return;
        if (caloriesRatio() < 0.50 && scavenge(world)) return;
        if (caloriesRatio() < 0.54 || RANDOM.nextDouble() < getGene("aggression") * 0.07) {
            if (hunt(world)) return;
        }
        if (RANDOM.nextDouble() < 0.45 + getGene("aggression") * 0.25) moveRandomly(world); else rest();
    }

    @Override
    public boolean isReproductivelyReady() {
        // predators use lower resource thresholds so they can recover population more easily
        return alive
                && ageTicks >= AnimalGeneEffects.reproductionAge(this)
                && reproductionCooldown <= 0
                && caloriesRatio() > 0.22
                && hydrationRatio() > 0.22
                && energyRatio() > 0.22
                && healthRatio() > 0.34;
    }

    @Override
    public boolean canEatAnimal(Animal other) {
        // predators can only eat herbivores
        return other instanceof Herbivore;
    }

    @Override
    protected Animal createChild(int row, int col, Map<String, Double> childGenes, String childSpeciesName, int childGeneration) {
        // create a predator child with inherited genes
        Predator child = new Predator(row, col, childGenes, childSpeciesName, childGeneration);
        child.setVariationRecord(getGenes(), "Inherited predator genes");
        return child;
    }

    @Override
    public String getAnimalType() {
        return "Predator";
    }

    @Override
    public Map<String, String> getInspectionData() {
        // add predator-specific hunting data to the general animal inspector data
        Map<String, String> data = new LinkedHashMap<>(super.getInspectionData());
        data.put("State", lastPrey == null ? "Patrolling" : "Tracking prey");
        data.put("Last Prey", lastPrey == null ? "none" : String.valueOf(lastPrey.getId()));
        data.put("Successful Attacks", String.valueOf(successfulAttacks));
        data.put("Failed Attacks", String.valueOf(failedAttacks));
        return data;
    }

    private boolean seekWater(WorldModel world) {
        // move toward water or drink if already close enough
        int[] water = world.findNearestWater(row, col, AnimalGeneEffects.detectionRange(this, "water"));
        if (water == null) return false;
        if (distanceTo(water[0], water[1]) <= 1.5) {
            drink(AnimalGeneEffects.drinkAmount(this));
            return true;
        }
        return moveTowardWithSpeed(water[0], water[1], world);
    }

    private boolean tryReproduce(WorldModel world) {
        // reproduce only if the predator population is not too high for the herbivore population
        int herbivores = world.countLivingAnimalsByType("Herbivore");
        int predatorLimit = Math.max(12, herbivores / 45 + 4);
        if (world.countLivingAnimalsByType("Predator") > predatorLimit) return false;
        if (!isReproductivelyReady() || RANDOM.nextDouble() > getGene("fertility") * 0.58) return false;
        Animal mate = world.findNearestMate(this, Math.max(60.0, AnimalGeneEffects.detectionRange(this, "social")));
        if (mate == null) return false;
        if (distanceTo(mate) <= 8.0) reproduceWith(mate, world); else moveTowardWithSpeed(mate.getRow(), mate.getCol(), world);
        return true;
    }

    private boolean scavenge(WorldModel world) {
        // search for corpse calories and eat them if nearby
        int[] corpse = world.findNearestCorpse(row, col, AnimalGeneEffects.detectionRange(this, "food"));
        if (corpse == null) return false;
        if (distanceTo(corpse[0], corpse[1]) <= 1.5) {
            double bite = 14 + getGene("size") * 9 + getGene("digestion") * 5;
            double eaten = world.eatCorpseAt(corpse[0], corpse[1], bite);
            gainCalories(eaten * (0.78 + getGene("digestion") * 0.16));
            drink(eaten * 0.16);
            return true;
        }
        return moveTowardWithSpeed(corpse[0], corpse[1], world);
    }

    private boolean hunt(WorldModel world) {
        // find prey, chase it if needed, and attack if close enough
        Animal prey = findBestPrey(world);
        if (prey == null) {
            frustration = clamp(frustration + 0.05, 0, 1);
            return false;
        }
        lastPrey = prey;
        if (distanceTo(prey) > 3.5) {
            return moveTowardWithSpeed(prey.getRow(), prey.getCol(), world);
        }
        attack(prey, world);
        return true;
    }

    private Animal findBestPrey(WorldModel world) {
        // score nearby herbivores based on food value, weakness, escape ability, pack help, and distance
        Animal best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double range = AnimalGeneEffects.detectionRange(this, "prey");
        for (Animal prey : world.getAnimalsSnapshot()) {
            if (!canEatAnimal(prey) || !prey.isAlive()) continue;
            double distance = distanceTo(prey);
            if (distance > range) continue;
            int pack = countPackAllies(world, prey);
            double preyValue = prey.getCalories() * 0.04 + prey.getGene("size") * 2.0 + (1.0 - prey.healthRatio()) * 3.0;
            double escapeCost = prey.getGene("speed") * 0.70 + prey.getGene("fear") * 0.38
                    + prey.getGene("social") * 0.20 + prey.getGene("camouflage") * 0.85 + prey.getGene("armor") * 0.32;
            double score = preyValue + pack * getGene("social") * 0.5 + getGene("camouflage") * 0.28 - escapeCost - distance * 0.28;
            if (score > bestScore) {
                bestScore = score;
                best = prey;
            }
        }
        return best;
    }

    private void attack(Animal prey, WorldModel world) {
        // attacking costs resources, then uses accuracy to decide if the attack succeeds
        double cost = 1.0 + getGene("attack") * 0.75 + getGene("speed") * 0.50;
        if (!payActionCost(cost, cost * 0.25)) {
            rest();
            return;
        }
        int pack = countPackAllies(world, prey);
        double accuracy = 0.46 + getGene("speed") * 0.10 + getGene("sense") * 0.06 + getGene("aggression") * 0.08
                + getGene("camouflage") * 0.08 + pack * getGene("social") * 0.03
                - prey.getGene("speed") * 0.095 - prey.getGene("fear") * 0.07 - prey.getGene("camouflage") * 0.10;
        if (RANDOM.nextDouble() > clamp(accuracy, 0.08, 0.78)) {
            failedAttacks++;
            frustration = clamp(frustration + 0.12, 0, 1);
            return;
        }
        double damage = prey.getHealth() + 500.0;
        prey.takeDamage(damage, world);
        successfulAttacks++;
        frustration = clamp(frustration - 0.20, 0, 1);
        if (!prey.isAlive()) {
            double eaten = world.eatCorpseAt(prey.getRow(), prey.getCol(), 62 + getGene("size") * 22);
            gainCalories(eaten * (0.78 + getGene("digestion") * 0.16));
            drink(eaten * 0.16);
        }
    }

    private int countPackAllies(WorldModel world, Animal prey) {
        // nearby predators increase hunting effectiveness through pack support
        int count = 0;
        for (Animal animal : world.getAnimalsSnapshot()) {
            if (animal != this && animal instanceof Predator && animal.isAlive() && animal.distanceTo(prey) <= 3.5 + getGene("social")) {
                count++;
            }
        }
        return count;
    }
}
