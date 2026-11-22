#!/usr/bin/env bash
# Rebuild and reinstall this TV's launcher and the Home Settings app (themes, dock, remote buttons, options,
# ad-blocking DNS). Builds in Docker (OrbStack), so no JDK or Android SDK is needed.
#
# Usage, from the repo root, with network debugging switched on on the TV:
#   TV_HOST=<tv ip> work/build-and-install.sh            # launcher + Home Settings
#   TV_HOST=<tv ip> work/build-and-install.sh launcher   # launcher only
#   TV_HOST=<tv ip> work/build-and-install.sh settings   # Home Settings only
set -euo pipefail
cd "$(dirname "$0")/.."
export TV_HOST="${TV_HOST:?set TV_HOST to the TV IP address}"
WHAT="${1:-all}"
PY=tools/venv/bin/python
[ -x "$PY" ] || { python3 -m venv tools/venv && tools/venv/bin/pip install -q -r tools/requirements.txt; }
docker info >/dev/null 2>&1 || { open -a OrbStack; echo "starting OrbStack..."; for _ in $(seq 30); do docker info >/dev/null 2>&1 && break; python3 -c 'import time; time.sleep(2)'; done; }

if [ "$WHAT" != settings ]; then
  echo "== building the launcher"
  tools/docker-build.sh launcher
  # The repo script installs it, makes it Home, and keeps the Home guard (it never leaves the TV without a Home).
  SKIP_BUILD=1 launcher/build-and-install.sh
fi

if [ "$WHAT" != launcher ]; then
  echo "== building Home Settings (every theme in themes/ is bundled)"
  python3 work/tv-settings/prepare_assets.py
  tools/docker-build.sh work/tv-settings
  "$PY" tools/b64push.py work/tv-settings/app/build/outputs/apk/release/app-release.apk /data/local/tmp/hs.apk
  "$PY" tools/tv.py 'pm install -r /data/local/tmp/hs.apk'
  "$PY" tools/tv.py 'rm /data/local/tmp/hs.apk'
  "$PY" tools/tv.py 'pm grant home.tvsettings android.permission.WRITE_SECURE_SETTINGS'
  "$PY" tools/tv.py 'appops set home.tvsettings WRITE_SETTINGS allow'
fi

# Android switches the launcher's remote-button helper off when the launcher is reinstalled. Opening Home Settings
# puts it back (when any remote button is set), then Home comes back to the front.
"$PY" tools/tv.py 'am start -n home.tvsettings/.MainActivity' >/dev/null 2>&1 || true
python3 -c 'import time; time.sleep(3)'
"$PY" tools/tv.py 'input keyevent HOME'
echo "Installed."
