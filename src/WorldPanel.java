//mainly Jason, I watched youtube tutorials on utilizing java swing to create visual tiles and awt to color tiles and implementing mouse listeners
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// this class displays the ecosystem as a colored swing grid
public class WorldPanel extends JPanel {
    // swing panels can be serialized, so this version id prevents a warning sidenote: not necessary, but tutorial recommended implementing this syntax to avoid warnings
    private static final long serialVersionUID = 1L;

    // these colors translate model data into visuals on the grid
    private static final Color ANIMAL_COLOR = Color.WHITE;
    private static final Color HERBIVORE_COLOR = new Color(230, 230, 230);
    private static final Color PREDATOR_COLOR = new Color(180, 30, 30);
    private static final Color PLANT_COLOR = new Color(0, 90, 0);
    private static final Color CORPSE_COLOR = new Color(120, 70, 35);
    private static final Color GRASS_COLOR = Color.GREEN;
    private static final Color WATER_COLOR = Color.BLUE;
    private static final Color ROCK_COLOR = Color.GRAY;

    // the model stores the actual ecosystem data that this panel reads from
    private final WorldModel model;
    private final int tileSize;

    // each jpanel in this 2d array represents one visible tile on the screen
    private final JPanel[][] tilePanels;

    public WorldPanel(WorldModel model, int tileSize) {
        this.model = model;
        this.tileSize = tileSize;

        // create storage for the visual tiles using the same size as the model grid
        this.tilePanels = new JPanel[model.getRows()][model.getCols()];

        // gridlayout arranges all tile panels into rows and columns
        setLayout(new GridLayout(model.getRows(), model.getCols()));

        // create the panels first, then color them based on the model
        createTilePanels();
        refreshAllTiles();

        // register update methods so the display changes when the model changes
        //learnt syntax from youtube tutorial demonstrating creating a 2d game using java swing
        model.addChangeListener(this::repaint);
        model.addTileChangeListener(this::refreshTile);
    }

    private void createTilePanels() {
        // create one small swing panel for every tile in the model grid
        for (int row = 0; row < model.getRows(); row++) {
            for (int col = 0; col < model.getCols(); col++) {
                JPanel tilePanel = createTilePanel(row, col);
                tilePanels[row][col] = tilePanel;
                add(tilePanel);
            }
        }
    }

    private JPanel createTilePanel(int row, int col) {
        // this creates one visible square for one model tile
        JPanel tilePanel = new JPanel();
        tilePanel.setPreferredSize(new Dimension(tileSize, tileSize)); 
        tilePanel.setOpaque(true);

        // left click edits the tile, while right click opens the inspector
        //general idea of code came from youtube tutorial demonstrating how to add mouselisteners
        //to jpanels but applying it to open up external panels didn't come from youtube
        tilePanel.addMouseListener(new MouseAdapter() {
            @Override 
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    InspectorDialog.show(WorldPanel.this, model, row, col);
                } else {
                    model.useSelectedTool(row, col);
                }
            }
        });

        return tilePanel;
    }

    public void refreshAllTiles() {
        // recolor every tile, usually when the grid is first created
        for (int row = 0; row < model.getRows(); row++) {
            for (int col = 0; col < model.getCols(); col++) {
                refreshTile(row, col);
            }
        }
    }

    private void refreshTile(int row, int col) {
        // ignore invalid coordinates so the display does not crash
        if (row < 0 || row >= tilePanels.length || col < 0 || col >= tilePanels[row].length) {
            return;
        }

        // update the color of one visible tile based on the model data
        tilePanels[row][col].setBackground(getTileColor(row, col));
    }

    private Color getTileColor(int row, int col) {
        // decide the visual color by checking what exists on this model tile
        Tile tile = model.getTileObject(row, col);

        if (tile == null) {
            return GRASS_COLOR;
        }

        // animals appear first so they visually cover terrain, plants, and corpses
        if (tile.hasLivingAnimal()) {
            Animal animal = tile.getAnimal();

            if (animal instanceof Predator) {
                return PREDATOR_COLOR;
            }

            if (animal instanceof Herbivore) {
                return HERBIVORE_COLOR;
            }

            return ANIMAL_COLOR;
        }

        if (tile.hasLivingPlant()) {
            return PLANT_COLOR;
        }

        if (tile.hasCorpse()) {
            return CORPSE_COLOR;
        }

        // if nothing living is on the tile, show the terrain color
        //switch case recommended by Kedaar, Jason learned syntax for it from a youtube tutorial and implemented it to 
        //change tile colors accordingly
        switch (tile.getTerrain()) {
            case GRASS:
                return GRASS_COLOR;
            case WATER:
                return WATER_COLOR;
            case ROCK:
                return ROCK_COLOR;
            default:
                return GRASS_COLOR;
        }
    }
}
