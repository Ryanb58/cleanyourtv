#!/usr/bin/env bash
# Push one of the repo's themes, but keep YOUR dock (YouTube, Netflix, Peacock, Apple TV, Prime Video,
# Web Video Caster) and 6 columns: many themes ship their own dock_apps/columns, which would replace yours.
# The merged copy is written to ~/cleanyourtv-theme (outside the repo) and pushed with tools/theme.sh.
#
# Usage, from the repo root, with network debugging on:
#   TV_HOST=<tv ip> work/theme.sh aurora        # any folder name under themes/
#   TV_HOST=<tv ip> work/theme.sh --reset       # back to the built-in look
#   tools/venv/bin/python -I tools/manage.py list themes   # what's available
set -euo pipefail
cd "$(dirname "$0")/.."
export TV_HOST="${TV_HOST:?set TV_HOST to the TV IP address}"
NAME="${1:?usage: work/theme.sh <theme name> | --reset}"
if [ "$NAME" = --reset ]; then exec tools/theme.sh --reset; fi
SRC="themes/$NAME"
[ -f "$SRC/config.json" ] || { echo "no such theme: $SRC (see: ls themes)" >&2; exit 2; }
DST="$HOME/cleanyourtv-theme"
rm -rf "$DST"; cp -R "$SRC" "$DST"
python3 - "$DST/config.json" <<'EOF'
import json, sys
p = sys.argv[1]
c = json.load(open(p))
c["dock_apps"] = ["com.google.android.youtube.tv", "com.netflix.ninja", "com.peacocktv.peacockandroid",
                  "com.apple.atve.sony.appletv", "com.amazon.amazonvideo.livingroom",
                  "com.instantbits.cast.receiver"]
c["columns"] = 6
hidden = c.get("hidden_apps", [])
if "com.sony.dtv.tvx" not in hidden:
    hidden.append("com.sony.dtv.tvx")
c["hidden_apps"] = hidden
c["live_tv_package"] = "com.sony.dtv.tvx"
json.dump(c, open(p, "w"), indent=2, ensure_ascii=False)
EOF
tools/theme.sh "$DST"
