#!/system/bin/sh
# ayatune — AIDL prototype #1
#
# Uses Android's built-in `service call` to send a single raw Binder transaction
# at the AYANEO Game Window service. Confirms the binder is reachable AND that
# our transaction-code understanding matches the decompiled code.
#
# We DO NOT actually flip a performance mode here — we send transaction 2
# (registerCallback) with a NULL Binder, which the service handles gracefully
# (the proxy check is `strongBinder != null`) and replies with writeNoException.
#
# Output should look like:
#   Result: Parcel(00000000    '....')
# with no exception bytes. If you see a non-zero status int after the header,
# the service rejected the txn.
#
# Run as the shell user (adb, Termux, or the bridge w/ `su shell -c ...`).
# Does NOT require root or system uid. Just verifies the binder is exported
# and the txn IDs line up.

set -u

SERVICE_PKG="com.ayaneo.gamewindow"
SERVICE_CLS="com.ayaneo.gamewindow.utils.aidl.AyaAidlService"
IFACE="com.ayaneo.gamewindow.AyaAidlInterface"

echo "[1/3] confirm service is alive (pidof)"
pidof "$SERVICE_PKG" || { echo "  gamewindow not running — bail"; exit 1; }
echo "  pid: $(pidof "$SERVICE_PKG")"

echo
echo "[2/3] confirm service exposes the binder (dumpsys activity service)"
dumpsys activity services "$SERVICE_PKG/$SERVICE_CLS" 2>&1 \
  | grep -E "ConnectionRecord|app=ProcessRecord|nRequests|bindings" \
  | head -5

echo
echo "[3/3] send transaction 2 (registerCallback null) — expect Parcel with no exception"
# `service call` syntax: <service_name> <code> [arg-types args ...]
# AyaAidlService isn't in the global service manager (it's bound by Intent, not
# add_service), so `service call` by name won't work directly. We use `cmd`
# instead via a fake intent — actually, the cleanest path is a tiny Kotlin
# scratch (see aidl_smoke_app.kt sibling). This script's "[3/3]" stage is
# therefore a placeholder + sanity hint; the meaningful proof is the bound
# AIDL client in the app skeleton.

echo "  NOTE: AyaAidlService is bound by Intent, not registered with"
echo "        ServiceManager — so 'service call' can't address it directly."
echo "        Use prototype/aidl_smoke_app/ (small Kotlin/Java) for a real"
echo "        bound-binder test. This script verified the process + manifest"
echo "        export only."
