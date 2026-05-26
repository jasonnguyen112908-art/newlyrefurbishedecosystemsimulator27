import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// this class tells the gui when the model has changed
public class ChangeNotifier {
    // these listeners run when the whole model needs to update
    private final List<Runnable> changeListeners;

    // these listeners run when specific tiles need to update
    private final List<TileChangeListener> tileChangeListeners;

    // this set stores changed tile coordinates without duplicates
    private final Set<String> changedTileKeys;

    // batching lets the model collect many changes and send them all at once
    private boolean batching;
    private boolean fullChangePending;

    public ChangeNotifier() {
        this.changeListeners = new ArrayList<>();
        this.tileChangeListeners = new ArrayList<>();
        this.changedTileKeys = new LinkedHashSet<>();
        this.batching = false;
        this.fullChangePending = false;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void addTileChangeListener(TileChangeListener listener) {
        if (listener != null) {
            tileChangeListeners.add(listener);
        }
    }

    public void beginBatch() {
        // start collecting changes instead of sending them immediately
        batching = true;
    }

    public void endBatch() {
        // end the batch and send the collected changes
        batching = false;

        if (fullChangePending || !changedTileKeys.isEmpty()) {
            flushChanges();
        }
    }

    public void markFullChange() {
        // a full change means general gui information like stats should refresh
        fullChangePending = true;

        if (!batching) {
            flushChanges();
        }
    }

    public void markTileChanged(int row, int col) {
        // store the changed tile as text so it can be saved in a set
        changedTileKeys.add(createTileKey(row, col));

        if (!batching) {
            flushChanges();
        }
    }

    public void markTilesChanged(int firstRow, int firstCol, int secondRow, int secondCol) {
        markTileChanged(firstRow, firstCol);
        markTileChanged(secondRow, secondCol);
    }

    private void flushChanges() {
        // copy the changed tiles, then clear the pending list for the next update
        List<String> tileKeys = new ArrayList<>(changedTileKeys);
        changedTileKeys.clear();
        fullChangePending = false;

        // run every general listener, such as the stats panel update
        for (Runnable listener : changeListeners) {
            listener.run();
        }

        // convert each saved row,col key back into numbers and notify tile listeners
        for (String key : tileKeys) {
            int separatorIndex = key.indexOf(',');
            int row = Integer.parseInt(key.substring(0, separatorIndex));
            int col = Integer.parseInt(key.substring(separatorIndex + 1));

            for (TileChangeListener listener : tileChangeListeners) {
                listener.tileChanged(row, col);
            }
        }
    }

    private String createTileKey(int row, int col) {
        return row + "," + col;
    }
}
