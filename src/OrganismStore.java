import java.util.ArrayList;
import java.util.List;

// this class is part of the model/data system
// it keeps the master lists of plants and animals separate from the visual swing code
public class OrganismStore {
    // these lists store every plant and animal currently tracked by the simulation
    private final List<Plant> plants;
    private final List<Animal> animals;

    public OrganismStore() {
        // create empty lists before the world is seeded
        this.plants = new ArrayList<>();
        this.animals = new ArrayList<>();
    }

    public boolean addPlant(Plant plant) {
        // add a plant only if it is real and not already stored
        if (plant == null || plants.contains(plant)) {
            return false;
        }

        return plants.add(plant);
    }

    public boolean removePlant(Plant plant) {
        // remove plants when they die or get erased
        if (plant == null) {
            return false;
        }

        return plants.remove(plant);
    }

    public boolean addAnimal(Animal animal) {
        // add an animal only if it is real and not already stored
        if (animal == null || animals.contains(animal)) {
            return false;
        }

        return animals.add(animal);
    }

    public boolean removeAnimal(Animal animal) {
        // remove animals when they die or get erased
        if (animal == null) {
            return false;
        }

        return animals.remove(animal);
    }

    public List<Plant> getPlantsSnapshot() {
        // return a copy so outside loops can read plants safely
        return new ArrayList<>(plants);
    }

    public List<Animal> getAnimalsSnapshot() {
        // return a copy so animals can be added or removed without breaking loops
        return new ArrayList<>(animals);
    }

    public int countLivingAnimals() {
        // count living animals for the stats panel and balancing logic
        int count = 0;

        for (Animal animal : animals) {
            if (animal.isAlive()) {
                count++;
            }
        }

        return count;
    }

    public Animal findNearestMate(Animal searcher, double visionRange) {
        // find the best nearby mate for an animal
        Animal best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Animal animal : animals) {
            if (animal != searcher && animal.isAlive() && searcher.canReproduceWith(animal)) {
                double distance = searcher.distanceTo(animal);

                if (distance <= visionRange) {
                    double score = AnimalGeneEffects.mateScore(searcher, animal) - distance * 0.03;

                    if (score > bestScore) {
                        bestScore = score;
                        best = animal;
                    }
                }
            }
        }

        return best;
    }

    public Animal findNearestPrey(Animal hunter, double visionRange) {
        // find the closest animal that a hunter can eat
        Animal closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Animal animal : animals) {
            if (animal != hunter && animal.isAlive() && hunter.canEatAnimal(animal)) {
                double distance = hunter.distanceTo(animal);

                if (distance <= visionRange && distance < closestDistance) {
                    closestDistance = distance;
                    closest = animal;
                }
            }
        }

        return closest;
    }
}
