import java.util.EnumMap;
import java.util.Map;

// this helper class creates and cleans plant gene maps
public class PlantGeneDefaults {
    private PlantGeneDefaults() {
    }

    public static EnumMap<PlantGene, Double> createDefaultGenes() {
        // enum map is efficient because the keys are plantgene enum values
        EnumMap<PlantGene, Double> genes = new EnumMap<>(PlantGene.class);
        for (PlantGene gene : PlantGene.values()) {
            genes.put(gene, gene.getDefaultValue());
        }
        return genes;
    }

    public static void cleanGenes(Map<PlantGene, Double> genes) {
        // make sure every plant gene exists and stays inside its safe range
        for (PlantGene gene : PlantGene.values()) {
            genes.put(gene, cleanGeneValue(gene, genes.containsKey(gene) ? genes.get(gene) : gene.getDefaultValue()));
        }
    }

    public static double cleanGeneValue(PlantGene gene, double value) {
        return gene.cleanValue(value);
    }
}
