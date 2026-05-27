#!/system/bin/sh
# ayatune — sysfs prototype #2
#
# Walks the write-access matrix for our target sysfs nodes from THREE different
# privilege levels (root / system uid / shell uid) and prints a pass/fail table.
# This is the empirical version of docs/HARDWARE_MAP_AR14.md §"Write-access
# matrix" — re-run it on any new device or after any ROM update to refresh.
#
# Requires root to launch (uses `su <user> -c` to drop privileges for each test).
# Restores every node to its pre-test value at the end.
#
# Usage:
#   adb shell su -c sh /sdcard/ayatune/prototype/sysfs_smoke.sh
#   or
#   getlog --exec "sh /sdcard/ayatune/prototype/sysfs_smoke.sh"

set -u

TARGETS="
/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
/sys/devices/system/cpu/cpufreq/policy0/scaling_governor
/sys/class/kgsl/kgsl-3d0/idle_timer
/sys/class/kgsl/kgsl-3d0/max_gpuclk
/sys/class/kgsl/kgsl-3d0/devfreq/min_freq
/sys/class/kgsl/kgsl-3d0/devfreq/max_freq
"

# Pick a safe value to write to each node — same value it already has, so
# the test is non-destructive even on success.
write_test() {
  local node="$1"
  local user="$2"   # root | system | shell
  local before
  before=$(cat "$node" 2>/dev/null)
  [ -z "$before" ] && { echo "  $user: SKIP (cannot read)"; return; }
  local rc
  if [ "$user" = "root" ]; then
    echo "$before" > "$node" 2>/dev/null
    rc=$?
  else
    su "$user" -c "echo '$before' > '$node'" 2>/dev/null
    rc=$?
  fi
  local after
  after=$(cat "$node" 2>/dev/null)
  if [ "$rc" = 0 ] && [ "$after" = "$before" ]; then
    echo "  $user: PASS"
  else
    echo "  $user: FAIL rc=$rc"
  fi
}

echo "=== ayatune sysfs write-access smoke test ==="
echo "Device: $(getprop ro.product.model) / $(getprop ro.build.fingerprint)"
echo

for node in $TARGETS; do
  [ -e "$node" ] || { echo "[skip] $node (not present)"; continue; }
  echo "$node"
  printf "  mode/label: "
  ls -lZ "$node" | awk '{print $1, $5}'
  write_test "$node" root
  write_test "$node" system
  write_test "$node" shell
  echo
done

echo "Done. Reference: docs/HARDWARE_MAP_AR14.md"
