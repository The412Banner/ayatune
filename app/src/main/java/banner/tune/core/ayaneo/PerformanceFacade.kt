package banner.tune.core.ayaneo

import banner.tune.core.hw.DeviceProfile
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Owns the in-memory mirror of AYANEO's [ConfigData] and provides a typed
 * API over the [AyaBridge] raw messages for performance-mode operations.
 *
 * The cache is populated, in order of preference:
 *   1. **Direct file read** at `/sdcard/.aya/<deviceKey>` — fast path, but
 *      needs MANAGE_EXTERNAL_STORAGE granted, OR a root grant we can use
 *      via libsu. Tried opportunistically on construction.
 *   2. **Broadcast capture** — every `com_set_performance_mode:<json>`
 *      that crosses the AIDL bus (sent by AyaSettings, ourselves, or any
 *      other client) updates the cache. Always-on.
 *
 * If both paths come up empty, callers should show a "sync needed" hint
 * and ask the user to flip a mode in AyaSettings once.
 */
class PerformanceFacade(
    private val bridge: AyaBridge,
    private val device: DeviceProfile,
    private val scope: CoroutineScope,
) {

    private val _config = MutableStateFlow<ConfigData?>(null)
    val config: StateFlow<ConfigData?> = _config.asStateFlow()

    init {
        // Always-on broadcast listener.
        scope.launch {
            bridge.messages
                .filter { it.startsWith(AyaBridge.MSG_TYPE_PERFORMANCE + ":") }
                .onEach { onPerformanceMessage(it) }
                .collect { }
        }
        // Opportunistic file read.
        scope.launch { tryLoadFromFile() }
    }

    private fun onPerformanceMessage(raw: String) {
        // Envelope: "msg_type_performance:com_set_performance_mode:<json>"
        val tail = raw.removePrefix(AyaBridge.MSG_TYPE_PERFORMANCE + ":")
        val sep = tail.indexOf(':')
        if (sep < 0) return
        val cmd = tail.substring(0, sep)
        val payload = tail.substring(sep + 1)
        when (cmd) {
            Com.SET_PERFORMANCE_MODE -> parseAndStore(payload)
            // Other COM_* commands (CPU, GPU, FAN, SCHEDULER) only mutate
            // one slice of the config. AYANEO normally re-broadcasts a full
            // SET_PERFORMANCE_MODE after them, so we don't need to merge here.
        }
    }

    private fun parseAndStore(json: String) {
        try {
            val cfg = AyaJson.codec.decodeFromString(ConfigData.serializer(), json)
            _config.value = cfg
            Timber.tag(TAG).d("cached config currentMode=%d via broadcast", cfg.currentMode)
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "failed to parse ConfigData payload (len=%d)", json.length)
        }
    }

    private suspend fun tryLoadFromFile() = withContext(Dispatchers.IO) {
        val path = "/sdcard/.aya/${device.configKey}"
        val direct = runCatching { File(path).readText() }
        val text = direct.getOrNull() ?: tryReadWithRoot(path)
        if (text != null && _config.value == null) {
            parseAndStore(text)
            Timber.tag(TAG).i("loaded ConfigData from %s (path=%s)",
                if (direct.isSuccess) "direct read" else "libsu", path)
        } else if (text == null) {
            Timber.tag(TAG).d("file read failed (direct=%s); waiting for broadcast",
                direct.exceptionOrNull()?.javaClass?.simpleName ?: "n/a")
        }
    }

    private fun tryReadWithRoot(path: String): String? {
        if (Shell.isAppGrantedRoot() != true) return null
        val out = Shell.cmd("cat $path").exec()
        return if (out.isSuccess) out.out.joinToString("\n") else null
    }

    /**
     * Switch AYANEO to [modeIndex] (0..4). Requires the config cache to
     * be populated; returns false otherwise. The send is fire-and-forget;
     * the resulting broadcast updates our cache (and confirms the change
     * landed).
     */
    fun setMode(modeIndex: Int): Boolean {
        require(modeIndex in 0..4) { "invalid mode index $modeIndex" }
        val current = _config.value ?: run {
            Timber.tag(TAG).w("setMode called before config cached — skipping")
            return false
        }
        val updated = current.copy(currentMode = modeIndex)
        val json = AyaJson.codec.encodeToString(ConfigData.serializer(), updated)
        val payload = "${Com.SET_PERFORMANCE_MODE}:$json"
        val sent = bridge.send(AyaBridge.MSG_TYPE_PERFORMANCE, payload)
        if (sent) {
            // Optimistic local update so the UI flips immediately; the
            // broadcast we receive shortly will reconcile.
            _config.value = updated
        }
        return sent
    }

    companion object {
        private const val TAG = "PerfFacade"
    }
}

/**
 * Per-device config-file basename, matches AYANEO's `IAyaDevices.i1()`.
 * AR03 → "aya_performance_mode.config_v10.conf"
 * AR10 → "aya_performance_mode_20250915.conf"
 * AR14 → "aya_performance_mode.config_ar14_v3.conf"
 */
val DeviceProfile.configKey: String
    get() = when (codename) {
        "AR14" -> "aya_performance_mode.config_ar14_v3.conf"
        "AR10" -> "aya_performance_mode_20250915.conf"
        else -> "aya_performance_mode.config_v10.conf"
    }
