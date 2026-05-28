// mainly created by Jason with help of youtube tutorials and website
// https://www.geeksforgeeks.org/java/jlabel-java-swing/
// https://www.youtube.com/watch?v=NWhkCZZumAE
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.util.List;

// this panel shows live numbers about the simulation at the top of the window
public class StatsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // each jlabel is one piece of text shown to the user
    private final JLabel tickLabel;
    private final JLabel plantCountLabel;
    private final JLabel animalCountLabel;
    private final JLabel predatorCountLabel;
    private final JLabel herbivoreCountLabel;
    private final JLabel averageGenerationLabel;
    private final JLabel averagePlantCaloriesLabel;
    private final JLabel averagePlantHeightLabel;
    private final JLabel averageAnimalCaloriesLabel;
    private final JLabel sunLabel;

    public StatsPanel(WorldModel model) {
        // create every label before adding it to the panel
        this.tickLabel = new JLabel();
        this.plantCountLabel = new JLabel();
        this.animalCountLabel = new JLabel();
        this.predatorCountLabel = new JLabel();
        this.herbivoreCountLabel = new JLabel();
        this.averageGenerationLabel = new JLabel();
        this.averagePlantCaloriesLabel = new JLabel();
        this.averagePlantHeightLabel = new JLabel();
        this.averageAnimalCaloriesLabel = new JLabel();
        this.sunLabel = new JLabel();

        // add the labels so swing displays them from left to right
        add(tickLabel);
        add(plantCountLabel);
        add(animalCountLabel);
        add(predatorCountLabel);
        add(herbivoreCountLabel);
        add(averageGenerationLabel);
        add(averagePlantCaloriesLabel);
        add(averagePlantHeightLabel);
        add(averageAnimalCaloriesLabel);
        add(sunLabel);

        updateStats(model);
    }

    public void updateStats(WorldModel model) {
        // get current plant and animal data from the model
        List<Plant> plants = model.getPlantsSnapshot();
        List<Animal> animals = model.getAnimalsSnapshot();
        PlantStats plantStats = calculatePlantStats(plants);
        AnimalStats animalStats = calculateAnimalStats(animals);

        // update the label text so the user sees the newest simulation values
        tickLabel.setText("Tick: " + model.getTickCount() + " | Year: " + format(model.getYear()) + " | " + model.getSeasonName());
        plantCountLabel.setText("Plants: " + plants.size());
        animalCountLabel.setText("Animals: " + model.countLivingAnimals());
        predatorCountLabel.setText("Predators: " + model.countLivingAnimalsByType("Predator"));
        herbivoreCountLabel.setText("Herbivores: " + model.countLivingAnimalsByType("Herbivore"));
        averageGenerationLabel.setText("Avg Gen: " + format((plantStats.averageGeneration + animalStats.averageGeneration) / 2.0));
        averagePlantCaloriesLabel.setText("Avg Plant Calories: " + format(plantStats.averageCalories));
        averagePlantHeightLabel.setText("Avg Plant Height: " + format(plantStats.averageHeight));
        averageAnimalCaloriesLabel.setText("Avg Animal Calories: " + format(animalStats.averageCalories));
        sunLabel.setText("Sun: " + format(model.getSunValue()));
    }

    private PlantStats calculatePlantStats(List<Plant> plants) {
        // avoid dividing by zero when no plants exist
        if (plants.isEmpty()) {
            return new PlantStats(0, 0, 0);
        }

        double totalGeneration = 0;
        double totalCalories = 0;
        double totalHeight = 0;

        // add up plant values so averages can be calculated
        for (Plant plant : plants) {
            totalGeneration += plant.getGeneration();
            totalCalories += plant.getCalories();
            totalHeight += plant.getHeight();
        }

        return new PlantStats(
                totalGeneration / plants.size(),
                totalCalories / plants.size(),
                totalHeight / plants.size()
        );
    }

    private AnimalStats calculateAnimalStats(List<Animal> animals) {
        int living = 0;
        double totalGeneration = 0;
        double totalCalories = 0;

        // only living animals count toward animal averages
        for (Animal animal : animals) {
            if (animal.isAlive()) {
                living++;
                totalGeneration += animal.getGeneration();
                totalCalories += animal.getCalories();
            }
        }

        if (living == 0) {
            return new AnimalStats(0, 0);
        }

        return new AnimalStats(totalGeneration / living, totalCalories / living);
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }

    // this small helper class groups plant average values together
    private static class PlantStats {
        private final double averageGeneration;
        private final double averageCalories;
        private final double averageHeight;

        private PlantStats(double averageGeneration, double averageCalories, double averageHeight) {
            this.averageGeneration = averageGeneration;
            this.averageCalories = averageCalories;
            this.averageHeight = averageHeight;
        }
    }

    // this small helper class groups animal average values together
    private static class AnimalStats {
        private final double averageGeneration;
        private final double averageCalories;

        private AnimalStats(double averageGeneration, double averageCalories) {
            this.averageGeneration = averageGeneration;
            this.averageCalories = averageCalories;
        }
    }
}
