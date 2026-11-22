package home.tvsettings;

import android.app.AlertDialog;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote buttons: press a button, pick what it does. Saved as the launcher's app_keys; the launcher's HomeKeyService
 * does the work, and this screen switches it on while any button is set (off again when the list is empty).
 */
public class ButtonsActivity extends Screen {
    private static final String LEARNER = "home.tvsettings/.LearnService";
    private final Handler main = new Handler(Looper.getMainLooper());
    private AlertDialog waiting;

    @Override protected String title() { return "Remote buttons"; }

    @Override
    protected void build() {
        setNote("Give a remote button a new job. While any is set, the Settings button is taken too: choose what it opens.");
        boolean panel = store.options().optBoolean("settings_key_panel");
        row("Settings button opens", panel ? "Launcher quick panel" : "TV's own menu", v -> {
            JSONObject o = store.options();
            try {
                if (panel) o.remove("settings_key_panel"); else o.put("settings_key_panel", true);
            } catch (JSONException ignored) {
            }
            store.save();
            refresh();
            syncAndApply();
        });
        row("+  Add a button", null, v -> learn());
        JSONArray b = store.buttons();
        if (b.length() > 0) header("Your buttons");
        for (int i = 0; i < b.length(); i++) {
            JSONObject e = b.optJSONObject(i);
            if (e == null) continue;
            final int at = i;
            String when = e.optBoolean("hold") ? "hold" : "press";
            row(e.optString("name") + "  →  " + e.optString("targetName"), when, v -> edit(at));
        }
    }

    // ------------------------------------------------------------ learning a button

    private void learn() {
        try {
            Store.setService(this, LEARNER, true);
        } catch (SecurityException e) {
            setNote("Home Settings has no permission to read buttons (see the README).");
            return;
        }
        LearnService.listener = (key, scan) -> {
            stopLearning();
            chooseTarget(buttonName(key, scan), keySpec(key, scan), false, -1);
        };
        waiting = new AlertDialog.Builder(this)
                .setTitle("Press the remote button")
                .setMessage("Press the button you want to change, for example the Disney+ button.\n\n"
                        + "Arrows, OK, Back, Home, power and volume can't be changed. Back cancels.")
                .setOnCancelListener(d -> stopLearning())
                .show();
        main.postDelayed(() -> {
            if (waiting != null && waiting.isShowing()) { waiting.dismiss(); stopLearning(); setNote("No button press arrived."); }
        }, 20000);
    }

    private void stopLearning() {
        LearnService.listener = null;
        if (waiting != null && waiting.isShowing()) waiting.dismiss();
        waiting = null;
        try {
            Store.setService(this, LEARNER, false);
        } catch (SecurityException ignored) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (LearnService.listener != null) stopLearning();
    }

    /** A scancode tells apart Sony's app buttons, which share one keycode; use it when the remote sends one. */
    static String keySpec(int key, int scan) { return scan > 0 ? "scan:" + scan : String.valueOf(key); }

    static String buttonName(int key, int scan) {
        switch (scan) {   // Sony BRAVIA remote app buttons (all keycode 196)
            case 485: return "YouTube button";
            case 490: return "Prime Video button";
            case 491: return "Disney+ button";
            default: break;
        }
        String s = KeyEvent.keyCodeToString(key).replace("KEYCODE_", "").replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1) + " button" + (scan > 0 ? " (" + scan + ")" : "");
    }

    // ------------------------------------------------------------ what it does

    private static final String[] ACTION_NAMES = {"Open an app…", "Switch to an input…", "Go Home", "Back",
            "Mute / unmute", "Sleep (standby)", "Screensaver", "TV settings", "Wi-Fi settings", "Do nothing"};
    private static final String[] ACTIONS = {null, null, "home", "back", "mute", "sleep", "screensaver", "settings",
            "wifi", "ignore"};

    /** Pick the action, then press or hold, then save (replacing entry {@code at} when it is 0 or more). */
    private void chooseTarget(String name, String key, boolean hold, int at) {
        new AlertDialog.Builder(this).setTitle(name + " should…").setItems(ACTION_NAMES, (d, w) -> {
            if (w == 0) {
                Apps.pick(this, "Open which app?", app -> choosePressOrHold(name, key, app.pkg, app.label, at));
            } else if (w == 1) {
                pickInput(input -> choosePressOrHold(name, key, "input:" + input[0], input[1], at));
            } else {
                choosePressOrHold(name, key, ACTIONS[w], ACTION_NAMES[w], at);
            }
        }).show();
    }

    private void choosePressOrHold(String name, String key, String target, String targetName, int at) {
        String[] when = {"When pressed", "When held (half a second)"};
        new AlertDialog.Builder(this).setTitle("When?").setItems(when, (d, w) ->
                save(name, key, w == 1, target, targetName, at)).show();
    }

    private void pickInput(java.util.function.Consumer<String[]> done) {
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        List<String[]> inputs = new ArrayList<>();
        if (tim != null) for (TvInputInfo info : tim.getTvInputList()) {
            if (info.isHidden(this)) continue;
            inputs.add(new String[]{info.getId(), String.valueOf(info.loadLabel(this))});
        }
        if (inputs.isEmpty()) { setNote("The TV listed no inputs."); return; }
        String[] names = new String[inputs.size()];
        for (int i = 0; i < names.length; i++) names[i] = inputs.get(i)[1];
        new AlertDialog.Builder(this).setTitle("Switch to which input?")
                .setItems(names, (d, w) -> done.accept(inputs.get(w))).show();
    }

    private void save(String name, String key, boolean hold, String target, String targetName, int at) {
        JSONArray b = store.buttons();
        try {
            // One entry per button and press/hold: a new one replaces the old.
            for (int i = b.length() - 1; i >= 0; i--) {
                JSONObject e = b.optJSONObject(i);
                if (i == at || (e != null && e.optString("key").equals(key) && e.optBoolean("hold") == hold)) b.remove(i);
            }
            b.put(new JSONObject().put("name", name).put("key", key).put("hold", hold)
                    .put("target", target).put("targetName", targetName));
        } catch (JSONException ignored) {
        }
        store.save();
        refresh();
        syncAndApply();
    }

    private void edit(int at) {
        JSONObject e = store.buttons().optJSONObject(at);
        if (e == null) return;
        String[] actions = {"Change what it does", "Remove"};
        new AlertDialog.Builder(this).setTitle(e.optString("name")).setItems(actions, (d, w) -> {
            if (w == 0) {
                chooseTarget(e.optString("name"), e.optString("key"), e.optBoolean("hold"), at);
            } else {
                store.buttons().remove(at);
                store.save();
                refresh();
                syncAndApply();
            }
        }).show();
    }

    /** Write the buttons into the launcher, then reconnect its helper so it reads them. */
    private void syncAndApply() {
        new Thread(() -> {
            String note;
            try {
                store.apply(store.theme(), false);
                store.syncHomeHelper();
                note = store.wantsHomeHelper()
                        ? "Saved. Try it now. If holding volume stops ramping, turn on Home options › Volume fix."
                        : "Saved. Nothing for the button helper to do, so it is off and the remote is as before.";
            } catch (SecurityException ex) {
                note = "Home Settings has no permission to change the launcher (see the README).";
            } catch (Exception ex) {
                note = "Could not save: " + ex.getMessage();
            }
            String n = note;
            runOnUiThread(() -> setNote(n));
        }).start();
    }
}
