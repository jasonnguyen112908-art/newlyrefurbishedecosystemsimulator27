// mainly created by Kedaar
import java.util.EnumMap;

// this class creates starting plants with default genes
public class PlantFactory {
    private PlantFactory() {
    }

    public static Plant createRandomFounderPlant(int row, int col) {
        // get the normal plant genes and add small random variation
        EnumMap<PlantGene, Double> base = PlantGeneDefaults.createDefaultGenes();
        EnumMap<PlantGene, Double> genes = PlantMutation.createFounderGenes(base);

        // generation 0 means this plant was created at the start, not grown from a parent
        Plant plant = new Plant(row, col, genes, "Plant", 0);
        plant.setFounderGeneBaseline(base);
        plant.setVariationRecord(base, "Founder plant variation");
        return plant;
    }
}
