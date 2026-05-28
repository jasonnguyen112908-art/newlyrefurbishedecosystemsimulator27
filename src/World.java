// mainly created by Jason
// https://www.youtube.com/watch?v=om59cwR7psI&list=PL_QPQmz5C6WUF-pOQDsbsKbaBZqXj4qSq&index=2
import javax.swing.JPanel;

import java.awt.BorderLayout;

// this class is the main panel that holds the entire simulation screen
public class World extends JPanel {
    private static final long serialVersionUID = 1L;

    // the model stores the actual ecosystem data and simulation logic
    private final WorldModel model;

    // these panels are the three main parts of the gui
    private final StatsPanel statsPanel;
    private final WorldPanel worldPanel;
    private final ToolbarPanel toolbarPanel;

    public World(int rows, int cols, int tileSize) {
        // create the model first because every panel needs to read from it
        this.model = new WorldModel(rows, cols);

        // fill the starting world with ponds, rocks, plants, herbivores, and predators
        BalancedWorldSeeder.seed(model);

        // create the visual panels that display and control the model
        this.statsPanel = new StatsPanel(model);
        this.worldPanel = new WorldPanel(model, tileSize);
        this.toolbarPanel = new ToolbarPanel(model);

        // borderlayout separates the screen into top, middle, and bottom sections
        setLayout(new BorderLayout());

        // top = stats, center = ecosystem grid, bottom = tool buttons
        add(statsPanel, BorderLayout.NORTH);
        add(worldPanel, BorderLayout.CENTER);
        add(toolbarPanel, BorderLayout.SOUTH);

        // whenever the model changes, update the stats at the top
        model.addChangeListener(() -> statsPanel.updateStats(model));
    }
}
