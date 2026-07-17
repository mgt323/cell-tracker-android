package com.ferhatozcelik.jetpackcomposetemplate.ui.home

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType
import com.ferhatozcelik.jetpackcomposetemplate.ui.theme.MyApplicationTheme

/**
 * Stateful entry point: wires up [CurrentConnectionViewModel] via Hilt and
 * delegates rendering to [CurrentConnectionContent]. Not used from
 * `@Preview`s so previews don't require Hilt.
 */
@Composable
fun CurrentConnectionScreen(
    onGrantPermissionsClick: () -> Unit,
    viewModel: CurrentConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CurrentConnectionContent(
        uiState = uiState,
        onGrantPermissionsClick = onGrantPermissionsClick
    )
}

/**
 * Stateless presentation composable driven entirely by [uiState]. The main
 * dashboard content ([CellSnapshotDashboard]) is only ever rendered for
 * [CurrentConnectionUiState.Content] — per `.cursor/rules/permissions.mdc`,
 * [CurrentConnectionUiState.PermissionsRequiredState] always renders the
 * dedicated fallback instead.
 */
@Composable
fun CurrentConnectionContent(
    uiState: CurrentConnectionUiState,
    onGrantPermissionsClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is CurrentConnectionUiState.Loading -> LoadingIndicator()
            is CurrentConnectionUiState.Content -> CellSnapshotDashboard(uiState.snapshot)
            is CurrentConnectionUiState.PermissionsRequiredState ->
                PermissionsRequiredScreen(onGrantPermissionsClick = onGrantPermissionsClick)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = snapshot.cellType.name, style = MaterialTheme.typography.titleLarge)
                    if (snapshot.isCarrierAggregationActive) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Carrier Aggregation") }
                        )
                    }
                }

                Text(text = "Band ${snapshot.band}", style = MaterialTheme.typography.bodyLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LabeledValue(label = "RSRP", value = "${snapshot.rsrp} dBm")
                    LabeledValue(label = "RSRQ", value = "${snapshot.rsrq} dB")
                    LabeledValue(label = "Bandwidth", value = "${snapshot.bandwidthKhz} kHz")
                }
            }
        }

        if (snapshot.secondaryBands.isNotEmpty()) {
            SecondaryBandsSection(secondaryBands = snapshot.secondaryBands)
        }
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

@Composable
private fun PermissionsRequiredScreen(onGrantPermissionsClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "This app needs Location and Phone State permissions to " +
                    "read cellular signal and carrier aggregation info from your " +
                    "device's modem. Without them, the current cell connection " +
                    "cannot be displayed.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onGrantPermissionsClick) {
                Text("Grant Permissions")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentConnectionContentPreview_WithAggregation() {
    MyApplicationTheme {
        CurrentConnectionContent(
            uiState = CurrentConnectionUiState.Content(FakeTelephonyRepository.SAMPLE_SNAPSHOT),
            onGrantPermissionsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentConnectionContentPreview_WithoutAggregation() {
    MyApplicationTheme {
        CurrentConnectionContent(
            uiState = CurrentConnectionUiState.Content(FakeTelephonyRepository.SAMPLE_SNAPSHOT_NO_AGGREGATION),
            onGrantPermissionsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentConnectionContentPreview_NrCell() {
    MyApplicationTheme {
        CurrentConnectionContent(
            uiState = CurrentConnectionUiState.Content(
                CellSnapshot(
                    cellType = CellType.NR,
                    band = "n78",
                    rsrp = -102,
                    rsrq = -13,
                    bandwidthKhz = 100000,
                    isCarrierAggregationActive = false,
                    secondaryBands = emptyList()
                )
            ),
            onGrantPermissionsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentConnectionContentPreview_PermissionsRequired() {
    MyApplicationTheme {
        CurrentConnectionContent(
            uiState = CurrentConnectionUiState.PermissionsRequiredState,
            onGrantPermissionsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentConnectionContentPreview_Loading() {
    MyApplicationTheme {
        CurrentConnectionContent(
            uiState = CurrentConnectionUiState.Loading,
            onGrantPermissionsClick = {}
        )
    }
}
