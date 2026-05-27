package banner.tune.core.sys

import com.topjohnwu.superuser.Shell
import timber.log.Timber

/**
 * Allowlisted privileged writes via libsu — used by the deep-tuning pane
 * for sysfs paths that AYANEO's AyaAidlService can't reach. See
 * docs/PRIVILEGED_WRITES.md for design rationale.
 *
 * Writes are batched on profile apply (one root subprocess for N writes)
 * to avoid per-slider IPC overhead. Always require [Privilege.hasRoot]
 * before calling; this class does not prompt.
 */
object PrivilegedWriter {

    private val allowlist: Set<String> = buildSet {
        // CPU per-policy governors
        for (p in intArrayOf(0, 2, 5, 7)) {
            add("/sys/devices/system/cpu/cpufreq/policy$p/scaling_governor")
            add("/sys/devices/system/cpu/cpufreq/policy$p/scaling_min_freq")
            add("/sys/devices/system/cpu/cpufreq/policy$p/scaling_max_freq")
        }
        // CPU per-core min/max (AYANEO ties min to max only in HIGH_PERFORMANCE;
        // we expose all 8 cores' min independently)
        for (c in 0..7) {
            add("/sys/devices/system/cpu/cpu$c/cpufreq/scaling_min_freq")
            add("/sys/devices/system/cpu/cpu$c/cpufreq/scaling_max_freq")
        }
        // GPU / KGSL
        listOf(
            "idle_timer",
            "max_gpuclk",
            "devfreq/min_freq",
            "devfreq/max_freq",
            "min_pwrlevel",
            "max_pwrlevel",
            "default_pwrlevel",
            "force_clk_on",
            "force_no_nap",
            "hwcg",
            "lpac",
            "l3_vote",
        ).forEach { add("/sys/class/kgsl/kgsl-3d0/$it") }
    }

    private val valueRe = Regex("""[A-Za-z0-9_\-]+""")

    /** Single fire-and-forget write. Returns true on success. */
    fun write(path: String, value: String): Boolean {
        require(path in allowlist) { "ayatune: path not in allowlist: $path" }
        require(valueRe.matches(value)) { "ayatune: bad value: $value" }
        val out = Shell.cmd("echo $value > $path").exec()
        if (!out.isSuccess) {
            Timber.tag(TAG).w("write failed rc=%d path=%s err=%s", out.code, path, out.err)
        }
        return out.isSuccess
    }

    /** Batch a profile's writes into one root subprocess (faster). */
    fun applyBatch(writes: List<Pair<String, String>>): Boolean {
        if (writes.isEmpty()) return true
        writes.forEach { (p, v) ->
            require(p in allowlist) { "ayatune: blocked: $p" }
            require(valueRe.matches(v)) { "ayatune: bad value: $v" }
        }
        val script = writes.joinToString("\n") { (p, v) -> "echo $v > $p" }
        val out = Shell.cmd(script).exec()
        if (!out.isSuccess) {
            Timber.tag(TAG).w("batch failed rc=%d err=%s", out.code, out.err)
        }
        return out.isSuccess
    }

    private const val TAG = "PrivWriter"
}
