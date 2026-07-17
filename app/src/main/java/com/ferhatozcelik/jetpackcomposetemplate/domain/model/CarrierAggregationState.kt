package com.ferhatozcelik.jetpackcomposetemplate.domain.model

/**
 * Domain representation of the carrier-aggregation situation of the currently
 * connected cell, decoupled from `android.telephony.PhysicalChannelConfig`
 * per `.cursor/rules/android-telephony.mdc`.
 *
 * @property isActive `true` when more than one physical channel is configured
 *   for the connected cell (i.e. at least one secondary serving cell).
 * @property primaryBandwidthKhz Downlink bandwidth of the primary serving
 *   channel in kHz, or `null` when the modem reports it as unknown.
 * @property secondaryBands Band labels (e.g. "B3", "n78") of the secondary
 *   serving channels. Empty when [isActive] is `false` or the bands are
 *   unknown.
 */
data class CarrierAggregationState(
    val isActive: Boolean,
    val primaryBandwidthKhz: Int?,
    val secondaryBands: List<String>
) {
    companion object {
        /** State used when no physical channel information is available. */
        val Inactive = CarrierAggregationState(
            isActive = false,
            primaryBandwidthKhz = null,
            secondaryBands = emptyList()
        )
    }
}
