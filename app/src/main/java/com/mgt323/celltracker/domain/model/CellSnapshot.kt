package com.mgt323.celltracker.domain.model

/**
 * The radio access technology of the currently connected cell.
 */
enum class CellType {
    LTE,
    NR
}

/**
 * A snapshot of the single currently connected cell's radio state — not a
 * history entry. Represents "what is the modem attached to right now",
 * decoupled from Android framework telephony types, per
 * `.cursor/rules/android-telephony.mdc`.
 *
 * Bandwidth and carrier aggregation are derived from `CellInfo` (identity
 * bandwidth + [android.telephony.CellInfo.getCellConnectionStatus]), not
 * from `PhysicalChannelConfig` (which requires privileges unavailable to a
 * normal third-party app).
 *
 * @property cellType Radio access technology of the primary serving cell.
 * @property band Band label of the primary serving cell (e.g. "B3", "n78").
 * @property rsrp Reference Signal Received Power, in dBm.
 * @property rsrq Reference Signal Received Quality, in dB.
 * @property bandwidthKhz Channel bandwidth of the primary serving cell, in kHz
 *   (`CellIdentityLte`/`CellIdentityNr` bandwidth APIs).
 * @property isCarrierAggregationActive `true` when more than one cell in the
 *   latest `CellInfo` list reports
 *   [android.telephony.CellInfo.CONNECTION_PRIMARY_SERVING] or
 *   [android.telephony.CellInfo.CONNECTION_SECONDARY_SERVING].
 * @property secondaryBands Band labels of cells with
 *   [android.telephony.CellInfo.CONNECTION_SECONDARY_SERVING]. Empty when
 *   [isCarrierAggregationActive] is `false`.
 */
data class CellSnapshot(
    val cellType: CellType,
    val band: String,
    val rsrp: Int,
    val rsrq: Int,
    val bandwidthKhz: Int,
    val isCarrierAggregationActive: Boolean,
    val secondaryBands: List<String>
)
