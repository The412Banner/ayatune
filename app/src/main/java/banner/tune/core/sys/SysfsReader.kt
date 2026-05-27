package banner.tune.core.sys

import timber.log.Timber
import java.io.File

/**
 * Unprivileged sysfs reads. All target nodes for live monitoring are
 * `-rw-r--r--` (world-readable); no root needed. Use [readInt]/[readLong]
 * /[readString] hot paths in the live-monitor overlay rather than
 * [PrivilegedWriter] for reads — much cheaper than spawning a shell.
 */
object SysfsReader {

    fun readString(path: String): String? = try {
        File(path).readText().trim()
    } catch (t: Throwable) {
        Timber.tag(TAG).v(t, "read string failed: %s", path)
        null
    }

    fun readInt(path: String): Int? = readString(path)?.toIntOrNull()
    fun readLong(path: String): Long? = readString(path)?.toLongOrNull()

    /** Whitespace-separated list of ints; e.g. KGSL gpu_available_frequencies. */
    fun readLongList(path: String): List<Long> =
        readString(path)?.split(Regex("\\s+"))?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

    object Paths {
        // CPU
        fun cpuScalingCurFreq(cpu: Int) = "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_cur_freq"
        fun cpuScalingMin(cpu: Int) = "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_min_freq"
        fun cpuScalingMax(cpu: Int) = "/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_max_freq"
        fun policyGovernor(policy: Int) = "/sys/devices/system/cpu/cpufreq/policy$policy/scaling_governor"

        // GPU (KGSL Adreno)
        const val GPU_CUR_FREQ = "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq"
        const val GPU_MIN_FREQ = "/sys/class/kgsl/kgsl-3d0/devfreq/min_freq"
        const val GPU_MAX_FREQ = "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq"
        const val GPU_AVAILABLE_FREQUENCIES = "/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies"
        const val GPU_BUSY = "/sys/class/kgsl/kgsl-3d0/gpubusy"
        const val GPU_BUSY_PCT = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
        const val GPU_IDLE_TIMER = "/sys/class/kgsl/kgsl-3d0/idle_timer"
        const val GPU_MODEL = "/sys/class/kgsl/kgsl-3d0/gpu_model"
        const val GPU_NUM_PWRLEVELS = "/sys/class/kgsl/kgsl-3d0/num_pwrlevels"
    }

    private const val TAG = "SysfsReader"
}
