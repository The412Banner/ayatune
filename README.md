# ayatune

Per-game tuning overlay for AYANEO handhelds — sits alongside AYANEO's own Game Window / Settings rather than replacing them.

> Status: pre-alpha. Design + reverse-engineering only. No installable build yet.

## What it is

A floating overlay (swipe-from-edge or notification-tile) that:

- Picks a CPU/GPU/fan profile **per game** (auto-applied on launch, reverted on exit).
- Mirrors AYANEO Settings' state so the stock Game Window stays accurate.
- On a rooted device — Magisk, KernelSU, etc. — unlocks a deep-tuning pane with knobs AYANEO doesn't expose: per-policy mixed governors, true per-CPU min freq, GPU power-level by index, `idle_timer` slider, and more. Root is requested via libsu at runtime; **no separate module or `.zip` to flash**.

Targets: AYANEO Pocket FIT (AR14 / SD8G2) first. Architecture generalizes to other AR-series (AR01..AR16) and Pocket S2 / Pocket Air / Pocket Micro / Pocket Pixel rebrands.

## How it works

```
┌──────────────────────────────┐
│  ayatune overlay (this app)  │  normal-priority install, any user
└────┬────────────────┬────────┘
     │                │
     │ AIDL (always)  │ libsu / Shell.cmd()  (only if root grant on tap)
     ▼                ▼
┌────────────────┐  ┌─────────────────────────┐
│ AYANEO         │  │ /sys/class/kgsl/...     │
│ gamewindow's   │  │ /sys/devices/system/cpu │
│ AyaAidlService │  │ (deep-tuning knobs)     │
└────────────────┘  └─────────────────────────┘
```

- **AIDL path** (always on): binds to `com.ayaneo.gamewindow/.utils.aidl.AyaAidlService` (exported, no permission required) and uses AYANEO's own `PerformanceManager` for everything it can do.
- **Root path** (opt-in on first tap of the deep-tuning pane): libsu asks the device's root daemon for a privileged shell. Standard one-tap Magisk/KernelSU prompt, persistent grant. Used only for sysfs writes outside AYANEO's surface, with a hardcoded allowlist.

See:
- [`docs/AIDL_SURFACE.md`](docs/AIDL_SURFACE.md) — full daemon contract, message catalog, ConfigData JSON schema.
- [`docs/HARDWARE_MAP_AR14.md`](docs/HARDWARE_MAP_AR14.md) — per-device frequency tables, sysfs paths, empirical write-access matrix.
- [`docs/PRIVILEGED_WRITES.md`](docs/PRIVILEGED_WRITES.md) — libsu wiring, allowlist, batch-apply pattern.

## Layout

```
aidl/                       reconstructed AYANEO AIDL stubs (single source of truth;
                            app/build.gradle.kts re-exposes them as an AIDL srcDir)
docs/                       RE notes, hardware references, libsu wiring guide
prototype/                  shell-script prototypes that exercise both AIDL and sysfs paths
                            without an APK install
app/                        Android app
  src/main/AndroidManifest.xml
  src/main/java/banner/tune/
    AyaTuneApplication.kt   libsu init
    MainActivity.kt         Compose host
    core/ayaneo/
      AyaBridge.kt          AyaAidlService client + StateFlow/SharedFlow
      PerformanceConfig.kt  JSON model (ConfigData/ModeConfiguration/CPU/GPU/Fan)
    core/sys/
      Privilege.kt          Shell.isAppGrantedRoot + on-demand prompt
      PrivilegedWriter.kt   libsu writer with hardcoded allowlist
      SysfsReader.kt        unprivileged reads (live monitors hot path)
    core/hw/
      DeviceProfile.kt      per-device CPU policy + GPU freq tables (AR14 in-tree)
    ui/screen/HomeScreen.kt scaffold landing screen
    ui/theme/Theme.kt       Material 3
.github/workflows/build.yml CI: gradle 8.10.2, assembleDebug + assembleRelease
```

## Status

- [x] AYANEO stack reverse-engineered (gamewindow 1.5.84, settings 1.1.112)
- [x] AIDL surface documented
- [x] Hardware map + write-access matrix verified on Pocket FIT
- [x] Privilege model: AIDL by default, libsu-runtime-root for deep tuning
- [x] App scaffold (Kotlin + Compose + libsu): AyaBridge, PrivilegedWriter, DeviceProfile, scaffold UI
- [ ] AIDL round-trip device-verified (currently scaffold only; bind succeeds in code, but no real `com_set_performance_mode` exercise yet)
- [ ] Mode picker UI + per-game profile model
- [ ] Floating overlay + foreground-app watcher
- [ ] Deep-tuning pane (CPU per-policy gov mix, GPU pwrlevel, idle_timer, …)
- [ ] RGB / controller layout / swap-zram / EQ panes (in-scope follow-ups; see project memory)
- [ ] Push `The412Banner/ayatune` (then CI auto-runs)

## Building

CI builds the APK on every push — see `.github/workflows/build.yml`. Local builds:

```sh
# one-time, generates gradle/wrapper/gradle-wrapper.jar
gradle wrapper --gradle-version 8.10.2
# subsequent builds
./gradlew :app:assembleDebug
```

The wrapper jar is intentionally not committed (binary). CI uses `setup-gradle` to provide gradle directly, so no wrapper is needed for the CI path.

## Warnings

- AYANEO Settings, Game Window, and `com.aya.gsset` ship with the platform key and run as `android.uid.system`. They are first-party and have access we cannot replicate from a normal install. ayatune deliberately stays out of that lane.
- `com.aya.gsset` is **not** what its name suggests (see [docs/AIDL_SURFACE.md §AyaGsSet warning](docs/AIDL_SURFACE.md)). It is a GSF Android-ID forcer and can break Google Play Services if you toggle the wrong things.
- Tuning the GPU clock high while running on battery without active fan = thermal throttling, sometimes hard. Always pair `GPU max` increases with `fan = BALANCE` or higher.
