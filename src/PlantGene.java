// mainly created by Kedaar
// this enum lists all plant genes and keeps their values in a safe range
public enum PlantGene {
    MAX_HEIGHT(4.0, 1.0, 8.0),
    GROWTH_RATE(0.65, 0.15, 1.60),
    LEAF_SIZE(1.0, 0.30, 3.20),
    ROOT_DEPTH(1.0, 0.30, 3.20),
    WOODINESS(0.75, 0.10, 2.60),
    TOXICITY(0.08, 0.00, 2.50),
    THORNS(0.15, 0.00, 2.20),
    WATER_STORAGE(1.0, 0.30, 3.00),
    SEED_SPREAD(4.0, 1.0, 11.0),
    SEED_SIZE(1.0, 0.35, 2.80),
    SHADE_TOLERANCE(0.55, 0.00, 1.50),
    FERTILITY(0.75, 0.15, 1.70),
    MUTATION_RATE(0.045, 0.00, 0.30),
    MUTATION_STRENGTH(0.065, 0.00, 0.30);

    // default value is used when a new plant starts with normal genes
    private final double defaultValue;

    // min and max stop mutation from making impossible plant traits
    private final double min;
    private final double max;

    PlantGene(double defaultValue, double min, double max) {
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public double cleanValue(double value) {
        // invalid numbers are reset to the default value
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            value = defaultValue;
        }

        // clamp keeps the gene inside its safe range
        return Math.max(min, Math.min(max, value));
    }
}
