package home.dnstoggle;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

/**
 * One switch for Private DNS: On points the TV at an ad-blocking resolver, Off goes back to Android's
 * default (opportunistic, i.e. the network's own DNS). Needs WRITE_SECURE_SETTINGS, granted over ADB.
 */
public class ToggleActivity extends Activity {
    static final String RESOLVER = "dns.adguard-dns.com";
    static final String MODE = "private_dns_mode";
    static final String SPECIFIER = "private_dns_specifier";

    private TextView state, note, toggle;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_toggle);
        state = findViewById(R.id.state);
        note = findViewById(R.id.note);
        toggle = findViewById(R.id.toggle);
        toggle.setOnClickListener(v -> flip());
        findViewById(R.id.close).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        toggle.requestFocus();
    }

    private boolean isOn() {
        String mode = Settings.Global.getString(getContentResolver(), MODE);
        String host = Settings.Global.getString(getContentResolver(), SPECIFIER);
        return "hostname".equals(mode) && RESOLVER.equals(host);
    }

    private void flip() {
        boolean turnOn = !isOn();
        try {
            if (turnOn) {
                Settings.Global.putString(getContentResolver(), SPECIFIER, RESOLVER);
                Settings.Global.putString(getContentResolver(), MODE, "hostname");
            } else {
                Settings.Global.putString(getContentResolver(), MODE, "opportunistic");
            }
            note.setVisibility(View.GONE);
        } catch (SecurityException e) {
            note.setText(R.string.no_permission);
            note.setVisibility(View.VISIBLE);
        }
        render();
    }

    private void render() {
        boolean on = isOn();
        state.setText(on ? R.string.state_on : R.string.state_off);
        toggle.setText(on ? R.string.turn_off : R.string.turn_on);
    }
}
