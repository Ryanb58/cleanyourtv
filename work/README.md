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

## Ad-blocking DNS

The **Ad-blocking DNS** tile (green shield) in the launcher's grid turns Private DNS on or off: OK on
"Turn off" / "Turn on". On, the TV resolves names through AdGuard DNS (`dns.adguard-dns.com`), which blocks
many ad and tracker domains. It does **not** block YouTube ads. If an app won't load, turn it off there first.
Its source is `work/dns-toggle/`; it has no internet permission, only the right to change that one setting.

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

## Changing the dock order or apps

Edit `dock_apps` (and `columns` for the number of slots) in
`launcher/app/src/main/res/values/config.xml`, then rebuild and install:

```bash
TV_HOST=<tv ip> work/build-and-install.sh launcher
```

Without a rebuild: on the TV, hold OK on a tile and pick **Move** to reorder it. Long-press the Settings pill
for Launcher settings (columns, motion, clock).

## Changing the theme or wallpaper

**On the TV, no computer needed:** open the **Themes** tile (purple palette) in the launcher's grid, pick a
theme with the remote and press OK. Home rebuilds with it in a second or two; your dock and 6 columns stay the
same in every theme. **Built-in** (first card) goes back to the plain look. All 32 themes from `themes/` are
inside the app (`work/theme-switcher/`, assets made by `prepare_assets.py`).

How it works: it writes the theme's files into the launcher through the launcher's own theme provider (the
same route `tools/theme.sh` uses), then nudges the font scale by 1% and puts it straight back, which makes
Android rebuild the Home screen so it reads the new theme. It holds two permissions, both granted once over ADB:
`WRITE_SECURE_SETTINGS` (the launcher's provider requires it) and `WRITE_SETTINGS` (the font-scale nudge).
No internet permission. The font scale was unset (1.0) before; it is written back as 1.0.

From the computer instead (needs network debugging on). See the themes with their screenshots in
`themes/README.md`, or `tools/venv/bin/python -I tools/manage.py list themes`.

```bash
TV_HOST=<tv ip> work/theme.sh aerial     # push a theme but keep your dock and 6 columns
TV_HOST=<tv ip> work/theme.sh --reset    # back to the built-in look
```

`work/theme.sh` copies the theme to `~/cleanyourtv-theme`, puts your dock back in it (many themes bring their own),
and pushes it with `tools/theme.sh`. For your own wallpaper: put a `wallpaper.jpg` (1920x1080) in that folder
and run `TV_HOST=<tv ip> tools/theme.sh ~/cleanyourtv-theme`.

## Remote buttons

Home, Input, Settings, the Netflix/YouTube/Prime buttons, the mic and held volume all work (checked on the real
remote). The **Apps** and **Help** buttons do nothing now: they opened the Google TV home and Sony's help app.
`docs/devices/sony-bravia-4k-vh2-remote-keys.md` describes a fix if you ever want them back.
