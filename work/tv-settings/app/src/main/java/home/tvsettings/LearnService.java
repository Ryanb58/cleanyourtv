package home.tvsettings;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Switched on only while Remote buttons waits for "press the button": it hands the next button's keycode and
 * scancode to the screen, swallows that one press, and Remote buttons switches it off again. Navigation keys,
 * Back, Home, power and volume are never taken, so the screen stays usable and nothing important can be remapped.
 */
public class LearnService extends AccessibilityService {
    interface Listener { void onButton(int keyCode, int scanCode); }

    static volatile Listener listener;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private int swallowUp = -1;

    static boolean learnable(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_HOME: case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP: case KeyEvent.KEYCODE_VOLUME_DOWN: case KeyEvent.KEYCODE_WAKEUP:
            case KeyEvent.KEYCODE_SLEEP:
                return false;
            default:
                return true;
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == swallowUp) {
            swallowUp = -1;
            return true;
        }
        Listener l = listener;
        if (l == null || event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0
                || !learnable(event.getKeyCode())) return false;
        listener = null;
        swallowUp = event.getKeyCode();
        int key = event.getKeyCode(), scan = event.getScanCode();
        MAIN.post(() -> l.onButton(key, scan));
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
}
