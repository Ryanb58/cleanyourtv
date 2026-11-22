#!/usr/bin/env python3
"""Copy every theme in themes/ into the switcher's assets, with this TV's dock and 6 columns kept.

Run from the repo root: python3 work/theme-switcher/prepare_assets.py
Each theme gets config.json (merged), its wallpaper.jpg or wallpapers/ (images + captions.txt) and a
small preview.jpg made from screenshot.jpg. themes/default is skipped: "Built-in" in the app is the same.
"""
import json, os, re, shutil, subprocess, sys

DOCK = ["com.google.android.youtube.tv", "com.netflix.ninja", "com.peacocktv.peacockandroid",
        "com.apple.atve.sony.appletv", "com.ubnt.unifi.protect", "com.instantbits.cast.receiver"]
LIVE_TV = "com.sony.dtv.tvx"
IMAGE = re.compile(r"^[A-Za-z0-9_.-]+\.(jpg|jpeg|png|webp)$", re.I)

root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
src = os.path.join(root, "themes")
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app", "src", "main", "assets", "themes")
shutil.rmtree(out, ignore_errors=True)
os.makedirs(out)
names = []
for name in sorted(os.listdir(src)):
    t = os.path.join(src, name)
    if name == "default" or not os.path.isfile(os.path.join(t, "config.json")):
        continue
    with open(os.path.join(t, "config.json"), encoding="utf-8-sig") as f:
        cfg = json.load(f)
    cfg["dock_apps"] = DOCK
    cfg["columns"] = 6
    cfg["live_tv_package"] = LIVE_TV
    hidden = [p for p in cfg.get("hidden_apps", []) if p not in DOCK]
    if LIVE_TV not in hidden:
        hidden.append(LIVE_TV)
    cfg["hidden_apps"] = hidden
    d = os.path.join(out, name)
    os.makedirs(d)
    with open(os.path.join(d, "config.json"), "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)
    if os.path.isfile(os.path.join(t, "wallpaper.jpg")):
        shutil.copy(os.path.join(t, "wallpaper.jpg"), d)
    w = os.path.join(t, "wallpapers")
    if os.path.isdir(w):
        os.makedirs(os.path.join(d, "wallpapers"))
        for fn in sorted(os.listdir(w)):
            if IMAGE.match(fn) or fn == "captions.txt":
                shutil.copy(os.path.join(w, fn), os.path.join(d, "wallpapers", fn))
    shot = os.path.join(t, "screenshot.jpg")
    if os.path.isfile(shot):
        subprocess.run(["sips", "-Z", "480", "-s", "formatOptions", "70", shot, "--out",
                        os.path.join(d, "preview.jpg")], check=True, capture_output=True)
    names.append(name)
with open(os.path.join(out, "index.txt"), "w") as f:
    f.write("\n".join(names) + "\n")
print(f"{len(names)} themes -> {out}")
