package home.tvsettings;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

/** The menu: Theme, Dock & layout, Remote buttons, Home options, Ad-blocking DNS. */
public class MainActivity extends Screen {
    static final String RESOLVER = "dns.adguard-dns.com";

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        // One-time hand-over from the earlier Themes app: `am start ... --es set_theme ridgelines`.
        String set = getIntent().getStringExtra("set_theme");
        if (set != null) { store.put("theme", set); store.save(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        store.repairHomeHelper();
    }

    @Override protected String title() { return getString(R.string.app_name); }

    @Override
    protected void build() {
        setNote("Your settings stay the same whichever theme is on.");
        row("Theme", ThemesActivity.label(store.theme()), v -> startActivity(new Intent(this, ThemesActivity.class)));
        row("Dock & layout", store.dock().size() + " apps · " + store.columns() + " columns",
                v -> startActivity(new Intent(this, DockActivity.class)));
        int n = store.buttons().length();
        row("Remote buttons", n == 0 ? "None set" : n + " set", v -> startActivity(new Intent(this, ButtonsActivity.class)));
        row("Home options", null, v -> startActivity(new Intent(this, OptionsActivity.class)));
        row("Ad-blocking DNS", dnsOn() ? "On" : "Off", v -> { toggleDns(); refresh(); });
    }

    private boolean dnsOn() {
        return "hostname".equals(Settings.Global.getString(getContentResolver(), "private_dns_mode"))
                && RESOLVER.equals(Settings.Global.getString(getContentResolver(), "private_dns_specifier"));
    }

    private void toggleDns() {
        try {
            if (dnsOn()) {
                Settings.Global.putString(getContentResolver(), "private_dns_mode", "opportunistic");
                setNote("Ad-blocking DNS is off: the TV uses your network's own DNS.");
            } else {
                Settings.Global.putString(getContentResolver(), "private_dns_specifier", RESOLVER);
                Settings.Global.putString(getContentResolver(), "private_dns_mode", "hostname");
                setNote("Ad-blocking DNS is on (AdGuard). It does not block YouTube or Peacock ads.");
            }
        } catch (SecurityException e) {
            setNote("No permission to change DNS (see the README).");
        }
    }
}
