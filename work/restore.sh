#!/usr/bin/env bash
# Put the Sony BRAVIA back the way it was before the cleanup (21 November 2026).
# Re-enables every package that was disabled, gives Home back to Google TV, and turns ad-blocking DNS off.
# Packages that were already off at the factory (homekit, b2b.vendorprotocol, b2b.softap, Disney+) stay off.
#
# Usage, from the repo root, with network debugging switched on again on the TV (see work/README.md):
#   TV_HOST=<tv ip> work/restore.sh            # everything
#   TV_HOST=<tv ip> work/restore.sh batch3     # just one batch, e.g. the streaming apps
set -euo pipefail
cd "$(dirname "$0")/.."
export TV_HOST="${TV_HOST:?set TV_HOST to the TV IP address}"
PY=tools/venv/bin/python
[ -x "$PY" ] || { python3 -m venv tools/venv && tools/venv/bin/pip install -q -r tools/requirements.txt; }
ONLY="${1:-}"

run() {  # $1 = batch file, $2 = optional stock launcher to give Home back to
  case "$1" in *"$ONLY"*) ;; *) return 0 ;; esac
  echo "== restoring $1"
  tools/restore.sh "work/$1" ${2:+"$2"}
}

# Newest first. Batch 4 (setup wizard) also re-enables the Google TV launcher and makes it Home again.
run batch4-setupwraith.txt com.google.android.apps.tv.launcherx
run batch3-apps.txt
run batch2-promos.txt
run batch1-trackers.txt

if [ -z "$ONLY" ] || [ "$ONLY" = dns ]; then
  echo "== ad-blocking DNS off"
  "$PY" tools/tv.py 'settings put global private_dns_mode opportunistic'
fi

echo
echo "Done. The new launcher (com.gathin.tvhome), Home Settings (home.tvsettings) and the Weather card"
echo "(io.github.sumyabazar1.weather) are still installed; remove them if you like, one per call:"
echo "  $PY tools/tv.py 'pm uninstall com.gathin.tvhome'"
echo "Remember to switch network debugging off again afterwards."
