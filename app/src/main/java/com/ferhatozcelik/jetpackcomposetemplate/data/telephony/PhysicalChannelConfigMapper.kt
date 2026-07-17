package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyManager

/**
 * Data-layer carrier-aggregation/bandwidth piece of
 * [com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot],
 * produced from the raw `PhysicalChannelConfig` list delivered through
 * `TelephonyCallback.PhysicalChannelConfigListener`
 * (`onPhysicalChannelConfigChanged`).
 */
data class CarrierAggregationData(
    val bandwidthKhz: Int,
    val isCarrierAggregationActive: Boolean,
    val secondaryBands: List<String>
)

/**
 * Pure mapping functions from raw [PhysicalChannelConfig] lists into
 * [CarrierAggregationData]. Carrier aggregation is active whenever more
 * than one [PhysicalChannelConfig] entry is reported for the connected
 * cell (i.e. at least one secondary serving channel besides the primary),
 * per `.cursor/rules/android-telephony.mdc`.
 *
 * ### Sentinel handling
 * [com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot.bandwidthKhz]
 * is non-nullable `Int`. [PhysicalChannelConfig.getCellBandwidthDownlinkKhz]
 * (the bandwidth source, matching the convention documented on
 * `data/local/CellMeasurement.kt`) reports
 * [PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN] (`Integer.MAX_VALUE`) when
 * unavailable; this is normalized to [CellInfo.UNAVAILABLE] so the whole
 * `CellSnapshot` uses one consistent "unknown" `Int` sentinel across
 * `rsrp`/`rsrq` (see [CellInfoMapper]) and `bandwidthKhz`, rather than
 * silently passing the raw magic number through unlabeled.
 */
object PhysicalChannelConfigMapper {

    /**
     * Maps [configs] (all physical channels for the connected cell) into
     * [CarrierAggregationData]. The primary channel is the one flagged
     * [CellInfo.CONNECTION_PRIMARY_SERVING], falling back to the first entry
     * if none is flagged. [CarrierAggregationData.secondaryBands] is derived
     * from every other (non-primary) entry.
     */
    fun map(configs: List<PhysicalChannelConfig>): CarrierAggregationData {
        if (configs.isEmpty()) {
            return CarrierAggregationData(
                bandwidthKhz = CellInfo.UNAVAILABLE,
                isCarrierAggregationActive = false,
                secondaryBands = emptyList()
            )
        }

        val primary = configs.firstOrNull { it.connectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING }
            ?: configs.first()
        val secondaryBands = configs.filterNot { it === primary }.mapNotNull(::bandLabel)

        return CarrierAggregationData(
            bandwidthKhz = primary.cellBandwidthDownlinkKhz.toUnavailableSentinelSafe(),
            isCarrierAggregationActive = configs.size > 1,
            secondaryBands = secondaryBands
        )
    }

    /** Best-effort band label for a secondary channel; `null` when the band is unreported. */
    private fun bandLabel(config: PhysicalChannelConfig): String? {
        if (config.band == PhysicalChannelConfig.BAND_UNKNOWN) return null
        return when (config.networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "n${config.band}"
            TelephonyManager.NETWORK_TYPE_LTE -> "B${config.band}"
            else -> null
        }
    }

    private fun Int.toUnavailableSentinelSafe(): Int =
        if (this == PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN) CellInfo.UNAVAILABLE else this
}
