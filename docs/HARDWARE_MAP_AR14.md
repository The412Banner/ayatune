# Hardware map — AYANEO Pocket FIT (AR14 / SD8G2)

Generated from live device inspection 2026-05-26, build `01.00_20251218`.

## CPU layout

Snapdragon 8 Gen 2 (kalama). 8 cores, 4 cpufreq policies:

| Policy | CPUs | Freq min (kHz) | Freq max (kHz) | Notes |
|---|---|---|---|---|
| `policy0` | cpu0, cpu1 | 364800 | 2265600 | Silver (efficiency, Cortex-A510?) |
| `policy2` | cpu2, cpu3, cpu4 | 499200 | 3148800 | Gold (Cortex-A715?) |
| `policy5` | cpu5, cpu6 | 499200 | 2956800 | Gold+ |
| `policy7` | cpu7 | 480000 | 3302400 | Prime (Cortex-X3) |

Current governor on all four: `walt` (Qualcomm Window-Assisted Load Tracking — proprietary, default balanced).

Available governors (per ROM): `powersave`, `walt`, `schedutil`, `performance`.

## GPU

Adreno 740 (kernel reports as `Adreno33v2`). AYANEO Settings spoofs the name to `"Adreno (TM) 750"` for app compatibility.

14 power levels, available frequencies (Hz):

```
1050000000  ← pwrlevel 0 (max), default_pwrlevel = 13 (=min)
1000000000
903000000
834000000
770000000
720000000
680000000
629000000
578000000
500000000
422000000
366000000
310000000
231000000  ← pwrlevel 13
```

KGSL device: `/sys/class/kgsl/kgsl-3d0`. Governor: `msm-adreno-tz` (only option).

Currently observed pinned at 1050 MHz (`min_freq = max_freq = 1050000000`), `idle_timer = 1000000` — i.e. AYANEO is in some kind of "performance hold" mode at the time of inspection.

## Write-access matrix (verified)

This is the *empirical* result of attempting writes via the bridge under different uids (each `su <user>` retains the `magisk` SELinux domain, but DAC is enforced first):

| Path | DAC mode | SELinux label | root | uid=system | uid=shell |
|---|---|---|---|---|---|
| `cpu*/cpufreq/scaling_max_freq` | `system:system 664` | `sysfs_devices_system_cpu` | ✅ | ✅ | ❌ |
| `cpu*/cpufreq/scaling_min_freq` | `system:system 664` | `sysfs_devices_system_cpu` | ✅ | ✅ | ❌ |
| `cpufreq/policy*/scaling_governor` | `root:root 644` | `sysfs_devices_system_cpu` | ✅ | ❌ | ❌ |
| `kgsl-3d0/idle_timer` | `root:root 644` | `vendor_sysfs_kgsl` | ✅ | ❌ | ❌ |
| `kgsl-3d0/max_gpuclk` | `root:root 644` | `vendor_sysfs_kgsl` | ✅ | ❌ | ❌ |
| `kgsl-3d0/devfreq/min_freq` | `root:root 644` | `vendor_sysfs_kgsl` | ✅ | ❌ | ❌ |
| `kgsl-3d0/devfreq/max_freq` | `root:root 644` | `vendor_sysfs_kgsl` | ✅ | ❌ | ❌ |

**Implications for ayatune:**

- A non-root install (whether normal app or Shizuku) **cannot** write any of these directly. Even cpufreq's `664 system:system` excludes the `shell` group.
- AYANEO Settings can write CPU max/min from system uid (group `system`). For per-policy governor and KGSL it presumably relies on something extra we haven't fully traced — either a vendor init.rc that loosens those nodes on boot for the AYANEO process, an undiscovered HAL daemon, or platform-specific capability grants. We can't replicate this without ROM modifications.
- This is why ayatune is **AIDL-first**: a plain install gets the same tuning surface AYANEO already provides, mediated through the daemon. On rooted devices, ayatune requests root from Magisk/KernelSU at runtime via libsu (no separate module install needed) and unlocks the deeper knobs (idle_timer, per-policy mixed governors, true per-CPU min freq, GPU pwrlevel by index, etc.). See `docs/PRIVILEGED_WRITES.md`.

## Other interesting kgsl nodes worth exposing (root-tier)

From `ls /sys/class/kgsl/kgsl-3d0/`:

- `idle_timer` — ms before downclock; high = "fixed", low = aggressive (AYANEO uses 80 / 10000000)
- `default_pwrlevel` — boot-time level (currently 13 = 231 MHz)
- `min_pwrlevel` — clamp ceiling (lower number = higher clock; 0 = unrestricted)
- `max_pwrlevel` — clamp floor
- `force_clk_on`, `force_bus_on`, `force_no_nap`, `force_rail_on` — for benchmarking; pin the clock fully
- `lpac` — low-power-async-compute toggle
- `bus_split` — DDR bandwidth split policy
- `hwcg` — hardware clock gating
- `l3_vote` — L3 cache frequency vote
- `bcl`, `clx`, `acd` — thermal/throttle subsystems
- `gpu_llc_slice_enable`, `gpuhtw_llc_slice_enable` — system-cache slice usage
- `gpubusy`, `gpu_busy_percentage`, `gpu_clock_stats` — readonly telemetry

## Fan

MCU over virtio-serial at `/dev/ttyHS5`. Payload format in `com.ayaneo.gamewindow.utils.newserial.NewSerialHelper`. Five named modes (`FAN_MODE_{OFF,MUTE,BALANCE,TURBO,CUSTOM}`). The MCU does its own curve in BALANCE / TURBO; CUSTOM lets the host stream a target-rpm/target-pct. We will **always go through the AIDL** for fan — direct serial writes would race AYANEO's serial loop.

## Other surfaces seen but out of scope (initially)

- RGB controller (via the same serial bus, `RgbCmdUtilAR03` etc.)
- Xbox controller simulator (`startXboxControllerSimulator`)
- 10-band audio equalizer + bass boost (provider keys `equalizer_*`)
- Brightness mask / day-night transforms
- Power-supply-status bypass (`bypass_power_supply_status` — for charge-while-running scenarios)
- Per-app rate (`aya_screen_rate_default`, default 120 Hz)
- Swap / zram management (`IAyaDevicesSwaps`)
