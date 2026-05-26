import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

// this helper class adds small random genetic changes to animals
public class AnimalMutation {
    private static final Random RANDOM = new Random();

    // founder variation controls how different starting animals are from the base genes
    private static final double FOUNDER_VARIATION = 0.12;

    // tiny changes below this amount are ignored when counting differences
    private static final double DIFFERENCE_THRESHOLD = 0.0001;

    private AnimalMutation() {
    }

    public static Map<String, Double> createFounderGenes(Map<String, Double> baseGenes, Map<String, Double> speciesAdjustments) {
        // create a new gene map so the original base genes do not get changed
        LinkedHashMap<String, Double> genes = new LinkedHashMap<>();

        // loop through every known animal gene and give it a safe value
        for (AnimalGene gene : AnimalGene.values()) {
            String key = gene.getKey();
            double value = speciesAdjustments != null && speciesAdjustments.containsKey(key)
                    ? speciesAdjustments.get(key)
                    : baseGenes.getOrDefault(key, gene.getDefaultValue());

            // multiply by a small random amount so founders are not all identical
            value *= 1.0 + (RANDOM.nextDouble() * 2.0 - 1.0) * FOUNDER_VARIATION;
            genes.put(key, gene.cleanValue(value));
        }
        return genes;
    }

    public static int countDifferences(Map<String, Double> originalGenes, Map<String, Double> newGenes) {
        // count how many genes changed noticeably from the original version
        int count = 0;
        for (String key : newGenes.keySet()) {
            if (originalGenes.containsKey(key) && Math.abs(originalGenes.get(key) - newGenes.get(key)) > DIFFERENCE_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    public static String summarizeDifferences(Map<String, Double> originalGenes, Map<String, Double> newGenes) {
        // create readable text that explains some of the gene changes
        StringBuilder summary = new StringBuilder();
        int shown = 0;
        for (String key : newGenes.keySet()) {
            if (originalGenes.containsKey(key) && Math.abs(originalGenes.get(key) - newGenes.get(key)) > DIFFERENCE_THRESHOLD && shown < 10) {
                summary.append(key).append(": ").append(format(originalGenes.get(key))).append(" -> ").append(format(newGenes.get(key))).append("; ");
                shown++;
            }
        }
        return summary.length() == 0 ? "No visible genetic change" : summary.toString();
    }

    private static String format(double value) {
        return String.format("%.3f", value);
    }
}
