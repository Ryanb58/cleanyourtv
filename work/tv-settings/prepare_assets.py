#!/usr/bin/env python3
"""Copy every theme in themes/ into Home Settings' assets, unchanged.

Run from the repo root: python3 work/tv-settings/prepare_assets.py
Each theme gets its config.json as the theme ships it, its wallpaper.jpg or wallpapers/ (images and
captions.txt) and a small preview.jpg made from screenshot.jpg. Your dock, columns, remote buttons and
options are NOT baked in: the app keeps them in its own settings and lays them over the theme when it
applies one. themes/default is skipped: "Built-in" in the app is the same.
"""
import os, re, shutil, subprocess

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
    d = os.path.join(out, name)
    os.makedirs(d)
    shutil.copy(os.path.join(t, "config.json"), d)
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
