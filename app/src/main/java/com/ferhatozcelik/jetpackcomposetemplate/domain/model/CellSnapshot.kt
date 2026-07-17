package com.ferhatozcelik.jetpackcomposetemplate.domain.model

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
 * decoupled from any Android framework telephony type
 * (`CellInfo`/`PhysicalChannelConfig`), per `.cursor/rules/android-telephony.mdc`.
 *
 * @property cellType Radio access technology of the primary serving cell.
 * @property band Band label of the primary serving cell (e.g. "B3", "n78").
 * @property rsrp Reference Signal Received Power, in dBm.
 * @property rsrq Reference Signal Received Quality, in dB.
 * @property bandwidthKhz Channel bandwidth of the primary serving cell, in kHz.
 * @property isCarrierAggregationActive Whether more than one
 *   `PhysicalChannelConfig` entry is active for the connected cell.
 * @property secondaryBands Band labels of any secondary serving cells
 *   contributing to carrier aggregation. Empty when
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
