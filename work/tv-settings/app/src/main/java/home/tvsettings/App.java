package home.tvsettings;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/**
 * Home reads its settings when its screen is built, and Android does not let an app restart the Home process. So
 * once you leave Home Settings after a change, this opens Home and nudges the font scale by 1%, then puts the exact
 * old value back: Android rebuilds Home for each change (the launcher does not handle font-scale changes itself),
 * and the rebuild reads the new settings.
 */
public class App extends Application {
    private static boolean homeStale;
    private static int started;
    private static App app;
    private final Handler main = new Handler(Looper.getMainLooper());

    static void markHomeStale() { homeStale = true; }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity a) { started++; }
            @Override public void onActivityStopped(Activity a) {
                started--;
                // Left the app (Back from the main screen, the Home button, another app): rebuild Home now.
                if (started == 0 && homeStale) rebuildHome();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityResumed(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }

    /** Open Home and have Android rebuild it. */
    static void rebuildHome() {
        homeStale = false;
        App a = app;
        ContentResolver cr = a.getContentResolver();
        a.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        final float original = Settings.System.getFloat(cr, Settings.System.FONT_SCALE, 1f);
        a.main.postDelayed(() -> {
            try {
                Settings.System.putFloat(cr, Settings.System.FONT_SCALE, original + 0.01f);
            } catch (SecurityException e) {
                return;   // no WRITE_SETTINGS: the change shows at the next Home restart
            }
            a.main.postDelayed(() -> Settings.System.putFloat(cr, Settings.System.FONT_SCALE, original), 1500);
        }, 700);
    }
}
