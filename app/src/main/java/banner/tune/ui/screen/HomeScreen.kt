package banner.tune.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import banner.tune.core.ayaneo.AyaBridge
import banner.tune.core.ayaneo.PerformanceFacade
import banner.tune.core.hw.DeviceProfiles
import banner.tune.core.sys.Privilege
import banner.tune.core.sys.SysfsReader

/**
 * Landing screen for the scaffold: device profile, AYANEO daemon connection
 * status, root state, live GPU readout, and the 5-mode picker (which will
 * fire `com_set_performance_mode` once the config cache is populated).
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val device = DeviceProfiles.current

    val bridge = remember { AyaBridge(ctx.applicationContext) }
    val perf = remember { PerformanceFacade(bridge, device, scope) }

    val bridgeState by bridge.state.collectAsState()
    val config by perf.config.collectAsState()
    var hasRoot by remember { mutableStateOf(Privilege.hasRoot) }

    LaunchedEffect(Unit) {
        bridge.connect()
        hasRoot = Privilege.hasRoot
    }
    DisposableEffect(Unit) {
        onDispose { bridge.disconnect() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ayatune", style = MaterialTheme.typography.headlineMedium)
        Text(
            "pre-alpha · v0.0.2 scaffold + mode picker",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        ModePicker(
            config = config,
            onModeSelected = { perf.setMode(it) },
            onOpenAyaSettings = {
                runCatching {
                    val intent = ctx.packageManager
                        .getLaunchIntentForPackage("com.ayaneo.settings")
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (intent != null) ctx.startActivity(intent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        InfoCard(
            title = "Device",
            lines = listOf(
                "${device.displayName} (${device.codename})",
                "${device.totalCores} cores · ${device.cpuPolicies.size} policies",
                "${device.gpuFrequencies.size} GPU pwrlevels (${device.gpuFrequencies.last() / 1_000_000}–${device.gpuFrequencies.first() / 1_000_000} MHz)",
            ),
        )

        InfoCard(
            title = "AYANEO Game Window",
            lines = listOf(
                "package: ${AyaBridge.GAMEWINDOW_PKG}",
                "state: ${bridgeState::class.simpleName}",
            ) + when (val s = bridgeState) {
                is AyaBridge.State.Connected -> listOf("clientId: ${s.clientId ?: "(awaiting register reply)"}")
                is AyaBridge.State.Failed -> listOf("error: ${s.reason}")
                else -> emptyList()
            },
        )

        InfoCard(
            title = "Root (deep tuning)",
            lines = listOf(
                if (hasRoot) "Granted — deep-tuning pane unlocked"
                else "Not granted — tap to enable deep tuning (deferred)"
            ),
        )

        InfoCard(
            title = "Live GPU",
            lines = listOf(
                "cur:   ${SysfsReader.readLong(SysfsReader.Paths.GPU_CUR_FREQ)?.div(1_000_000) ?: "?"} MHz",
                "range: ${SysfsReader.readLong(SysfsReader.Paths.GPU_MIN_FREQ)?.div(1_000_000) ?: "?"}–${SysfsReader.readLong(SysfsReader.Paths.GPU_MAX_FREQ)?.div(1_000_000) ?: "?"} MHz",
                "idle_timer: ${SysfsReader.readString(SysfsReader.Paths.GPU_IDLE_TIMER) ?: "?"} ms",
                "model: ${SysfsReader.readString(SysfsReader.Paths.GPU_MODEL) ?: "?"}",
            ),
        )
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
