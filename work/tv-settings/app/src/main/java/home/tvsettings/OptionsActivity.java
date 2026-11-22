package home.tvsettings;

import org.json.JSONObject;

/**
 * Launcher options, each laid over every theme. "Theme's choice" leaves it to the theme; On and Off are yours.
 * The keys are the launcher's own config.json keys (themes/README.md).
 */
public class OptionsActivity extends Screen {
    private static final String[][] SWITCHES = {
            {"date_below_clock", "Date under the clock"},
            {"home_ignore_channel_keys", "Channel buttons do nothing on Home"},
            {"show_greeting", "Greeting above the clock"},
            {"wifi_status_color", "Green Wi-Fi icon while online"},
            {"always_show_names", "Names under every tile"},
            {"clock_wallpaper", "Clock screensaver over the wallpaper"},
            {"volume_pauses_filter", "Volume fix for remote buttons"},
            {"app_library", "App library (every app; add or remove tiles)"},
            {"show_library_pill", "App library pill after the last row"},
    };
    private static final int[] IDLE = {-1, 0, 2, 5, 10, 30};

    @Override protected String title() { return "Home options"; }

    @Override
    protected void build() {
        setNote("Each option applies whatever theme is on. OK changes it.");
        JSONObject o = store.options();
        int idle = o.has("home_idle_clock_minutes") ? o.optInt("home_idle_clock_minutes") : -1;
        row("Clock screensaver after", idle < 0 ? "Theme's choice" : idle == 0 ? "Off" : idle + " min", v -> {
            int next = IDLE[0];
            for (int i = 0; i < IDLE.length; i++) if (IDLE[i] == idle) next = IDLE[(i + 1) % IDLE.length];
            set("home_idle_clock_minutes", next < 0 ? null : next);
        });
        for (String[] s : SWITCHES) {
            String key = s[0];
            String value = !o.has(key) ? "Theme's choice" : o.optBoolean(key) ? "On" : "Off";
            row(s[1], value, v -> {
                if (!o.has(key)) set(key, true);
                else if (o.optBoolean(key)) set(key, false);
                else set(key, null);
            });
        }
    }

    private void set(String key, Object value) {
        JSONObject o = store.options();
        if (value == null) o.remove(key);
        else store.put("options", putQuiet(o, key, value));
        store.save();
        refresh();
        applyQuietly();
    }

    private static JSONObject putQuiet(JSONObject o, String key, Object value) {
        try {
            o.put(key, value);
        } catch (Exception ignored) {
        }
        return o;
    }
}
