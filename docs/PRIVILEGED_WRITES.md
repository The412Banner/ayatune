# Privileged writes (deep-tuning pane)

The ayatune overlay's default install is unprivileged: it talks to AYANEO's `AyaAidlService` for everything that surface exposes. The **deep-tuning pane** unlocks knobs AYANEO doesn't expose (per-policy mixed governors, `idle_timer`, GPU power-level by index, etc.) — those are sysfs paths that only a root process can write.

We get root the same way Tasker, AdAway, Titanium Backup do: **request it at runtime via libsu**. No Magisk module, no `.zip` to flash, no pre-staged binary. libsu spawns `su` (Magisk's, KernelSU's, whichever is installed), the user gets the standard root-grant prompt once, and ayatune has root for that install.

## Gradle wiring

```kotlin
// app/build.gradle.kts
dependencies {
  val libsuVersion = "5.2.2"  // pin a version; libsu is API-stable but updates
  implementation("com.github.topjohnwu.libsu:core:$libsuVersion")
  // no service/io modules needed — we only do short fire-and-forget writes
}
```

In `Application.onCreate`:

```kotlin
Shell.enableVerboseLogging = BuildConfig.DEBUG
Shell.setDefaultBuilder(
  Shell.Builder.create()
    .setFlags(Shell.FLAG_REDIRECT_STDERR)
    .setTimeout(5)
)
```

## Detection

```kotlin
object Privilege {
  /** true = Magisk/KernelSU/etc. has granted root to this app. */
  val hasRoot: Boolean get() = Shell.isAppGrantedRoot() == true
  /** true = a root daemon is installed on the device (independent of grant). */
  val rootAvailable: Boolean get() = Shell.getCachedShell()?.isRoot ?: Shell.cmd("id").exec().isSuccess
}
```

Use `hasRoot` to gate the deep-tuning pane in the UI. If false: hide it, or show a "Enable deep tuning (requires root)" CTA that calls `Shell.getShell {}` and lets Magisk prompt the user.

## The writer

```kotlin
object PrivilegedWriter {
  private val allowlist = setOf(
    // CPU
    "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor",
    "/sys/devices/system/cpu/cpufreq/policy2/scaling_governor",
    "/sys/devices/system/cpu/cpufreq/policy5/scaling_governor",
    "/sys/devices/system/cpu/cpufreq/policy7/scaling_governor",
    "/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq",
    "/sys/devices/system/cpu/cpufreq/policy2/scaling_min_freq",
    "/sys/devices/system/cpu/cpufreq/policy5/scaling_min_freq",
    "/sys/devices/system/cpu/cpufreq/policy7/scaling_min_freq",
    // per-CPU min — AYANEO ties min to max only in HIGH_PERFORMANCE; we
    // expose all 8 cores' min independently.
    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu1/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu3/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu5/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu6/cpufreq/scaling_min_freq",
    "/sys/devices/system/cpu/cpu7/cpufreq/scaling_min_freq",
    // GPU (KGSL / Adreno)
    "/sys/class/kgsl/kgsl-3d0/idle_timer",
    "/sys/class/kgsl/kgsl-3d0/max_gpuclk",
    "/sys/class/kgsl/kgsl-3d0/devfreq/min_freq",
    "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
    "/sys/class/kgsl/kgsl-3d0/min_pwrlevel",
    "/sys/class/kgsl/kgsl-3d0/max_pwrlevel",
    "/sys/class/kgsl/kgsl-3d0/default_pwrlevel",
    "/sys/class/kgsl/kgsl-3d0/force_clk_on",
    "/sys/class/kgsl/kgsl-3d0/force_no_nap",
    "/sys/class/kgsl/kgsl-3d0/hwcg",
    "/sys/class/kgsl/kgsl-3d0/lpac",
    "/sys/class/kgsl/kgsl-3d0/l3_vote",
  )

  private val valueRe = Regex("""[A-Za-z0-9_\-]+""")

  fun write(path: String, value: String): Result<Unit> = runCatching {
    require(path in allowlist) { "path not in allowlist: $path" }
    require(valueRe.matches(value)) { "invalid value: $value" }
    val out = Shell.cmd("echo $value > $path").exec()
    check(out.isSuccess) { "write failed rc=${out.code} ${out.err.joinToString()}" }
  }

  /** Batch writes in one root subprocess — much faster for applying a profile. */
  fun applyProfile(writes: List<Pair<String, String>>): Result<Unit> = runCatching {
    writes.forEach { (p, v) ->
      require(p in allowlist) { "blocked: $p" }
      require(valueRe.matches(v)) { "bad value: $v" }
    }
    val script = writes.joinToString("\n") { (p, v) -> "echo $v > $p" }
    val out = Shell.cmd(script).exec()
    check(out.isSuccess) { "batch failed rc=${out.code} ${out.err.joinToString()}" }
  }
}
```

## Why an allowlist

The app process is the security boundary, not the root subprocess. libsu just exec's whatever the app sends, so a compromise of the app *would* get root. The allowlist makes that worse-case meaningfully smaller: even if an attacker hijacks our process, they can only write to a fixed set of CPU/GPU tuning nodes — not `/data/data/com.google.android.gms/...` or `/dev/block/sdaX`.

It's not a defense against ayatune itself — it's a defense against bugs in ayatune.

## Reads do not need root

All target nodes are world-readable (`-rw-r--r--`). Read with normal `File("...").readText()` — no Shell call needed. This matters: live monitors (gpubusy, temps, cur_freq) poll several times a second; doing those reads through `Shell.cmd` would be wasteful.

## Reverting on profile exit

Profile apply must be paired with a revert. Two strategies:

1. **Snapshot before apply.** Read the current value of every node the profile writes, store, restore on exit. Safe; survives crashes if you persist the snapshot to a file.
2. **Apply AYANEO's "Balance" mode via AIDL.** Lets AYANEO's `PerformanceManager` reset everything — but only the things it manages. Anything we touched outside AYANEO's surface (idle_timer, l3_vote, etc.) needs strategy 1 anyway.

Use both: AIDL-reset for the AYANEO-managed knobs, snapshot-restore for the deep-tuning extras.
