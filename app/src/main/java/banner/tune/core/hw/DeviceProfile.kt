package banner.tune.core.hw

import android.os.Build

/**
 * Per-device static facts: CPU policy layout, GPU power-level table, fan
 * mode availability, etc. AYANEO uses a runtime device-key dispatch (AR01,
 * AR03, AR14, ...); we mirror that here.
 *
 * For now [current] returns the AR14 profile if the device matches, else
 * a generic 4-policy fallback. New devices land as additional entries.
 */
data class DeviceProfile(
    val codename: String,
    val displayName: String,
    /** Logical cores per cpufreq policy, in policy-index order. */
    val cpuPolicies: List<CpuPolicy>,
    /** Available GPU frequencies in Hz, high → low (matches KGSL ordering). */
    val gpuFrequencies: List<Long>,
    val fanModesSupported: Boolean,
    val rgbSupported: Boolean,
) {
    val totalCores: Int get() = cpuPolicies.sumOf { it.cpus.size }
}

data class CpuPolicy(
    val index: Int,
    val cpus: IntRange,
    val label: String,
    val minHz: Int,
    val maxHz: Int,
)

object DeviceProfile_AR14_PocketFIT : DeviceProfile_Companion(
    DeviceProfile(
        codename = "AR14",
        displayName = "AYANEO Pocket FIT",
        cpuPolicies = listOf(
            CpuPolicy(0, 0..1, "Silver",  364_800, 2_265_600),
            CpuPolicy(2, 2..4, "Gold",    499_200, 3_148_800),
            CpuPolicy(5, 5..6, "Gold+",   499_200, 2_956_800),
            CpuPolicy(7, 7..7, "Prime",   480_000, 3_302_400),
        ),
        gpuFrequencies = listOf(
            1_050_000_000L,
            1_000_000_000L,
              903_000_000L,
              834_000_000L,
              770_000_000L,
              720_000_000L,
              680_000_000L,
              629_000_000L,
              578_000_000L,
              500_000_000L,
              422_000_000L,
              366_000_000L,
              310_000_000L,
              231_000_000L,
        ),
        fanModesSupported = true,
        rgbSupported = true,
    )
)

abstract class DeviceProfile_Companion(val profile: DeviceProfile)

object DeviceProfiles {
    /** Best-effort detection. Real AYANEO devices set ro.product.name. */
    val current: DeviceProfile by lazy { detect() }

    private fun detect(): DeviceProfile {
        val name = Build.PRODUCT.orEmpty()
        // Pocket FIT internally reports as Pocket_FIT / PocketS2 — both → AR14
        return when {
            name.contains("Pocket_FIT", ignoreCase = true) ||
            name.contains("PocketS2", ignoreCase = true) ||
            name.contains("Pocket_S", ignoreCase = true)
                -> DeviceProfile_AR14_PocketFIT.profile

            else -> DeviceProfile_AR14_PocketFIT.profile // fallback for now
        }
    }
}
