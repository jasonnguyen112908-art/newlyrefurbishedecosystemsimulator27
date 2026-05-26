// this enum lists all animal genes and keeps their values in a safe range
public enum AnimalGene {
    SIZE("size", 1.0, 0.45, 2.60),
    SPEED("speed", 1.0, 0.45, 2.80),
    SENSE("sense", 1.0, 0.30, 3.00),
    STAMINA("stamina", 1.0, 0.35, 2.80),
    METABOLISM("metabolism", 1.0, 0.35, 2.60),
    ARMOR("armor", 0.35, 0.00, 2.50),
    ATTACK("attack", 0.50, 0.00, 3.20),
    AGGRESSION("aggression", 0.45, 0.00, 1.00),
    FEAR("fear", 0.45, 0.00, 1.00),
    SOCIAL("social", 0.45, 0.00, 1.00),
    FERTILITY("fertility", 0.55, 0.05, 1.70),
    PARENTAL_CARE("parentalCare", 0.45, 0.00, 1.50),
    LITTER_SIZE("litterSize", 1.10, 1.00, 3.50),
    CAMOUFLAGE("camouflage", 0.35, 0.00, 1.50),
    DIGESTION("digestion", 1.00, 0.40, 2.20),
    WATER_RETENTION("waterRetention", 1.00, 0.30, 2.30),
    MUTATION_RATE("mutationRate", 0.055, 0.00, 0.35),
    MUTATION_STRENGTH("mutationStrength", 0.075, 0.00, 0.35);

    // key is the string name used inside animal gene maps
    private final String key;

    // default value is used when no custom value is given
    private final double defaultValue;

    // min and max prevent mutation from creating broken values
    private final double min;
    private final double max;

    AnimalGene(String key, double defaultValue, double min, double max) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public String getKey() {
        return key;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public double cleanValue(double value) {
        // if the number is invalid, reset it to the default
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            value = defaultValue;
        }

        // clamp the value so it stays between the gene minimum and maximum
        return Math.max(min, Math.min(max, value));
    }

    public static AnimalGene findByKey(String key) {
        // search for the enum value that matches the given string key
        for (AnimalGene gene : values()) {
            if (gene.key.equals(key)) {
                return gene;
            }
        }
        return null;
    }

    public static boolean isKnownGene(String key) {
        return findByKey(key) != null;
    }
}
