// mainly Jason with help of Kedaar and youtube tutorials and website tutorial
// https://www.youtube.com/watch?v=NWhkCZZumAE
// https://www.tutorialspoint.com/swing/swing_jbutton.htm
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

// this panel creates the buttons that control the simulation
public class ToolbarPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;

    // this controls how fast the simulation runs when start is pressed
    private static final int SIMULATION_DELAY_MS = 90;

    // this controls how much the sun changes when the sun buttons are pressed
    private static final double SUN_STEP = 0.1;

    private final WorldModel model;

    // this timer repeatedly calls updateWorld while the simulation is running
    private final Timer simulationTimer;

    private boolean simulationRunning;

    public ToolbarPanel(WorldModel model) {
        this.model = model;
        this.simulationTimer = new Timer(SIMULATION_DELAY_MS, e -> model.updateWorld());
        this.simulationRunning = false;

        createButtons();
    }

    private void createButtons() {
        // buttongroup makes sure only one editing tool is selected at a time
        ButtonGroup toolGroup = new ButtonGroup();

        // these toggle buttons choose what left-clicking the grid will do
        addToolButton(toolGroup, "Grass", ToolType.GRASS, true);
        addToolButton(toolGroup, "Water", ToolType.WATER, false);
        addToolButton(toolGroup, "Rock", ToolType.ROCK, false);
        addToolButton(toolGroup, "Plant", ToolType.PLANT, false);
        addToolButton(toolGroup, "Herbivore", ToolType.HERBIVORE, false);
        addToolButton(toolGroup, "Predator", ToolType.PREDATOR, false);
        addToolButton(toolGroup, "Erase Plant", ToolType.ERASE_PLANT, false);
        addToolButton(toolGroup, "Erase Animal", ToolType.ERASE_ANIMAL, false);

        // step runs exactly one simulation tick
        JButton stepButton = new JButton("Step");
        // syntaxes below that use e -> are implemented by Kedaar
        stepButton.addActionListener(e -> model.updateWorld());

        // start and pause control the swing timer
        JButton startPauseButton = new JButton("Start");
        startPauseButton.addActionListener(e -> toggleSimulation(startPauseButton));

        // these buttons change the sun value in the model
        JButton lessSunButton = new JButton("Less Sun");
        lessSunButton.addActionListener(e -> model.setSunValue(model.getSunValue() - SUN_STEP));

        JButton moreSunButton = new JButton("More Sun");
        moreSunButton.addActionListener(e -> model.setSunValue(model.getSunValue() + SUN_STEP));

        add(stepButton);
        add(startPauseButton);
        add(lessSunButton);
        add(moreSunButton);
    }

    private void addToolButton(ButtonGroup group, String label, ToolType tool, boolean selected) {
        JToggleButton button = new JToggleButton(label);

        // when this button is clicked, the selected tool in the model changes
        button.setSelected(selected);
        button.addActionListener(e -> model.setSelectedTool(tool));

        group.add(button);
        add(button);
    }

    private void toggleSimulation(JButton startPauseButton) {
        simulationRunning = !simulationRunning;

        // starting the timer makes updateWorld run repeatedly
        if (simulationRunning) {
            simulationTimer.start();
            startPauseButton.setText("Pause");
        } else {
            simulationTimer.stop();
            startPauseButton.setText("Start");
        }
    }
}
