#!/usr/bin/env bash
# Rebuild and reinstall this TV's launcher (plus the Ad-blocking DNS switch and the Themes app) after changing
# launcher/app/src/main/res/values/config.xml. Builds in Docker (OrbStack), so no JDK or Android SDK is needed.
#
# Usage, from the repo root, with network debugging switched on on the TV:
#   TV_HOST=<tv ip> work/build-and-install.sh          # launcher + DNS switch + Themes app
#   TV_HOST=<tv ip> work/build-and-install.sh launcher # launcher only
set -euo pipefail
cd "$(dirname "$0")/.."
export TV_HOST="${TV_HOST:?set TV_HOST to the TV IP address}"
PY=tools/venv/bin/python
[ -x "$PY" ] || { python3 -m venv tools/venv && tools/venv/bin/pip install -q -r tools/requirements.txt; }
docker info >/dev/null 2>&1 || { open -a OrbStack; echo "starting OrbStack..."; for _ in $(seq 30); do docker info >/dev/null 2>&1 && break; python3 -c 'import time; time.sleep(2)'; done; }

echo "== building the launcher"
tools/docker-build.sh launcher
# The repo script installs it, makes it Home, and keeps the Home guard (it never leaves the TV without a Home).
SKIP_BUILD=1 launcher/build-and-install.sh

[ "${1:-}" = launcher ] && exit 0

echo "== building the Ad-blocking DNS switch"
tools/docker-build.sh work/dns-toggle
"$PY" tools/b64push.py work/dns-toggle/app/build/outputs/apk/release/app-release.apk /data/local/tmp/dns-toggle.apk
"$PY" tools/tv.py 'pm install -r /data/local/tmp/dns-toggle.apk'
"$PY" tools/tv.py 'pm grant home.dnstoggle android.permission.WRITE_SECURE_SETTINGS'
"$PY" tools/tv.py 'rm /data/local/tmp/dns-toggle.apk'

echo "== building the Themes switcher (every theme in themes/, with this TV's dock kept)"
python3 work/theme-switcher/prepare_assets.py
tools/docker-build.sh work/theme-switcher
"$PY" tools/b64push.py work/theme-switcher/app/build/outputs/apk/release/app-release.apk /data/local/tmp/themes.apk
"$PY" tools/tv.py 'pm install -r /data/local/tmp/themes.apk'
"$PY" tools/tv.py 'pm grant home.themes android.permission.WRITE_SECURE_SETTINGS'
"$PY" tools/tv.py 'appops set home.themes WRITE_SETTINGS allow'
"$PY" tools/tv.py 'rm /data/local/tmp/themes.apk'
echo "Installed. Press Home on the remote."
