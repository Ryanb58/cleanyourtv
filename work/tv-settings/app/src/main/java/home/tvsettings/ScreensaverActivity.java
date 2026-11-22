package home.tvsettings;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.provider.Settings;
import android.service.dreams.DreamService;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The TV's screensaver: every installed one (Art Frame, Neon Dream, Koi Pond, the TV's own), a preview, how soon it
 * starts, and the chosen one's own settings. These are the TV's settings (screensaver_components, screen_off_timeout),
 * not the launcher's, so they do not depend on the theme.
 */
public class ScreensaverActivity extends Screen {
    private static final String SAVERS = "screensaver_components";
    private static final int[] MINUTES = {5, 10, 15, 30, 60};

    @Override protected String title() { return "Screensaver"; }

    static final class Saver {
        final ComponentName component;
        final String label;
        final ComponentName settings;
        Saver(ComponentName c, String l, ComponentName s) { component = c; label = l; settings = s; }
    }

    static List<Saver> list(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        List<Saver> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentServices(new Intent(DreamService.SERVICE_INTERFACE), PackageManager.GET_META_DATA)) {
            ServiceInfo si = r.serviceInfo;
            out.add(new Saver(new ComponentName(si.packageName, si.name), String.valueOf(r.loadLabel(pm)), settingsOf(pm, si)));
        }
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    /** The settings screen a screensaver names in its android.service.dream metadata, if any. */
    private static ComponentName settingsOf(PackageManager pm, ServiceInfo si) {
        try (XmlResourceParser p = si.loadXmlMetaData(pm, DreamService.DREAM_META_DATA)) {
            if (p == null) return null;
            for (int t = p.getEventType(); t != XmlPullParser.END_DOCUMENT; t = p.next()) {
                if (t != XmlPullParser.START_TAG) continue;
                String a = p.getAttributeValue("http://schemas.android.com/apk/res/android", "settingsActivity");
                if (a == null) return null;
                ComponentName c = ComponentName.unflattenFromString(a);
                return c != null ? c : new ComponentName(si.packageName, a.startsWith(".") ? si.packageName + a : a);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    static String current(Context ctx) {
        if (Settings.Secure.getInt(ctx.getContentResolver(), "screensaver_enabled", 1) == 0) return "";
        String s = Settings.Secure.getString(ctx.getContentResolver(), SAVERS);
        return s == null ? "" : s.split(",")[0];
    }

    static String currentName(Context ctx) {
        String cur = current(ctx);
        if (cur.isEmpty()) return "Off";
        ComponentName c = ComponentName.unflattenFromString(cur);
        for (Saver s : list(ctx)) if (s.component.equals(c)) return s.label;
        return "Off";
    }

    @Override
    protected void build() {
        setNote("Shown when the TV sits idle. OK on one makes it the screensaver.");
        int minutes = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 600000) / 60000;
        row("Starts after", minutes + " min", v -> {
            int next = MINUTES[0];
            for (int i = 0; i < MINUTES.length; i++) if (MINUTES[i] == minutes) next = MINUTES[(i + 1) % MINUTES.length];
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, next * 60000);
            } catch (SecurityException e) {
                setNote("No permission to change the time (see the README).");
            }
            refresh();
        });
        row("Preview now", null, v -> preview());
        String cur = current(this);
        ComponentName curName = ComponentName.unflattenFromString(cur);
        for (Saver s : list(this)) {
            if (s.component.equals(curName) && s.settings != null) {
                row(s.label + " settings", null, v -> open(s.settings));
            }
        }
        header("Choose");
        row(cur.isEmpty() ? "✓  Off" : "Off", "screen stays as it is", v -> choose(null));
        for (Saver s : list(this)) {
            boolean on = s.component.equals(curName);
            row(on ? "✓  " + s.label : s.label, null, v -> choose(s.component));
        }
    }

    private void choose(ComponentName c) {
        try {
            if (c == null) {
                Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 0);
            } else {
                Settings.Secure.putString(getContentResolver(), SAVERS, c.flattenToShortString());
                Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 1);
            }
            setNote(c == null ? "Screensaver off." : "Saved. Preview now shows it.");
        } catch (SecurityException e) {
            setNote("No permission to change the screensaver (see the README).");
        }
        refresh();
    }

    private void preview() {
        if (current(this).isEmpty()) { setNote("Choose a screensaver first."); return; }
        try {
            startActivity(new Intent().setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            setNote("This TV has no preview; it starts after the idle time.");
        }
    }

    private void open(ComponentName c) {
        try {
            startActivity(new Intent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            setNote("That screensaver's settings did not open.");
        }
    }
}
