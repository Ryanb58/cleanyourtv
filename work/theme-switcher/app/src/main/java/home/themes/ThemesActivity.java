package home.themes;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pick a theme with the remote. The themes are bundled in assets/themes/<name>/; applying one writes its files
 * into the Clean Your TV launcher through its ThemeProvider (as tools/theme.sh does), then restarts Home.
 */
public class ThemesActivity extends Activity {
    static final String LAUNCHER = "com.gathin.tvhome";
    static final String BASE = "content://" + LAUNCHER + ".theme/";
    static final String BUILT_IN = "";   // the row that removes the theme: back to the launcher's config.xml

    private final List<String> themes = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView status;
    private GridView grid;
    private boolean busy;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_themes);
        prefs = getSharedPreferences("themes", MODE_PRIVATE);
        status = findViewById(R.id.status);
        grid = findViewById(R.id.grid);
        themes.add(BUILT_IN);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("themes/index.txt")))) {
            for (String line; (line = r.readLine()) != null; ) if (!line.trim().isEmpty()) themes.add(line.trim());
        } catch (IOException ignored) {
        }
        grid.setAdapter(new Cards());
        grid.setOnItemClickListener((parent, view, position, id) -> apply(themes.get(position)));
        String current = prefs.getString("current", BUILT_IN);
        int at = Math.max(0, themes.indexOf(current));
        grid.setSelection(at);
        grid.requestFocus();
    }

    static String label(String name) {
        if (name.isEmpty()) return "Built-in";
        String s = name.replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void apply(String name) {
        if (busy) return;
        busy = true;
        status.setText(getString(R.string.applying, label(name)));
        new Thread(() -> {
            String error = null;
            try {
                write(name);
            } catch (SecurityException e) {
                error = getString(R.string.no_permission);
            } catch (Exception e) {
                error = getString(R.string.failed, String.valueOf(e.getMessage()));
            }
            String failure = error;
            main.post(() -> {
                busy = false;
                if (failure != null) {
                    status.setText(failure);
                    return;
                }
                prefs.edit().putString("current", name).apply();
                ((BaseAdapter) grid.getAdapter()).notifyDataSetChanged();
                restartHome();
            });
        }).start();
    }

    /** The same steps as tools/theme.sh: clear the old theme's files, write the new ones, bump the revision. */
    private void write(String name) throws IOException {
        ContentResolver cr = getContentResolver();
        AssetManager am = getAssets();
        cr.delete(Uri.parse(BASE + "config.json"), null, null);
        cr.delete(Uri.parse(BASE + "wallpaper.jpg"), null, null);
        cr.delete(Uri.parse(BASE + "wallpapers/captions.txt"), null, null);
        cr.delete(Uri.parse(BASE + "wallpapers"), null, null);
        if (!name.isEmpty()) {
            String dir = "themes/" + name;
            String[] top = am.list(dir);
            if (top != null) for (String f : top) {
                if (f.equals("config.json") || f.equals("wallpaper.jpg")) copy(am, dir + "/" + f, f);
            }
            String[] walls = am.list(dir + "/wallpapers");
            if (walls != null) for (String f : walls) copy(am, dir + "/wallpapers/" + f, "wallpapers/" + f);
        }
        try (OutputStream o = cr.openOutputStream(Uri.parse(BASE + "theme_revision"), "wt")) {
            o.write(UUID.randomUUID().toString().getBytes("US-ASCII"));
        }
    }

    private void copy(AssetManager am, String asset, String path) throws IOException {
        try (InputStream in = am.open(asset);
             OutputStream out = getContentResolver().openOutputStream(Uri.parse(BASE + path), "wt")) {
            if (out == null) throw new IOException("launcher refused " + path);
            byte[] buf = new byte[64 * 1024];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        }
    }

    /**
     * Home reads its theme when its screen is built. Android does not let an app end the Home process, so open
     * Home, then nudge the font scale by 1% and put the exact old value back: each change makes the system
     * rebuild Home (it does not handle font-scale changes itself), and the rebuild reads the new theme.
     */
    private void restartHome() {
        ContentResolver cr = getContentResolver();
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        final float original = Settings.System.getFloat(cr, Settings.System.FONT_SCALE, 1f);
        main.postDelayed(() -> {
            try {
                Settings.System.putFloat(cr, Settings.System.FONT_SCALE, original + 0.01f);
            } catch (SecurityException e) {
                return;   // no WRITE_SETTINGS: the theme shows at the next Home restart
            }
            main.postDelayed(() -> {
                Settings.System.putFloat(cr, Settings.System.FONT_SCALE, original);
                finish();
            }, 1500);
        }, 700);
    }

    private class Cards extends BaseAdapter {
        private final Bitmap[] previews = new Bitmap[themes.size()];

        @Override public int getCount() { return themes.size(); }
        @Override public Object getItem(int i) { return themes.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            if (view == null) view = LayoutInflater.from(ThemesActivity.this).inflate(R.layout.card, parent, false);
            String name = themes.get(i);
            ImageView image = view.findViewById(R.id.preview);
            if (previews[i] == null && !name.isEmpty()) {
                try (InputStream in = getAssets().open("themes/" + name + "/preview.jpg")) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inPreferredConfig = Bitmap.Config.RGB_565;
                    previews[i] = BitmapFactory.decodeStream(in, null, o);
                } catch (IOException ignored) {
                }
            }
            if (previews[i] != null) image.setImageBitmap(previews[i]);
            else image.setImageResource(R.drawable.builtin);
            boolean current = name.equals(prefs.getString("current", BUILT_IN));
            ((TextView) view.findViewById(R.id.name)).setText(current ? "✓  " + label(name) : label(name));
            return view;
        }
    }
}
