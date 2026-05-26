// this class stores global simulation information like time, seasons, and sun level
public class SimulationState {
    // every 4 ticks equals one year in the simulation
    public static final int TICKS_PER_YEAR = 4;

    private static final double DEFAULT_SUN_VALUE = 1.0;
    private static final double MIN_SUN_VALUE = 0.0;
    private static final double MAX_SUN_VALUE = 1.5;

    private double sunValue;
    private int tickCount;
    private int nextGeneratedSpeciesNumber;

    public SimulationState() {
        this.sunValue = DEFAULT_SUN_VALUE;
        this.tickCount = 0;
        this.nextGeneratedSpeciesNumber = 1;
    }

    public void advanceTick() {
        // one call to updateWorld counts as one tick
        tickCount++;
    }

    public int getTickCount() {
        return tickCount;
    }

    public double getYear() {
        // divide by ticks per year so tick count becomes a year value
        return tickCount / (double) TICKS_PER_YEAR;
    }

    public String getSeasonName() {
        // the remainder decides which season the current tick is in
        switch (tickCount % TICKS_PER_YEAR) {
            case 0: return "Spring";
            case 1: return "Summer";
            case 2: return "Autumn";
            default: return "Winter";
        }
    }

    public double getSunValue() {
        return sunValue;
    }

    public boolean setSunValue(double sunValue) {
        // clamp keeps the sun value inside a safe range
        double clampedValue = clamp(sunValue, MIN_SUN_VALUE, MAX_SUN_VALUE);

        if (this.sunValue == clampedValue) {
            return false;
        }

        this.sunValue = clampedValue;
        return true;
    }

    public String createGeneratedSpeciesName() {
        // this creates names for new species that appear through mutation
        return "Species-" + nextGeneratedSpeciesNumber++;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
