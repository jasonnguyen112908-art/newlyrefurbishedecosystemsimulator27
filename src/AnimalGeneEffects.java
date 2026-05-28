// mainly created by Kedaar
// this class is part of the gene and biology system
// it translates animal genes into actual simulation effects like health, speed, hunger, and reproduction
public class AnimalGeneEffects {
    private AnimalGeneEffects() {
    }

    // converts genes into maximum health
    public static double maxHealth(Animal a) {
        return 42 + a.getGene("size") * 30 + a.getGene("stamina") * 9 + a.getGene("armor") * 18;
    }

    // converts genes into maximum calorie storage
    public static double maxCalories(Animal a) {
        return 55 + a.getGene("size") * 42 + a.getGene("stamina") * 14 + a.getGene("digestion") * 8;
    }

    // converts genes into maximum hydration storage
    public static double maxHydration(Animal a) {
        return 48 + a.getGene("size") * 20 + a.getGene("stamina") * 10 + a.getGene("waterRetention") * 22;
    }

    // converts genes into maximum movement and action energy
    public static double maxEnergy(Animal a) {
        double value = 38 + a.getGene("stamina") * 34 - a.getGene("armor") * 5 - a.getGene("waterRetention") * 2;
        return Math.max(28, value);
    }

    // controls how quickly animals lose calories every tick
    public static double calorieBurn(Animal a) {
        double burn = 2.8
                + a.getGene("metabolism") * 2.6
                + a.getGene("size") * 1.35
                + a.getGene("speed") * 0.85
                + a.getGene("sense") * 0.45
                + a.getGene("armor") * 0.55
                + a.getGene("attack") * 0.38
                + a.getGene("camouflage") * 0.30
                + a.getGene("digestion") * 0.30
                + a.getGene("parentalCare") * 0.18;
        if (a instanceof Predator) return burn * 0.20;
        if (a instanceof Herbivore) return burn * 0.36;
        return burn;
    }

    // controls how quickly animals lose water every tick
    public static double hydrationLoss(Animal a) {
        double loss = 1.7 + a.getGene("metabolism") * 0.85 + a.getGene("size") * 0.55 + a.getGene("speed") * 0.30;
        loss /= 0.72 + a.getGene("waterRetention") * 0.48;
        if (a instanceof Predator) return loss * 0.20;
        if (a instanceof Herbivore) return loss * 0.55;
        return loss;
    }

    // controls how quickly animals regain energy
    public static double energyRecovery(Animal a) {
        return 5.5 + a.getGene("stamina") * 4.8 + a.getGene("metabolism") * 1.2 - a.getGene("armor") * 0.35;
    }

    // controls passive healing when animals have enough resources
    public static double healing(Animal a) {
        return 0.9 + a.getGene("metabolism") * 0.55 + a.getGene("stamina") * 0.45;
    }

    // controls how long animals can live before age damage starts
    public static int maxAge(Animal a) {
        double years = 20.0
                + a.getGene("stamina") * 2.5
                + a.getGene("waterRetention") * 0.8
                + a.getGene("armor") * 0.6
                - a.getGene("fertility") * 0.5
                - (a.getGene("litterSize") - 1.0) * 0.3
                - a.getGene("metabolism") * 0.2;
        if (a instanceof Predator) years += 6.0;
        return Math.max(24, (int) Math.round(years * SimulationState.TICKS_PER_YEAR));
    }

    // calculates how much energy it costs to enter a tile
    public static double moveEnergyCost(Animal a, WorldModel world, int row, int col) {
        double terrain = world == null ? 1.0 : world.getMovementCost(row, col);
        double bodyCost = 2.5 + a.getGene("size") * 1.0 + a.getGene("armor") * 1.3 + a.getGene("waterRetention") * 0.25;
        double speedCost = a.getGene("speed") * a.getGene("speed") * 1.35;
        double gutCost = Math.max(0, a.getGene("digestion") - 1.0) * 0.45;
        return terrain * (bodyCost + speedCost + gutCost) / Math.max(0.75, a.getGene("stamina"));
    }

    // calculates the calorie cost of moving
    public static double moveCalorieCost(Animal a) {
        return 0.6 + a.getGene("size") * 0.35 + a.getGene("metabolism") * 0.25 + a.getGene("speed") * 0.20;
    }

    // decides how many steps an animal can take in one action
    public static int movementSteps(Animal a, boolean fleeing) {
        double score = 1.15 + a.getGene("speed") * 1.20 + a.getGene("stamina") * 0.30
                - a.getGene("size") * 0.18 - a.getGene("armor") * 0.22;
        if (fleeing) score += a.getGene("fear") * 0.30;
        return Math.max(1, Math.min(4, (int) Math.round(score)));
    }

    // controls how far animals can detect food, danger, mates, or water
    public static double detectionRange(Animal a, String purpose) {
        double range = 3.2 + a.getGene("sense") * 4.6;
        if ("threat".equals(purpose)) range += a.getGene("fear") * 2.0;
        if ("social".equals(purpose)) range += a.getGene("social") * 3.0;
        if ("water".equals(purpose)) range += a.getGene("waterRetention") * 1.6;
        if ("prey".equals(purpose)) range += 5.0 + a.getGene("aggression") * 3.0 + a.getGene("stamina") * 2.0;
        return Math.max(1.5, range);
    }

    // decides how much water an animal gains from drinking
    public static double drinkAmount(Animal a) {
        return 18 + a.getGene("size") * 7 + a.getGene("waterRetention") * 8;
    }

    // reduces incoming damage based on defensive genes
    public static double defendedDamage(Animal victim, double damage) {
        double reduction = victim.getGene("armor") * 0.15 + victim.getGene("size") * 0.035 + victim.getGene("camouflage") * 0.02;
        return Math.max(0.0, damage * clamp(1.0 - reduction, 0.35, 1.0));
    }

    // decides how many calories a dead animal leaves behind
    public static double corpseCalories(Animal a) {
        return a.getCalories() + a.getGene("size") * 24;
    }

    // decides how many calories reproduction costs
    public static double reproductionCostCalories(Animal a) {
        return 10
                + a.getGene("size") * 5
                + a.getGene("fertility") * 3.6
                + a.getGene("parentalCare") * 8.5
                + (a.getGene("litterSize") - 1.0) * 9;
    }

    // decides how much energy reproduction costs
    public static double reproductionCostEnergy(Animal a) {
        return 5.5
                + a.getGene("speed") * 1.4
                + a.getGene("fertility") * 2.8
                + a.getGene("parentalCare") * 5.0
                + (a.getGene("litterSize") - 1.0) * 5.8;
    }

    // decides when an animal is old enough to reproduce
    public static int reproductionAge(Animal a) {
        double years = 1.6 + a.getGene("size") * 0.45 - a.getGene("fertility") * 0.35 + a.getGene("parentalCare") * 0.18;
        if (a instanceof Predator) years += 1.2;
        return Math.max(4, (int) Math.round(years * SimulationState.TICKS_PER_YEAR));
    }

    // decides how long an animal must wait after reproducing
    public static int reproductionCooldown(Animal a) {
        double seasons = 2.8
                + a.getGene("size") * 0.65
                + a.getGene("parentalCare") * 1.15
                + (a.getGene("litterSize") - 1.0) * 1.35
                - a.getGene("fertility") * 0.85;
        if (a instanceof Predator) seasons += 3.2;
        return Math.max(2, (int) Math.round(seasons));
    }

    // gives children starting resources based on parental care
    public static double childProvisionRatio(Animal parentOne, Animal parentTwo, int litterCount) {
        double care = (parentOne.getGene("parentalCare") + parentTwo.getGene("parentalCare")) / 2.0;
        double litterPenalty = Math.max(0, litterCount - 1) * 0.07;
        return clamp(0.48 + care * 0.30 - litterPenalty, 0.30, 0.88);
    }

    // scores how good a possible mate is
    public static double mateScore(Animal searcher, Animal candidate) {
        double vigor = (candidate.healthRatio() + candidate.energyRatio() + candidate.caloriesRatio()) / 3.0;
        double compatibility = 1.0 - searcher.geneticDistanceTo(candidate) * 0.40;
        return vigor + compatibility + searcher.getGene("social") * 0.23 + searcher.getGene("fertility") * 0.10
                + searcher.getGene("parentalCare") * 0.08;
    }

    // decides whether two animals are allowed to reproduce
    public static boolean canMate(Animal a, Animal b) {
        if (a == null || b == null || a.getSex() == b.getSex() || !a.getAnimalType().equals(b.getAnimalType())) return false;
        double tolerance = 0.62 + (a.getGene("social") + b.getGene("social")) * 0.10;
        return a.geneticDistanceTo(b) <= tolerance && mateScore(a, b) > 0.72;
    }

    // keeps a number between a minimum and maximum value
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
