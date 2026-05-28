// mainly created by Kedaar
import java.util.LinkedHashMap;
import java.util.Map;

// this class creates starting animals with default genes
public class AnimalFactory {
    private AnimalFactory() {
    }

    public static Herbivore createRandomFounderHerbivore(int row, int col) {
        // get the normal herbivore genes and add small random variation
        Map<String, Double> base = herbivoreDefaults();
        Map<String, Double> genes = AnimalMutation.createFounderGenes(base, null);

        // generation 0 means this animal was created at the start, not born from parents
        Herbivore herbivore = new Herbivore(row, col, genes, "Herbivore", 0);
        herbivore.setFounderGeneBaseline(base);
        herbivore.setVariationRecord(base, "Founder herbivore variation");
        return herbivore;
    }

    public static Predator createRandomFounderPredator(int row, int col) {
        // get the normal predator genes and add small random variation
        Map<String, Double> base = predatorDefaults();
        Map<String, Double> genes = AnimalMutation.createFounderGenes(base, null);

        // generation 0 means this animal was created at the start, not born from parents
        Predator predator = new Predator(row, col, genes, "Predator", 0);
        predator.setFounderGeneBaseline(base);
        predator.setVariationRecord(base, "Founder predator variation");
        return predator;
    }

    private static Map<String, Double> herbivoreDefaults() {
        // herbivore genes make herbivores faster, more fearful, and better at eating plants
        Map<String, Double> g = Genome.createDefaultAnimalGenome().toMap();
        g.put("size", 0.90);
        g.put("speed", 1.22);
        g.put("sense", 1.28);
        g.put("stamina", 1.15);
        g.put("metabolism", 0.82);
        g.put("armor", 0.42);
        g.put("attack", 0.10);
        g.put("aggression", 0.16);
        g.put("fear", 0.68);
        g.put("social", 0.70);
        g.put("fertility", 0.88);
        g.put("parentalCare", 0.52);
        g.put("litterSize", 1.25);
        g.put("camouflage", 0.55);
        g.put("digestion", 1.12);
        g.put("waterRetention", 1.05);
        return g;
    }

    private static Map<String, Double> predatorDefaults() {
        // predator genes make predators stronger, more aggressive, and better at hunting
        Map<String, Double> g = new LinkedHashMap<>(Genome.createDefaultAnimalGenome().toMap());
        g.put("size", 1.28);
        g.put("speed", 1.32);
        g.put("sense", 1.78);
        g.put("stamina", 1.24);
        g.put("metabolism", 0.70);
        g.put("armor", 0.55);
        g.put("attack", 1.10);
        g.put("aggression", 0.60);
        g.put("fear", 0.16);
        g.put("social", 0.46);
        g.put("fertility", 1.0);
        g.put("parentalCare", 0.62);
        g.put("litterSize", 2.0);
        g.put("camouflage", 0.62);
        g.put("digestion", 1.18);
        g.put("waterRetention", 1.00);
        return g;
    }
}
