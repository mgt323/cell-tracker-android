package com.mgt323.celltracker.ui.currentconnection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mgt323.celltracker.domain.model.CellSnapshot
import com.mgt323.celltracker.ui.theme.MyApplicationTheme

/**
 * Stateful entry point: wires up [CurrentConnectionViewModel] via Hilt and
 * delegates all rendering to [CurrentConnectionContent], which is stateless
 * and Hilt-free so it can be driven directly from `@Preview`s.
 *
 * Uses [collectAsState] rather than `collectAsStateWithLifecycle` because
 * `androidx.lifecycle:lifecycle-runtime-compose` is not yet a declared
 * dependency of this module (checked `gradle/libs.versions.toml` and
 * `app/build.gradle` — see the mapper `lifecycle-runtime-ktx` entry, which
 * is a different artifact); adding it was avoided per the task's
 * instructions to prefer the plain `collectAsState()` fallback.
 */
@Composable
fun CurrentConnectionScreen(
    viewModel: CurrentConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CurrentConnectionContent(state = uiState)
}

/**
 * Stateless presentation composable driven entirely by [state]. The main
 * dashboard ([CellSnapshotDashboard]) is only ever rendered for
 * [CurrentConnectionUiState.Content] — per `.cursor/rules/permissions.mdc`,
 * [CurrentConnectionUiState.PermissionsRequired] always renders the
 * dedicated fallback instead, never the dashboard underneath it.
 */
@Composable
fun CurrentConnectionContent(state: CurrentConnectionUiState) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is CurrentConnectionUiState.Loading -> LoadingIndicator()
            is CurrentConnectionUiState.PermissionsRequired -> PermissionsRequiredScreen()
            is CurrentConnectionUiState.Content -> CellSnapshotDashboard(state.snapshot)
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PermissionsRequiredScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Cell Tracker needs the Location (precise) and Phone " +
                    "State permissions to read your device's current cellular " +
                    "signal and carrier aggregation info. Android requires " +
                    "ACCESS_FINE_LOCATION in addition to READ_PHONE_STATE to " +
                    "access cell info on this OS version. Grant both " +
                    "permissions in Settings to see your current connection.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CellSnapshotDashboard(snapshot: CellSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                    Text(
                        text = "${snapshot.cellType.name} \u2022 ${snapshot.band}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                CarrierAggregationBadge(isActive = snapshot.isCarrierAggregationActive)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    LabeledValue(label = "RSRP", value = "${snapshot.rsrp} dBm")
                    LabeledValue(label = "RSRQ", value = "${snapshot.rsrq} dB")
                    LabeledValue(label = "Bandwidth", value = "${snapshot.bandwidthKhz} kHz")
                }
            }
        }

        if (snapshot.isCarrierAggregationActive && snapshot.secondaryBands.isNotEmpty()) {
            SecondaryBandsSection(secondaryBands = snapshot.secondaryBands)
        }
    }
}

@Composable
private fun CarrierAggregationBadge(isActive: Boolean) {
    val (containerColor, contentColor, label) = if (isActive) {
        Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Carrier Aggregation Active")
    } else {
        Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Carrier Aggregation Inactive")
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SecondaryBandsSection(secondaryBands: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Secondary bands", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(secondaryBands) { band ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(band) },
                    colors = SuggestionChipDefaults.suggestionChipColors()
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun CurrentConnectionContentPreview_Loading() {
    MyApplicationTheme {
        CurrentConnectionContent(state = CurrentConnectionUiState.Loading)
    }
}

@Preview(showBackground = true, name = "Permissions Required")
@Composable
private fun CurrentConnectionContentPreview_PermissionsRequired() {
    MyApplicationTheme {
        CurrentConnectionContent(state = CurrentConnectionUiState.PermissionsRequired)
    }
}

@Preview(showBackground = true, name = "LTE with Carrier Aggregation")
@Composable
private fun CurrentConnectionContentPreview_LteWithAggregation() {
    MyApplicationTheme {
        CurrentConnectionContent(
            state = CurrentConnectionUiState.Content(FakeTelephonyRepository.SAMPLE_LTE_WITH_AGGREGATION)
        )
    }
}

@Preview(showBackground = true, name = "5G-NR, no aggregation")
@Composable
private fun CurrentConnectionContentPreview_NrNoAggregation() {
    MyApplicationTheme {
        CurrentConnectionContent(
            state = CurrentConnectionUiState.Content(FakeTelephonyRepository.SAMPLE_NR_NO_AGGREGATION)
        )
    }
}
