package home.tvsettings;

import android.app.AlertDialog;

import java.util.List;

/** The dock: which apps, in which order, and how many columns. */
public class DockActivity extends Screen {
    @Override protected String title() { return "Dock & layout"; }

    @Override
    protected void build() {
        setNote("The dock has one slot per column. OK on an app moves, replaces or removes it.");
        row("Columns", String.valueOf(store.columns()), v -> pickColumns());
        header("Dock, left to right");
        List<String> dock = store.dock();
        for (int i = 0; i < dock.size(); i++) {
            final int at = i;
            row((i + 1) + ".  " + Apps.label(this, dock.get(i)), null, v -> editSlot(at));
        }
        if (dock.size() < store.columns()) row("+  Add an app", null, v -> Apps.pick(this, "Add to the dock", app -> {
            List<String> d = store.dock();
            if (!d.contains(app.pkg)) d.add(app.pkg);
            save(d);
        }));
    }

    private void pickColumns() {
        String[] options = {"4", "5", "6", "7"};
        new AlertDialog.Builder(this).setTitle("Columns (dock slots)").setItems(options, (d, w) -> {
            store.setColumns(4 + w);
            List<String> dock = store.dock();
            while (dock.size() > store.columns()) dock.remove(dock.size() - 1);
            save(dock);
        }).show();
    }

    private void editSlot(int at) {
        String[] actions = {"Move left", "Move right", "Replace with…", "Remove"};
        new AlertDialog.Builder(this).setTitle(Apps.label(this, store.dock().get(at))).setItems(actions, (d, w) -> {
            List<String> dock = store.dock();
            switch (w) {
                case 0:
                    if (at > 0) { dock.add(at - 1, dock.remove(at)); save(dock); }
                    break;
                case 1:
                    if (at < dock.size() - 1) { dock.add(at + 1, dock.remove(at)); save(dock); }
                    break;
                case 2:
                    Apps.pick(this, "Replace with", app -> {
                        List<String> d2 = store.dock();
                        d2.set(at, app.pkg);
                        for (int i = d2.size() - 1; i >= 0; i--) if (i != at && d2.get(i).equals(app.pkg)) d2.remove(i);
                        save(d2);
                    });
                    break;
                default:
                    dock.remove(at);
                    save(dock);
            }
        }).show();
    }

    private void save(List<String> dock) {
        store.setDock(dock);
        store.save();
        refresh();
        applyQuietly();
    }
}
