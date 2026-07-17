package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.os.Build
import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CarrierAggregationState

/**
 * Maps raw [PhysicalChannelConfig] lists (delivered through
 * `TelephonyCallback.PhysicalChannelConfigListener`) into the domain model
 * [CarrierAggregationState], so the framework type never leaks outside
 * `data/telephony` (see `.cursor/rules/android-telephony.mdc`).
 *
 * Carrier aggregation is considered active when more than one physical
 * channel entry is present for the connected cell. Unknown sentinel values
 * ([PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN],
 * [PhysicalChannelConfig.BAND_UNKNOWN]) are mapped to `null`/omitted.
 */
@RequiresApi(Build.VERSION_CODES.S)
class PhysicalChannelConfigMapper {

    fun mapToCarrierAggregationState(configs: List<PhysicalChannelConfig>): CarrierAggregationState {
        if (configs.isEmpty()) return CarrierAggregationState.Inactive

        val primary = configs.firstOrNull {
            it.connectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING
        } ?: configs.first()
        val secondaries = configs.filterNot { it === primary }

        return CarrierAggregationState(
            isActive = configs.size > 1,
            primaryBandwidthKhz = primary.cellBandwidthDownlinkKhz
                .takeUnless { it == PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN },
            secondaryBands = secondaries.mapNotNull(::bandLabel)
        )
    }

    private fun bandLabel(config: PhysicalChannelConfig): String? {
        val band = config.band.takeUnless { it == PhysicalChannelConfig.BAND_UNKNOWN } ?: return null
        return when (config.networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "n$band"
            TelephonyManager.NETWORK_TYPE_LTE -> "B$band"
            else -> null
        }
    }
}
