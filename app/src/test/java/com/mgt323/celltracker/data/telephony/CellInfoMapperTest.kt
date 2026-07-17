package com.mgt323.celltracker.data.telephony

import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import com.mgt323.celltracker.domain.model.CellType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CellInfoMapperTest {

    // region LTE primary mapping

    @Test
    fun mapCellInfoLte_withValidReading_returnsCorrectDomainModel() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf(3), bandwidthKhz = 20_000)

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(
            PrimaryCellData(
                cellType = CellType.LTE,
                band = "B3",
                rsrp = -95,
                rsrq = -12,
                bandwidthKhz = 20_000
            ),
            result
        )
    }

    @Test
    fun mapCellInfoLte_withMaxIntRsrp_mapsToUnavailableSentinel() {
        val cellInfo = lteCellInfo(rsrp = Int.MAX_VALUE, rsrq = -12, bands = intArrayOf(3))

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(CellInfo.UNAVAILABLE, result.rsrp)
        assertEquals(-12, result.rsrq)
    }

    @Test
    fun mapCellInfoLte_withUnavailableBandwidth_mapsToUnavailableSentinel() {
        val cellInfo = lteCellInfo(
            rsrp = -95,
            rsrq = -12,
            bands = intArrayOf(3),
            bandwidthKhz = CellInfo.UNAVAILABLE
        )

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(CellInfo.UNAVAILABLE, result.bandwidthKhz)
    }

    @Test
    fun mapCellInfoLte_withEmptyBandsArray_returnsUnknownBand() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf())

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(CellInfoMapper.BAND_UNKNOWN, result.band)
    }

    // endregion

    // region NR primary mapping

    @Test
    fun mapCellInfoNr_withValidReading_returnsCorrectDomainModel() {
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf(78))

        val result = CellInfoMapper.mapCellInfoNr(cellInfo)

        assertEquals(
            PrimaryCellData(
                cellType = CellType.NR,
                band = "n78",
                rsrp = -100,
                rsrq = -11,
                bandwidthKhz = CellInfo.UNAVAILABLE
            ),
            result
        )
    }

    @Test
    fun mapCellInfoNr_withMaxIntSsRsrp_mapsToUnavailableSentinel() {
        val cellInfo = nrCellInfo(ssRsrp = Int.MAX_VALUE, ssRsrq = -11, bands = intArrayOf(78))

        val result = CellInfoMapper.mapCellInfoNr(cellInfo)

        assertEquals(CellInfo.UNAVAILABLE, result.rsrp)
        assertEquals(-11, result.rsrq)
    }

    @Test
    fun mapCellInfoNr_withEmptyBandsArray_returnsUnknownBand() {
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf())

        val result = CellInfoMapper.mapCellInfoNr(cellInfo)

        assertEquals(CellInfoMapper.BAND_UNKNOWN, result.band)
    }

    // endregion

    // region snapshot / CA

    @Test
    fun mapCellSnapshot_withNoUsablePrimary_returnsNull() {
        val unregistered = lteCellInfo(
            rsrp = -95,
            rsrq = -12,
            bands = intArrayOf(3),
            registered = false,
            connectionStatus = CellInfo.CONNECTION_NONE
        )

        assertNull(CellInfoMapper.mapCellSnapshot(listOf(unregistered)))
        assertNull(CellInfoMapper.mapCellSnapshot(emptyList()))
    }

    @Test
    fun mapCellSnapshot_withSinglePrimaryServing_returnsInactiveCa() {
        val primary = lteCellInfo(
            rsrp = -95,
            rsrq = -12,
            bands = intArrayOf(3),
            bandwidthKhz = 20_000,
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING
        )

        val result = CellInfoMapper.mapCellSnapshot(listOf(primary))

        requireNotNull(result)
        assertEquals(CellType.LTE, result.cellType)
        assertEquals("B3", result.band)
        assertEquals(20_000, result.bandwidthKhz)
        assertFalse(result.isCarrierAggregationActive)
        assertTrue(result.secondaryBands.isEmpty())
    }

    @Test
    fun mapCellSnapshot_withPrimaryAndSecondaryServing_returnsActiveCaAndSecondaryBands() {
        val primary = lteCellInfo(
            rsrp = -95,
            rsrq = -12,
            bands = intArrayOf(3),
            bandwidthKhz = 20_000,
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING
        )
        val secondaryLte = lteCellInfo(
            rsrp = -100,
            rsrq = -14,
            bands = intArrayOf(7),
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING
        )
        val secondaryNr = nrCellInfo(
            ssRsrp = -90,
            ssRsrq = -10,
            bands = intArrayOf(78),
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING
        )

        val result = CellInfoMapper.mapCellSnapshot(listOf(primary, secondaryLte, secondaryNr))

        requireNotNull(result)
        assertTrue(result.isCarrierAggregationActive)
        assertEquals(listOf("B7", "n78"), result.secondaryBands)
        assertEquals(20_000, result.bandwidthKhz)
        assertEquals("B3", result.band)
    }

    @Test
    fun mapCellSnapshot_withPrimaryServingNrAndRegisteredLte_prefersPrimaryServingNr() {
        val lte = lteCellInfo(
            rsrp = -95,
            rsrq = -12,
            bands = intArrayOf(3),
            connectionStatus = CellInfo.CONNECTION_NONE,
            registered = true
        )
        val nr = nrCellInfo(
            ssRsrp = -100,
            ssRsrq = -11,
            bands = intArrayOf(78),
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING
        )

        val result = CellInfoMapper.mapCellSnapshot(listOf(lte, nr))

        assertEquals(CellType.NR, result?.cellType)
        assertEquals("n78", result?.band)
        assertEquals(CellInfo.UNAVAILABLE, result?.bandwidthKhz)
    }

    // endregion

    private fun lteCellInfo(
        rsrp: Int,
        rsrq: Int,
        bands: IntArray = intArrayOf(),
        bandwidthKhz: Int = 20_000,
        registered: Boolean = true,
        connectionStatus: Int = CellInfo.CONNECTION_PRIMARY_SERVING
    ): CellInfoLte {
        val identity = mockk<CellIdentityLte> {
            every { this@mockk.bands } returns bands
            every { bandwidth } returns bandwidthKhz
        }
        val signal = mockk<CellSignalStrengthLte> {
            every { this@mockk.rsrp } returns rsrp
            every { this@mockk.rsrq } returns rsrq
        }
        return mockk {
            every { isRegistered } returns registered
            every { cellConnectionStatus } returns connectionStatus
            every { cellIdentity } returns identity
            every { cellSignalStrength } returns signal
        }
    }

    private fun nrCellInfo(
        ssRsrp: Int,
        ssRsrq: Int,
        bands: IntArray = intArrayOf(),
        registered: Boolean = true,
        connectionStatus: Int = CellInfo.CONNECTION_PRIMARY_SERVING
    ): CellInfoNr {
        val identity = mockk<CellIdentityNr> {
            every { this@mockk.bands } returns bands
        }
        val signal = mockk<CellSignalStrengthNr> {
            every { this@mockk.ssRsrp } returns ssRsrp
            every { this@mockk.ssRsrq } returns ssRsrq
        }
        return mockk {
            every { isRegistered } returns registered
            every { cellConnectionStatus } returns connectionStatus
            every { cellIdentity } returns identity
            every { cellSignalStrength } returns signal
        }
    }
}
