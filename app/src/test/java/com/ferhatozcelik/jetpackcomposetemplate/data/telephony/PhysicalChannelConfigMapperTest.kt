package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhysicalChannelConfigMapperTest {

    @Test
    fun map_withEmptyList_returnsInactiveWithUnavailableBandwidth() {
        val result = PhysicalChannelConfigMapper.map(emptyList())

        assertFalse(result.isCarrierAggregationActive)
        assertEquals(CellInfo.UNAVAILABLE, result.bandwidthKhz)
        assertTrue(result.secondaryBands.isEmpty())
    }

    @Test
    fun map_withSinglePrimaryChannel_returnsInactiveWithBandwidth() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = 20_000
        )

        val result = PhysicalChannelConfigMapper.map(listOf(primary))

        assertFalse(result.isCarrierAggregationActive)
        assertEquals(20_000, result.bandwidthKhz)
        assertTrue(result.secondaryBands.isEmpty())
    }

    @Test
    fun map_withMultipleChannels_returnsActiveWithSecondaryBands() {
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

        val result = PhysicalChannelConfigMapper.map(listOf(primary, secondaryLte, secondaryNr))

        assertTrue(result.isCarrierAggregationActive)
        assertEquals(listOf("B7", "n78"), result.secondaryBands)
    }

    @Test
    fun map_withUnknownBandwidthSentinel_mapsToUnavailableSentinel() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = PhysicalChannelConfig.CELL_BANDWIDTH_UNKNOWN
        )

        val result = PhysicalChannelConfigMapper.map(listOf(primary))

        assertEquals(CellInfo.UNAVAILABLE, result.bandwidthKhz)
    }

    @Test
    fun map_withUnknownSecondaryBandSentinel_omitsThatBandLabel() {
        val primary = config(
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            bandwidthKhz = 20_000
        )
        val secondaryUnknownBand = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            band = PhysicalChannelConfig.BAND_UNKNOWN,
            networkType = TelephonyManager.NETWORK_TYPE_LTE
        )

        val result = PhysicalChannelConfigMapper.map(listOf(primary, secondaryUnknownBand))

        assertTrue(result.isCarrierAggregationActive)
        assertTrue(result.secondaryBands.isEmpty())
    }

    @Test
    fun map_withNoPrimaryFlaggedChannel_usesFirstEntryAsPrimary() {
        val first = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            bandwidthKhz = 10_000
        )
        val second = config(
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            band = 3,
            networkType = TelephonyManager.NETWORK_TYPE_LTE
        )

        val result = PhysicalChannelConfigMapper.map(listOf(first, second))

        assertEquals(10_000, result.bandwidthKhz)
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
