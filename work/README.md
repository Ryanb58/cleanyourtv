# Sony BRAVIA 4K VH2: what was done (21 November 2026)

TV: Sony BRAVIA 4K VH2, firmware `BRAVIA_VH2_M_UC` (North America), Google TV 12 / API 31, build S158.
Based on the tested list in `examples/sony-bravia-4k-vh2/`. Everything was **disabled, not uninstalled**,
and every step can be undone.

## What changed

| Batch file | Disabled |
|---|---|
| `batch1-trackers.txt` | Samba TV (ACR: identifies what's on screen), Sony/Qterics device agents, Google Feedback, Sony usage agents |
| `batch2-promos.txt` | Demo/shop mode, Help tiles, Sony promos ("Freeplay"), Sony Select, Sony tab, partner auto-installs, Sony IoT |
| `batch3-apps.txt` | YouTube TV, YouTube Kids, Hulu, Max, Paramount+, Pluto TV, NFL, Google TV Movies, Sony Pictures Core, Play Games, Sony Music player, Hue Sync |
| `batch4-setupwraith.txt` | Google TV setup wizard (it takes over Home otherwise) |
| (by `launcher/build-and-install.sh`) | Google TV home screen, `com.google.android.apps.tv.launcherx` |

Kept on purpose: HDMI/tuner/Live TV, Bravia Sync, remote and Bluetooth, keyboard, Play Store/Services,
Assistant, Chromecast, AirPlay, firmware updates, and the apps you use.

Also: a saved Wi-Fi network owned by the setup wizard was forgotten, so disabling the wizard could
not drop the TV off Wi-Fi. The display had a 1920x1080 size override before we started; it was left as is.

Backups of the original state are in `backup/` (package lists, settings, getprop). `tools/disable.sh` also kept
recovery records in `~/.cleanyourtv/`: keep them, `restore.sh` uses them.

## Home Settings (the app that changes everything)

Open the **Home Settings** tile (slider icon) in the launcher's grid. Everything below is done there with the
remote, no computer needed. Your choices live in the app, in one place, and are laid over whichever theme is on:
a theme only decides the look.

| Row | What it does |
|---|---|
| **Theme** | All 32 themes from `themes/`, with previews. OK applies one; **Built-in** is the plain look. Your dock, buttons and options carry over. |
| **Dock & layout** | Columns (4 to 7 = dock slots), and the dock apps: OK on one moves it left/right, replaces or removes it; **+ Add an app**. |
| **Remote buttons** | **Settings button opens**: the TV's own menu, or the launcher's quick panel (TV settings, Display, Network, Apps, Launcher settings). **+ Add a button**, press the remote button (Disney+, for example), then pick what it does: open an app, switch to an HDMI input, Home, Back, mute, sleep, screensaver, TV settings, Wi-Fi settings or nothing, on a press or a hold. OK on a set button changes or removes it. |
| **Home options** | Clock screensaver after N minutes, date under the clock, Channel buttons do nothing on Home, greeting, green Wi-Fi icon, names under every tile, clock over the wallpaper, Volume fix, App library and its pill after the last row. Each one cycles Theme's choice → On → Off. |
| **Screensaver** | Every installed screensaver (Art Frame, Neon Dream, Koi Pond, the launcher's Clock, Google's Ambient Mode) or Off; **Starts after** 5 to 60 min; **Preview now**; and the chosen one's own settings. These are TV settings, not theme settings. |
| **Ad-blocking DNS** | On: the TV resolves names through AdGuard DNS (`dns.adguard-dns.com`), which blocks many ad and tracker domains. It does **not** block YouTube or Peacock ads (they are stitched into the video). If an app won't load, turn it off here first. |

Changes to the dock, buttons and options are saved at once; Home picks them up when you leave Home Settings.

How it works, for the curious (source: `work/tv-settings/`):

- It writes the launcher's `config.json` (the theme's, with your dock, columns, buttons as `app_keys` and options
  laid over it) and the theme's pictures through the launcher's own theme provider, the route `tools/theme.sh` uses.
- The launcher reads that file when Home is built, and Android won't let an app restart Home, so on leaving the app
  it opens Home and nudges the font scale by 1%, then puts the exact old value back: Android rebuilds Home for each
  change. The font scale was unset (1.0) before; it is written back as 1.0.
- Remote buttons are carried out by the launcher's own HomeKeyService (an accessibility helper). Home Settings
  switches it on while any button is set and off when none is; it also makes the remote's Apps button go Home.
  If holding volume stops ramping once a button is set, turn on **Home options › Volume fix**.
- "Add a button" switches on a tiny helper of the app's own (Home Settings button learning) that reads the one
  press, so it sees buttons the TV's firmware keeps from apps, then switches it off again. Arrows, OK, Back, Home,
  power and volume can't be remapped.
- Permissions, both granted once over ADB: `WRITE_SECURE_SETTINGS` (theme provider, DNS, the two helpers) and
  `WRITE_SETTINGS` (the font-scale nudge). No internet permission.
- Reinstalling the launcher switches its button helper off; opening Home Settings once puts it back
  (`work/build-and-install.sh` does that for you).

## Add-ons installed

- **Weather** (`addons/weather`), Neon Dream (`addons/neon-dream`) and Koi Pond (`addons/koi-pond`): from this repo,
  built with `tools/docker-build.sh addons/<name>` and installed with `pm install`. Neon Dream and Koi Pond are
  screensavers; pick them in Home Settings › Screensaver.
- **Art Frame** (`com.cgogoel.artframe`): third-party, from the `art-frame` branch of github.com/cgogoel/cleanyourtv.
  Its source was read before building (network only to the museums, NASA, Wikimedia and Unsplash over HTTPS; the phone
  upload page answers only the home network with a one-time code; no tracking). Kept out of git in `addons/art-frame/`
  (listed in `.git/info/exclude`). Installed without "display over other apps" (only its TCL-only Wake into art needs
  it) and without its install script's screensaver change. Three of its nine starter paintings are bundled: the Art
  Institute of Chicago's server refused the other six (HTTP 403); add art through its own search. The fork's
  launcher change (`system_wallpaper`) was not taken.

## Weather

The **Weather** card (from `addons/weather`) shows the weather for one city. Open it once and type the city; Fahrenheit
is under Weather settings at the bottom of its page. It reads Open-Meteo only while it is on screen.

## Turning network debugging back on (needed for everything below)

Debugging was switched off when we finished, for security. To run any script here:

1. TV: Settings > System > About > press OK on **Build** 7 times (only if Developer options vanished).
2. Settings > System > Developer options: switch on **USB debugging** and **Network debugging** (or **ADB debugging**).
3. Run the script; accept the "Allow USB debugging?" prompt on the TV.
4. Switch both off again afterwards, and check from the repo root: `tools/venv/bin/python tools/adb-closed.py <tv ip>`.

## Re-enabling an app

One app, from the repo root:

```bash
TV_HOST=<tv ip> tools/venv/bin/python tools/tv.py 'pm enable com.hulu.livingroomplus'
```

Package names are in the batch files above. A whole batch: `TV_HOST=<tv ip> work/restore.sh batch3`.
Everything, Google TV home included: `TV_HOST=<tv ip> work/restore.sh`.

## Rebuilding

After changing the launcher's code or `config.xml`, or Home Settings' code:

```bash
TV_HOST=<tv ip> work/build-and-install.sh            # both
TV_HOST=<tv ip> work/build-and-install.sh settings   # Home Settings only (also refreshes its themes from themes/)
```

Home Settings keeps your choices across a reinstall. The launcher's own Launcher settings (hold OK on the Settings
pill) and tile **Move** still work, but Home Settings is the place to change the dock: it rewrites the layout each
time it applies.

## Remote buttons on this TV

Home, Input, Settings, the Netflix/YouTube/Prime buttons, the mic and held volume all work (checked on the real
remote). The **Help** button does nothing (it opened Sony's help app, now disabled); give it a job in Home Settings ›
Remote buttons. The **Apps** button goes Home once any button is set there.
