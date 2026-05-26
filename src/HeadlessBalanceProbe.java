import java.util.List;

public class HeadlessBalanceProbe {
    private static final int DEFAULT_RUNS = 5;
    private static final int DEFAULT_TICKS = 700;
    private static final int DEFAULT_INTERVAL = 100;

    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_RUNS;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_TICKS;
        int interval = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_INTERVAL;
        boolean verbose = args.length < 4 || !"quiet".equalsIgnoreCase(args[3]);
        int burnInTick = Math.min(240, Math.max(80, ticks / 3));

        int passed = 0;
        for (int run = 1; run <= runs; run++) {
            WorldModel model = new WorldModel(100, 200);
            BalancedWorldSeeder.seed(model);
            RunSummary summary = new RunSummary(run, burnInTick);

            for (int tick = 0; tick <= ticks; tick++) {
                PopulationStats stats = collectStats(model);
                summary.observe(model.getTickCount(), stats);
                if (verbose && (tick % interval == 0 || tick == ticks)) {
                    printStats(run, model, stats);
                }
                if (tick < ticks) {
                    model.updateWorld();
                }
            }

            boolean runPassed = summary.passed();
            if (runPassed) passed++;
            System.out.println(summary.describe(runPassed));
        }

        System.out.printf("BALANCE_RESULT,%s,PASSED_RUNS,%d,TOTAL_RUNS,%d%n",
                passed == runs ? "PASS" : "CHECK", passed, runs);
    }

    private static PopulationStats collectStats(WorldModel model) {
        List<Plant> plants = model.getPlantsSnapshot();
        List<Animal> animals = model.getAnimalsSnapshot();
        PopulationStats stats = new PopulationStats();
        stats.tick = model.getTickCount();
        stats.plants = plants.size();
        stats.herbivores = model.countLivingAnimalsByType("Herbivore");
        stats.predators = model.countLivingAnimalsByType("Predator");

        for (Plant plant : plants) {
            stats.plantCalories += plant.getCalories();
            stats.plantHeight += plant.getHeight();
            stats.maxPlantGeneration = Math.max(stats.maxPlantGeneration, plant.getGeneration());
        }

        for (Animal animal : animals) {
            if (animal.isAlive()) {
                stats.maxAnimalGeneration = Math.max(stats.maxAnimalGeneration, animal.getGeneration());
            }
        }
        return stats;
    }

    private static void printStats(int run, WorldModel model, PopulationStats stats) {
        System.out.printf(
                "RUN,%d,TICK,%d,YEAR,%.1f,PLANTS,%d,HERBIVORES,%d,PREDATORS,%d,AVG_PLANT_CALORIES,%.2f,AVG_PLANT_HEIGHT,%.2f,MAX_PLANT_GEN,%d,MAX_ANIMAL_GEN,%d%n",
                run,
                model.getTickCount(),
                model.getYear(),
                stats.plants,
                stats.herbivores,
                stats.predators,
                stats.plants == 0 ? 0.0 : stats.plantCalories / stats.plants,
                stats.plants == 0 ? 0.0 : stats.plantHeight / stats.plants,
                stats.maxPlantGeneration,
                stats.maxAnimalGeneration
        );
    }

    private static class PopulationStats {
        int tick;
        int plants;
        int herbivores;
        int predators;
        int maxPlantGeneration;
        int maxAnimalGeneration;
        double plantCalories;
        double plantHeight;
    }

    private static class RunSummary {
        private final int run;
        private final int burnInTick;
        private int finalPlants;
        private int finalHerbivores;
        private int finalPredators;
        private int minPlants = Integer.MAX_VALUE;
        private int maxPlants = Integer.MIN_VALUE;
        private int minHerbivores = Integer.MAX_VALUE;
        private int maxHerbivores = Integer.MIN_VALUE;
        private int minPredators = Integer.MAX_VALUE;
        private int maxPredators = Integer.MIN_VALUE;
        private int maxAnimalGeneration;
        private int maxPlantGeneration;

        RunSummary(int run, int burnInTick) {
            this.run = run;
            this.burnInTick = burnInTick;
        }

        void observe(int tick, PopulationStats stats) {
            finalPlants = stats.plants;
            finalHerbivores = stats.herbivores;
            finalPredators = stats.predators;
            maxAnimalGeneration = Math.max(maxAnimalGeneration, stats.maxAnimalGeneration);
            maxPlantGeneration = Math.max(maxPlantGeneration, stats.maxPlantGeneration);

            if (tick < burnInTick) {
                return;
            }

            minPlants = Math.min(minPlants, stats.plants);
            maxPlants = Math.max(maxPlants, stats.plants);
            minHerbivores = Math.min(minHerbivores, stats.herbivores);
            maxHerbivores = Math.max(maxHerbivores, stats.herbivores);
            minPredators = Math.min(minPredators, stats.predators);
            maxPredators = Math.max(maxPredators, stats.predators);
        }

        boolean passed() {
            return finalPlants >= 1500 && finalPlants <= 12000
                    && finalHerbivores >= 35 && finalHerbivores <= 430
                    && finalPredators >= 1 && finalPredators <= 18
                    && minHerbivores >= 15
                    && maxHerbivores <= 430
                    && minPredators >= 1
                    && maxPlants <= 14000
                    && maxAnimalGeneration >= 4
                    && maxPlantGeneration >= 4;
        }

        String describe(boolean passed) {
            return String.format(
                    "RUN_SUMMARY,%d,%s,BURN_IN,%d,FINAL_PLANTS,%d,FINAL_HERBIVORES,%d,FINAL_PREDATORS,%d,PLANT_RANGE,%d-%d,HERBIVORE_RANGE,%d-%d,PREDATOR_RANGE,%d-%d,MAX_PLANT_GEN,%d,MAX_ANIMAL_GEN,%d",
                    run,
                    passed ? "PASS" : "CHECK",
                    burnInTick,
                    finalPlants,
                    finalHerbivores,
                    finalPredators,
                    safe(minPlants),
                    safe(maxPlants),
                    safe(minHerbivores),
                    safe(maxHerbivores),
                    safe(minPredators),
                    safe(maxPredators),
                    maxPlantGeneration,
                    maxAnimalGeneration
            );
        }

        private int safe(int value) {
            return value == Integer.MAX_VALUE || value == Integer.MIN_VALUE ? 0 : value;
        }
    }
}
