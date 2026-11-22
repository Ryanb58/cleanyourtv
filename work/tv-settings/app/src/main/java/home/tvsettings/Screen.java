package home.tvsettings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A plain TV list screen: a title, an optional line under it, and focusable rows built in {@link #build()}. */
public abstract class Screen extends Activity {
    protected Store store;
    private LinearLayout list;
    private TextView note;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = Store.get(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(64), dp(36), dp(64), 0);
        TextView title = text(title(), 34, Color.WHITE);
        root.addView(title);
        note = text("", 15, 0xB3FFFFFF);
        note.setPadding(0, dp(6), 0, dp(16));
        root.addView(note);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(40));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(dp(820), LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    protected abstract String title();

    protected abstract void build();

    /** Rebuild the rows, keeping focus on the same row index. */
    protected void refresh() {
        View f = getCurrentFocus();
        int at = f != null ? list.indexOfChild(f) : 0;
        list.removeAllViews();
        build();
        View target = list.getChildAt(Math.max(0, Math.min(at, list.getChildCount() - 1)));
        if (target != null && !target.isFocusable()) target = firstFocusable();
        if (target != null) target.requestFocus();
    }

    private View firstFocusable() {
        for (int i = 0; i < list.getChildCount(); i++) if (list.getChildAt(i).isFocusable()) return list.getChildAt(i);
        return null;
    }

    protected void setNote(String s) { note.setText(s); }

    protected void header(String s) {
        TextView t = text(s.toUpperCase(), 13, 0x8CFFFFFF);
        t.setLetterSpacing(0.15f);
        t.setPadding(dp(4), dp(18), 0, dp(8));
        list.addView(t);
    }

    /** A row: a name on the left, its value on the right; OK runs {@code onClick}. */
    protected View row(String name, String value, View.OnClickListener onClick) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackgroundResource(R.drawable.row);
        r.setFocusable(true);
        r.setPadding(dp(18), 0, dp(18), 0);
        TextView n = text(name, 17, 0);
        n.setTextColor(getColorStateList(R.color.row_text));
        n.setDuplicateParentStateEnabled(true);
        r.addView(n, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        if (value != null) {
            TextView v = text(value, 15, 0);
            v.setTextColor(getColorStateList(R.color.row_text));
            v.setAlpha(0.75f);
            v.setDuplicateParentStateEnabled(true);
            r.addView(v);
        }
        r.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.bottomMargin = dp(8);
        list.addView(r, lp);
        return r;
    }

    protected TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (color != 0) t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setSingleLine(true);
        return t;
    }

    protected int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** Apply your settings over the current theme (config.json only) after a change on this screen. */
    protected void applyQuietly() {
        new Thread(() -> {
            String error = null;
            try {
                store.apply(store.theme(), false);
            } catch (SecurityException e) {
                error = "Home Settings has no permission to change the launcher (see the README).";
            } catch (Exception e) {
                error = "Could not save: " + e.getMessage();
            }
            String failure = error;
            runOnUiThread(() -> setNote(failure != null ? failure : "Saved. Home updates when you leave Home Settings."));
        }).start();
    }
}
