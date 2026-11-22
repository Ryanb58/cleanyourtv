package home.tvsettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Your own settings for the Clean Your TV launcher, kept in one file (personal.json) and laid over whichever theme
 * is on: a theme decides the look, this decides the dock, the columns, the remote buttons and the options you set.
 *
 * Applying writes the merged config.json (and, when the theme changes, its pictures) into the launcher through its
 * ThemeProvider, the same route tools/theme.sh uses.
 */
final class Store {
    static final String LAUNCHER = "com.gathin.tvhome";
    static final String HOME_HELPER = LAUNCHER + "/.HomeKeyService";
    private static final String BASE = "content://" + LAUNCHER + ".theme/";
    private static final String FILE = "personal.json";

    /** Keys this app owns. A theme's own value for any of them is replaced by yours. */
    static final String DOCK = "dock_apps", COLUMNS = "columns", HIDDEN = "hidden_apps", LIVE_TV = "live_tv_package";

    private static Store instance;
    private final Context ctx;
    JSONObject data;

    private Store(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        data = read();
    }

    static synchronized Store get(Context ctx) {
        if (instance == null) instance = new Store(ctx);
        return instance;
    }

    // ---------------------------------------------------------------- the personal file

    private JSONObject read() {
        File f = new File(ctx.getFilesDir(), FILE);
        if (f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                return new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
            } catch (IOException | JSONException ignored) {
                // fall through to fresh defaults
            }
        }
        return importFromLauncher();
    }

    void save() {
        File f = new File(ctx.getFilesDir(), FILE);
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) {
        }
    }

    /**
     * First run: start from what the launcher already shows. The dock, columns, hidden apps and Live TV app come from
     * the config.json on the TV when there is one, otherwise from the launcher's built-in config.xml.
     */
    private JSONObject importFromLauncher() {
        JSONObject d = new JSONObject();
        try {
            JSONObject current = readLauncherConfig();
            Resources res = ctx.getPackageManager().getResourcesForApplication(LAUNCHER);
            d.put(DOCK, current != null && current.has(DOCK) ? current.getJSONArray(DOCK)
                    : new JSONArray(stringArray(res, "dock_apps")));
            d.put(COLUMNS, current != null && current.has(COLUMNS) ? current.getInt(COLUMNS)
                    : res.getInteger(res.getIdentifier("columns", "integer", LAUNCHER)));
            d.put(HIDDEN, current != null && current.has(HIDDEN) ? current.getJSONArray(HIDDEN)
                    : new JSONArray(stringArray(res, "hidden_apps")));
            d.put(LIVE_TV, current != null && current.has(LIVE_TV) ? current.getString(LIVE_TV)
                    : res.getString(res.getIdentifier("live_tv_package", "string", LAUNCHER)));
        } catch (Exception ignored) {
            // the launcher is not installed or not readable: start empty
        }
        try {
            d.put("theme", "");
            d.put("buttons", new JSONArray());
            d.put("options", new JSONObject());
        } catch (JSONException ignored) {
        }
        return d;
    }

    private static List<String> stringArray(Resources res, String name) {
        int id = res.getIdentifier(name, "array", LAUNCHER);
        List<String> out = new ArrayList<>();
        if (id != 0) for (String s : res.getStringArray(id)) out.add(s);
        return out;
    }

    private JSONObject readLauncherConfig() {
        try (InputStream in = ctx.getContentResolver().openInputStream(Uri.parse(BASE + "config.json"))) {
            if (in == null) return null;
            return new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- accessors

    String theme() { return data.optString("theme", ""); }

    List<String> dock() {
        List<String> out = new ArrayList<>();
        JSONArray a = data.optJSONArray(DOCK);
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        return out;
    }

    void setDock(List<String> dock) { put(DOCK, new JSONArray(dock)); }

    int columns() { return data.optInt(COLUMNS, 6); }

    void setColumns(int n) { put(COLUMNS, n); }

    JSONArray buttons() {
        JSONArray a = data.optJSONArray("buttons");
        if (a == null) { a = new JSONArray(); put("buttons", a); }
        return a;
    }

    JSONObject options() {
        JSONObject o = data.optJSONObject("options");
        if (o == null) { o = new JSONObject(); put("options", o); }
        return o;
    }

    void put(String key, Object value) {
        try {
            data.put(key, value);
        } catch (JSONException ignored) {
        }
    }

    /** The app_keys list the launcher reads, built from the buttons you set. */
    JSONArray appKeys() {
        JSONArray out = new JSONArray();
        JSONArray b = buttons();
        for (int i = 0; i < b.length(); i++) {
            JSONObject e = b.optJSONObject(i);
            if (e == null) continue;
            out.put((e.optBoolean("hold") ? "hold:" : "") + e.optString("key") + ":" + e.optString("target"));
        }
        return out;
    }

    // ---------------------------------------------------------------- applying

    /**
     * Write the theme (its config.json with your settings laid over it) into the launcher. With {@code pictures}
     * the theme's wallpapers are replaced too; without, only config.json changes (a dock or option change).
     */
    void apply(String theme, boolean pictures) throws IOException, JSONException {
        AssetManager am = ctx.getAssets();
        JSONObject config = theme.isEmpty() ? new JSONObject()
                : new JSONObject(new String(readAll(am.open("themes/" + theme + "/config.json")), StandardCharsets.UTF_8));
        // Yours over the theme's.
        config.put(DOCK, data.optJSONArray(DOCK) != null ? data.optJSONArray(DOCK) : new JSONArray());
        config.put(COLUMNS, columns());
        JSONArray hidden = data.optJSONArray(HIDDEN);
        if (hidden != null) config.put(HIDDEN, hidden);
        String live = data.optString(LIVE_TV, "");
        if (!live.isEmpty()) config.put(LIVE_TV, live);
        JSONArray keys = appKeys();
        if (keys.length() > 0) config.put("app_keys", keys);
        JSONObject options = options();
        for (Iterator<String> it = options.keys(); it.hasNext(); ) {
            String k = it.next();
            config.put(k, options.get(k));
        }

        ContentResolver cr = ctx.getContentResolver();
        if (pictures) {
            cr.delete(Uri.parse(BASE + "wallpaper.jpg"), null, null);
            cr.delete(Uri.parse(BASE + "wallpapers/captions.txt"), null, null);
            cr.delete(Uri.parse(BASE + "wallpapers"), null, null);
            if (!theme.isEmpty()) {
                String dir = "themes/" + theme;
                String[] top = am.list(dir);
                if (top != null) for (String f : top) if (f.equals("wallpaper.jpg")) copyAsset(dir + "/" + f, f);
                String[] walls = am.list(dir + "/wallpapers");
                if (walls != null) for (String f : walls) copyAsset(dir + "/wallpapers/" + f, "wallpapers/" + f);
            }
        }
        cr.delete(Uri.parse(BASE + "config.json"), null, null);
        writeBytes("config.json", config.toString(2).getBytes(StandardCharsets.UTF_8));
        writeBytes("theme_revision", UUID.randomUUID().toString().getBytes(StandardCharsets.US_ASCII));
        put("theme", theme);
        save();
        App.markHomeStale();
    }

    private void copyAsset(String asset, String path) throws IOException {
        try (InputStream in = ctx.getAssets().open(asset)) {
            writeStream(path, in);
        }
    }

    private void writeBytes(String path, byte[] bytes) throws IOException {
        try (OutputStream out = ctx.getContentResolver().openOutputStream(Uri.parse(BASE + path), "wt")) {
            if (out == null) throw new IOException("the launcher refused " + path);
            out.write(bytes);
        }
    }

    private void writeStream(String path, InputStream in) throws IOException {
        try (OutputStream out = ctx.getContentResolver().openOutputStream(Uri.parse(BASE + path), "wt")) {
            if (out == null) throw new IOException("the launcher refused " + path);
            byte[] buf = new byte[64 * 1024];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- accessibility helpers

    static boolean serviceOn(Context ctx, String component) {
        String s = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && (":" + s + ":").contains(":" + component + ":");
    }

    /** Add or remove one service in the accessibility list, leaving the others as they are. */
    static void setService(Context ctx, String component, boolean on) {
        ContentResolver cr = ctx.getContentResolver();
        String s = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        List<String> list = new ArrayList<>();
        if (s != null) for (String part : s.split(":")) if (!part.isEmpty() && !part.equals(component)) list.add(part);
        if (on) list.add(component);
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, String.join(":", list));
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, list.isEmpty() ? 0 : 1);
    }

    /**
     * The launcher's HomeKeyService does the remote-button work. It reads the buttons when it connects, so after a
     * change it is taken off the list and put back; with nothing for it to do it is switched off, so nothing filters keys.
     */
    /** The helper is needed while any button is set, or while the Settings button opens the launcher's panel. */
    boolean wantsHomeHelper() {
        return buttons().length() > 0 || options().optBoolean("settings_key_panel");
    }

    void syncHomeHelper() {
        boolean want = wantsHomeHelper();
        if (serviceOn(ctx, HOME_HELPER)) setService(ctx, HOME_HELPER, false);
        if (want) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException ignored) {
            }
            setService(ctx, HOME_HELPER, true);
        }
    }

    /** Android takes the helper off the list when the launcher is reinstalled: put it back if buttons are set. */
    void repairHomeHelper() {
        if (wantsHomeHelper() && !serviceOn(ctx, HOME_HELPER)) {
            try {
                setService(ctx, HOME_HELPER, true);
            } catch (SecurityException ignored) {
            }
        }
    }
}
