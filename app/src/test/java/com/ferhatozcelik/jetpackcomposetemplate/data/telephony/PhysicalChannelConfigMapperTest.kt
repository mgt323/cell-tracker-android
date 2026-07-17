package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyManager
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CarrierAggregationState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhysicalChannelConfigMapperTest {

    private val mapper = PhysicalChannelConfigMapper()

    @Test
    fun mapToCarrierAggregationState_withEmptyList_returnsInactiveState() {
        val result = mapper.mapToCarrierAggregationState(emptyList())

        assertEquals(CarrierAggregationState.Inactive, result)
    }

    @Test
    fun mapToCarrierAggregationState_withSinglePrimaryChannel_returnsInactiveWithBandwidth() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = 20_000
        )

        val result = mapper.mapToCarrierAggregationState(listOf(primary))

        assertFalse(result.isActive)
        assertEquals(20_000, result.primaryBandwidthKhz)
        assertTrue(result.secondaryBands.isEmpty())
    }

    @Test
    fun mapToCarrierAggregationState_withMultipleChannels_returnsActiveWithSecondaryBands() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = 20_000
        )
        val secondaryLte = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            band = 7,
            networkType = TelephonyManager.NETWORK_TYPE_LTE
        )
        val secondaryNr = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            band = 78,
            networkType = TelephonyManager.NETWORK_TYPE_NR
        )

        val result = mapper.mapToCarrierAggregationState(listOf(primary, secondaryLte, secondaryNr))

        assertTrue(result.isActive)
        assertEquals(listOf("B7", "n78"), result.secondaryBands)
    }

    @Test
    fun mapToCarrierAggregationState_withUnknownBandwidthSentinel_mapsToNull() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN
        )

        val result = mapper.mapToCarrierAggregationState(listOf(primary))

        assertNull(result.primaryBandwidthKhz)
    }

    @Test
    fun mapToCarrierAggregationState_withUnknownSecondaryBand_omitsThatBandLabel() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = 20_000
        )
        val secondaryUnknownBand = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            band = PhysicalChannelConfig.BAND_UNKNOWN,
            networkType = TelephonyManager.NETWORK_TYPE_LTE
        )

        val result = mapper.mapToCarrierAggregationState(listOf(primary, secondaryUnknownBand))

        assertTrue(result.isActive)
        assertTrue(result.secondaryBands.isEmpty())
    }

    @Test
    fun mapToCarrierAggregationState_withNoPrimaryFlaggedChannel_usesFirstEntryAsPrimary() {
        val first = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            bandwidthKhz = 10_000
        )
        val second = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            band = 3,
            networkType = TelephonyManager.NETWORK_TYPE_LTE
        )

        val result = mapper.mapToCarrierAggregationState(listOf(first, second))

        assertEquals(10_000, result.primaryBandwidthKhz)
        assertEquals(listOf("B3"), result.secondaryBands)
    }

    private fun config(
        connectionStatus: Int,
        bandwidthKhz: Int = PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN,
        band: Int = PhysicalChannelConfig.BAND_UNKNOWN,
        networkType: Int = TelephonyManager.NETWORK_TYPE_LTE
    ): PhysicalChannelConfig = mockk {
        every { this@mockk.connectionStatus } returns connectionStatus
        every { cellBandwidthDownlinkKhz } returns bandwidthKhz
        every { this@mockk.band } returns band
        every { this@mockk.networkType } returns networkType
    }
}
