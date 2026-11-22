package home.tvsettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** The TV apps (those with a TV launcher entry), for the dock and the remote-button pickers. */
final class Apps {
    static final class App {
        final String pkg, label;
        App(String pkg, String label) { this.pkg = pkg; this.label = label; }
    }

    static List<App> list(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<App> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName()) || pkg.equals(Store.LAUNCHER)) continue;
            boolean dup = false;
            for (App a : out) if (a.pkg.equals(pkg)) dup = true;
            if (!dup) out.add(new App(pkg, String.valueOf(r.loadLabel(pm))));
        }
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    static String label(Context ctx, String pkg) {
        try {
            PackageManager pm = ctx.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    static void pick(Context ctx, String title, Consumer<App> done) {
        List<App> apps = list(ctx);
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).label;
        new AlertDialog.Builder(ctx).setTitle(title).setItems(names, (d, w) -> done.accept(apps.get(w))).show();
    }
}
