package banner.tune.core.ayaneo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Mirror of AYANEO's `com.ayaneo.gamewindow.ui.window.performance.util.*`
 * data classes. The on-the-wire JSON (payload of
 * `com_set_performance_mode`) follows this shape exactly — match field
 * names + types or AYANEO's Gson will reject it.
 *
 * See docs/AIDL_SURFACE.md §"ConfigData JSON schema".
 */
@Serializable
data class ConfigData(
    @SerialName("currentMode") val currentMode: Int,
    @SerialName("modeConfigurations") val modeConfigurations: Map<String, ModeConfiguration>,
)

@Serializable
data class ModeConfiguration(
    @SerialName("cpuFrequencies") val cpuFrequencies: List<CPUFrequency>,
    @SerialName("cpuSchedulerMode") val cpuSchedulerMode: CPUSchedulerMode,
    @SerialName("gpuFrequency") val gpuFrequency: GPUFrequency,
    @SerialName("fanMode") val fanMode: FanMode,
    @SerialName("lastFanMode") val lastFanMode: FanMode = fanMode,
)

@Serializable
data class CPUFrequency(
    @SerialName("cpuId") val cpuId: Int,
    @SerialName("minFrequency") val minFrequency: Int,
    @SerialName("maxFrequency") val maxFrequency: Int,
    @SerialName("selectedFrequency") val selectedFrequency: Int,
    @SerialName("frequencies") val frequencies: List<Int> = emptyList(),
)

@Serializable
data class GPUFrequency(
    @SerialName("minFrequency") val minFrequency: Long,
    @SerialName("maxFrequency") val maxFrequency: Long,
    @SerialName("selectedFrequency") val selectedFrequency: Long,
    @SerialName("isFixed") val isFixed: Boolean,
)

@Suppress("EnumEntryName")
@Serializable
enum class CPUSchedulerMode { POWER_SAVING, BALANCED, HIGH_PERFORMANCE }

@Suppress("EnumEntryName")
@Serializable
enum class FanMode {
    FAN_MODE_OFF,
    FAN_MODE_MUTE,
    FAN_MODE_BALANCE,
    FAN_MODE_TURBO,
    FAN_MODE_CUSTOM,
}

/** AYANEO's preset slots — value matches `currentMode` field. */
object PerformanceMode {
    const val SAVING = 0
    const val BALANCE = 1
    const val GAME = 2
    const val MAX = 3
    const val STREAMING = 4
}

/** COM_* command strings — see AyaSettings AidlConstants. */
object Com {
    const val SET_PERFORMANCE_MODE = "com_set_performance_mode"
    const val SET_PERFORMANCE_RESET = "com_set_performance_reset"
    const val SET_PERFORMANCE_FAN = "com_set_performance_fan"
    const val SET_PERFORMANCE_CPU = "com_set_performance_cpu"
    const val SET_PERFORMANCE_SCHEDULER = "com_set_performance_scheduler"
    const val SET_PERFORMANCE_GPU = "com_set_performance_gpu"
    const val SET_PERFORMANCE_GPU_IS_FIXED = "com_set_performance_gpu_is_fixed"
    const val SET_FAN_SPEED_STRATEGY = "com_set_fan_speed_strategy"
    const val SET_FAN_SPEED_IS_LINEAR = "com_set_fan_speed_is_linear"
    const val SET_RGB_IS_OPEN = "com_set_rgb_is_open"
    const val SET_ABXY_MODE = "com_set_abxy_mode"
    const val SET_L1L2R1R2_MODE = "com_set_l1l2r1r2_mode"
    const val SET_DIRECTION_DPAD_MODE = "com_set_direction_dpad_mode"
}

object AyaJson {
    val codec: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}
