// mainly created by Jason
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

// this class starts the whole program and creates the main swing window
public class Main {
    // these constants control how many rows and columns the ecosystem grid has
    private static final int WORLD_ROWS = 100;
    private static final int WORLD_COLS = 200;

    // this controls how large each tile looks on the screen
    // use 2 if you want a visible 1000 x 500 window
    // use 1 if performance is bad
    private static final int TILE_SIZE = 2;

    public static void main(String[] args) {
        // this makes sure swing creates the window on the correct gui thread
        // took syntax from website tutorial demonstrating basic Javaswing functions
        // https://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        // jframe is the main outside window that holds the entire simulation
        JFrame window = new JFrame("Ecosystem Simulation");

        // world is the custom panel that contains the model, grid, stats, and toolbar
        World world = new World(WORLD_ROWS, WORLD_COLS, TILE_SIZE);

        // these lines place the world inside the window and then show it on screen
        window.add(world);
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
