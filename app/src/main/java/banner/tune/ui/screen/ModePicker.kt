package banner.tune.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import banner.tune.core.ayaneo.ConfigData
import banner.tune.core.ayaneo.PerformanceMode

private val MODE_LABELS = mapOf(
    PerformanceMode.SAVING    to "Saving",
    PerformanceMode.BALANCE   to "Balance",
    PerformanceMode.GAME      to "Game",
    PerformanceMode.MAX       to "Max",
    PerformanceMode.STREAMING to "Streaming",
)

/**
 * AYANEO 5-mode picker. Highlights the cached `currentMode`. Tapping a
 * chip flips the mode via [onModeSelected]; the resulting broadcast from
 * AYANEO updates the cache and re-renders.
 *
 * If [config] is null we don't know AYANEO's current state yet. Show a
 * sync hint instead of disabled chips so the user knows what to do.
 */
@Composable
fun ModePicker(
    config: ConfigData?,
    onModeSelected: (Int) -> Unit,
    onOpenAyaSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Performance mode", style = MaterialTheme.typography.titleMedium)
            if (config == null) {
                SyncHint(onOpenAyaSettings)
            } else {
                ModeChips(currentMode = config.currentMode, onModeSelected = onModeSelected)
            }
        }
    }
}

@Composable
private fun ModeChips(currentMode: Int, onModeSelected: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MODE_LABELS.forEach { (index, label) ->
            FilterChip(
                selected = index == currentMode,
                onClick = { if (index != currentMode) onModeSelected(index) },
                label = {
                    Text(
                        label,
                        fontWeight = if (index == currentMode) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun SyncHint(onOpenAyaSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Sync needed. Open AYANEO Settings → Performance → tap any mode " +
                "(you can re-tap the current one). The mode picker activates as soon " +
                "as AYANEO broadcasts the change.",
            style = MaterialTheme.typography.bodyMedium,
        )
        AssistChip(
            onClick = onOpenAyaSettings,
            label = { Text("Open AYANEO Settings") },
            colors = AssistChipDefaults.assistChipColors(),
        )
    }
}
