package home.tvsettings;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Pick a theme; your dock, buttons and options are laid over it, then Home is rebuilt. */
public class ThemesActivity extends Activity {
    private final List<String> themes = new ArrayList<>();
    private TextView status;
    private GridView grid;
    private Store store;
    private boolean busy;

    static String label(String name) {
        if (name == null || name.isEmpty()) return "Built-in";
        String s = name.replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = Store.get(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(64), dp(36), dp(64), 0);
        TextView title = new TextView(this);
        title.setText("Theme");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        root.addView(title);
        status = new TextView(this);
        status.setText("Press OK on a theme to use it. Your dock, buttons and options stay as they are.");
        status.setTextColor(0xB3FFFFFF);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        status.setPadding(0, dp(6), 0, dp(16));
        root.addView(status);
        grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setHorizontalSpacing(dp(20));
        grid.setVerticalSpacing(dp(16));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(R.drawable.focus);
        grid.setDrawSelectorOnTop(true);
        grid.setClipToPadding(false);
        grid.setPadding(0, 0, 0, dp(40));
        root.addView(grid, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        themes.add("");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("themes/index.txt")))) {
            for (String line; (line = r.readLine()) != null; ) if (!line.trim().isEmpty()) themes.add(line.trim());
        } catch (IOException ignored) {
        }
        grid.setAdapter(new Cards());
        grid.setOnItemClickListener((p, v, position, id) -> apply(themes.get(position)));
        grid.setSelection(Math.max(0, themes.indexOf(store.theme())));
        grid.requestFocus();
    }

    private void apply(String name) {
        if (busy) return;
        busy = true;
        status.setText("Applying " + label(name) + "…");
        new Thread(() -> {
            String error = null;
            try {
                store.apply(name, true);
            } catch (SecurityException e) {
                error = "Home Settings has no permission to change the launcher (see the README).";
            } catch (Exception e) {
                error = "Could not apply the theme: " + e.getMessage();
            }
            String failure = error;
            runOnUiThread(() -> {
                busy = false;
                if (failure != null) { status.setText(failure); return; }
                finishAffinity();   // leaving the app rebuilds Home (App)
            });
        }).start();
    }

    private class Cards extends BaseAdapter {
        private final Bitmap[] previews = new Bitmap[themes.size()];

        @Override public int getCount() { return themes.size(); }
        @Override public Object getItem(int i) { return themes.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            LinearLayout card = (LinearLayout) view;
            if (card == null) {
                card = new LinearLayout(ThemesActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(6), dp(6), dp(6), dp(6));
                ImageView image = new ImageView(ThemesActivity.this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setBackgroundColor(0xFF1B1C20);
                card.addView(image, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(140)));
                TextView name = new TextView(ThemesActivity.this);
                name.setTextColor(Color.WHITE);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                name.setGravity(Gravity.CENTER_HORIZONTAL);
                name.setSingleLine(true);
                name.setPadding(0, dp(8), 0, dp(4));
                card.addView(name);
            }
            String theme = themes.get(i);
            ImageView image = (ImageView) card.getChildAt(0);
            if (previews[i] == null && !theme.isEmpty()) {
                try (InputStream in = getAssets().open("themes/" + theme + "/preview.jpg")) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inPreferredConfig = Bitmap.Config.RGB_565;
                    previews[i] = BitmapFactory.decodeStream(in, null, o);
                } catch (IOException ignored) {
                }
            }
            if (previews[i] != null) image.setImageBitmap(previews[i]);
            else image.setImageResource(R.drawable.builtin);
            boolean current = theme.equals(store.theme());
            ((TextView) card.getChildAt(1)).setText(current ? "✓  " + label(theme) : label(theme));
            return card;
        }
    }
}
