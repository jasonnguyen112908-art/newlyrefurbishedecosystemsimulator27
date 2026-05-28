// mainly created by Kedaar
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

// this class stores animal genes and helps create inherited child genes
public class Genome {
    // linkedhashmap keeps genes in a predictable order for inspection
    private final LinkedHashMap<String, Double> values = new LinkedHashMap<>();

    public static Genome createDefaultAnimalGenome() {
        // create a genome with every animal gene set to its default value
        Genome genome = new Genome();
        for (AnimalGene gene : AnimalGene.values()) {
            genome.put(gene.getKey(), gene.getDefaultValue());
        }
        return genome;
    }

    public static Genome fromAnimalGenes(Map<String, Double> customGenes) {
        // start with defaults so missing genes still have safe values
        Genome genome = createDefaultAnimalGenome();
        if (customGenes != null) {
            for (Map.Entry<String, Double> entry : customGenes.entrySet()) {
                genome.put(entry.getKey(), entry.getValue());
            }
        }
        return genome;
    }

    public static Genome createMixedMutatedGenome(Genome first, Genome second, Random random) {
        // child genes are chosen from either parent and may randomly mutate
        Genome child = new Genome();
        double rate = (first.get("mutationRate") + second.get("mutationRate")) / 2.0;
        double strength = (first.get("mutationStrength") + second.get("mutationStrength")) / 2.0;

        for (String geneName : first.values.keySet()) {
            double value = random.nextBoolean() ? first.get(geneName) : second.get(geneName);
            if (random.nextDouble() < rate) {
                value *= 1.0 + (random.nextDouble() * 2.0 - 1.0) * strength;
            }
            child.put(geneName, value);
        }
        return child;
    }

    public void put(String geneName, double value) {
        // gene names must be real text so the map can find them later
        if (geneName == null || geneName.trim().isEmpty()) {
            throw new IllegalArgumentException("Gene name cannot be blank");
        }
        values.put(geneName, cleanValue(geneName, value));
    }

    public double get(String geneName) {
        // missing genes should be treated as an error because animals need complete data
        if (!values.containsKey(geneName)) {
            throw new IllegalArgumentException("Missing animal gene: " + geneName);
        }
        return values.get(geneName);
    }

    public boolean contains(String geneName) {
        return values.containsKey(geneName);
    }

    public double distanceTo(Genome other) {
        // genetic distance measures how different two animals are from each other
        if (other == null) {
            return Double.MAX_VALUE;
        }

        double total = 0;
        int count = 0;
        for (String geneName : values.keySet()) {
            if (!other.contains(geneName)) {
                continue;
            }
            double a = get(geneName);
            double b = other.get(geneName);
            double scale = (Math.abs(a) + Math.abs(b)) / 2.0;
            if (scale > 0) {
                total += Math.abs(a - b) / scale;
                count++;
            }
        }
        return count == 0 ? Double.MAX_VALUE : total / count;
    }

    public Map<String, Double> toMap() {
        // return a copy so outside classes do not directly change this genome
        return new LinkedHashMap<>(values);
    }

    Map<String, Double> backingMap() {
        return values;
    }

    private double cleanValue(String geneName, double value) {
        // known animal genes use their own min and max limits
        AnimalGene gene = AnimalGene.findByKey(geneName);
        if (gene != null) {
            return gene.cleanValue(value);
        }
        return Math.max(0.001, Double.isFinite(value) ? value : 0.001);
    }
}
