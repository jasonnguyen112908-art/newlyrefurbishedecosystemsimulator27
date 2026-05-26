import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

// this helper class adds small random genetic changes to plants
public class PlantMutation {
    private static final Random RANDOM = new Random();

    // founder variation controls how different starting plants are from the base genes
    private static final double FOUNDER_VARIATION = 0.12;

    // tiny changes below this amount are ignored when counting differences
    private static final double DIFFERENCE_THRESHOLD = 0.0001;

    private PlantMutation() {
    }

    public static EnumMap<PlantGene, Double> createFounderGenes(Map<PlantGene, Double> baseGenes) {
        // founder plants always receive some small starting variation
        return variedCopy(baseGenes, FOUNDER_VARIATION, 1.0);
    }

    public static EnumMap<PlantGene, Double> createChildGenes(Map<PlantGene, Double> parentGenes) {
        // child mutation depends on the parent's mutation rate and strength genes
        double rate = parentGenes.get(PlantGene.MUTATION_RATE);
        double strength = parentGenes.get(PlantGene.MUTATION_STRENGTH);
        return variedCopy(parentGenes, strength, rate);
    }

    private static EnumMap<PlantGene, Double> variedCopy(Map<PlantGene, Double> source, double strength, double rate) {
        // create a new gene map so the original parent genes do not get changed
        EnumMap<PlantGene, Double> copy = new EnumMap<>(PlantGene.class);
        for (PlantGene gene : PlantGene.values()) {
            double value = source.containsKey(gene) ? source.get(gene) : gene.getDefaultValue();

            // randomly change the value based on the mutation rate
            if (RANDOM.nextDouble() < rate) {
                value *= 1.0 + (RANDOM.nextDouble() * 2.0 - 1.0) * strength;
            }
            copy.put(gene, PlantGeneDefaults.cleanGeneValue(gene, value));
        }
        return copy;
    }

    public static int countDifferences(Map<PlantGene, Double> originalGenes, Map<PlantGene, Double> newGenes) {
        // count how many plant genes changed noticeably
        int count = 0;
        for (PlantGene gene : PlantGene.values()) {
            if (Math.abs(originalGenes.get(gene) - newGenes.get(gene)) > DIFFERENCE_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    public static String summarizeDifferences(Map<PlantGene, Double> originalGenes, Map<PlantGene, Double> newGenes) {
        // create readable text that explains some of the gene changes
        StringBuilder summary = new StringBuilder();
        int shown = 0;
        for (PlantGene gene : PlantGene.values()) {
            if (Math.abs(originalGenes.get(gene) - newGenes.get(gene)) > DIFFERENCE_THRESHOLD && shown < 8) {
                summary.append(gene).append(": ").append(format(originalGenes.get(gene))).append(" -> ").append(format(newGenes.get(gene))).append("; ");
                shown++;
            }
        }
        return summary.length() == 0 ? "No visible genetic change" : summary.toString();
    }

    private static String format(double value) {
        return String.format("%.3f", value);
    }
}
