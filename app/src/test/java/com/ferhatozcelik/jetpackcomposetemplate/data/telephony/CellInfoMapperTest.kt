package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CellInfoMapperTest {

    private val mapper = CellInfoMapper(sdkInt = API_34)

    // region LTE

    @Test
    fun mapToServingCell_withRegisteredLteCell_returnsLteDomainModel() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bandwidthKhz = 20_000, bands = intArrayOf(3))

        val result = mapper.mapToServingCell(listOf(cellInfo))

        assertEquals(
            ServingCell(cellType = CellType.LTE, band = "B3", rsrp = -95, rsrq = -12, bandwidthKhz = 20_000),
            result
        )
    }

    @Test
    fun mapToServingCell_withUnavailableLteRsrp_mapsSentinelToNull() {
        val cellInfo = lteCellInfo(rsrp = CellInfo.UNAVAILABLE, rsrq = -12, bandwidthKhz = 20_000, bands = intArrayOf(3))

        val result = mapper.mapToServingCell(listOf(cellInfo))

        assertNull(result?.rsrp)
        assertEquals(-12, result?.rsrq)
    }

    @Test
    fun mapToServingCell_withUnavailableLteRsrqAndBandwidth_mapsSentinelsToNull() {
        val cellInfo = lteCellInfo(
            rsrp = -95,
            rsrq = CellInfo.UNAVAILABLE,
            bandwidthKhz = CellInfo.UNAVAILABLE,
            bands = intArrayOf(3)
        )

        val result = mapper.mapToServingCell(listOf(cellInfo))

        assertNull(result?.rsrq)
        assertNull(result?.bandwidthKhz)
        assertEquals(-95, result?.rsrp)
    }

    @Test
    fun mapToServingCell_onApi29WithKnownEarfcn_derivesBandFromEarfcn() {
        val api29Mapper = CellInfoMapper(sdkInt = API_29)
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bandwidthKhz = 20_000, earfcn = 1300)

        val result = api29Mapper.mapToServingCell(listOf(cellInfo))

        assertEquals("B3", result?.band)
    }

    @Test
    fun mapToServingCell_onApi29WithUnavailableEarfcn_returnsNullBand() {
        val api29Mapper = CellInfoMapper(sdkInt = API_29)
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bandwidthKhz = 20_000, earfcn = CellInfo.UNAVAILABLE)

        val result = api29Mapper.mapToServingCell(listOf(cellInfo))

        assertNull(result?.band)
    }

    @Test
    fun mapToServingCell_withEmptyLteBandsArray_fallsBackToEarfcn() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bandwidthKhz = 20_000, bands = intArrayOf(), earfcn = 6300)

        val result = mapper.mapToServingCell(listOf(cellInfo))

        assertEquals("B20", result?.band)
    }

    // endregion

    // region NR

    @Test
    fun mapToServingCell_withRegisteredNrCell_returnsNrDomainModel() {
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf(78))

        val result = mapper.mapToServingCell(listOf(cellInfo))

        assertEquals(
            ServingCell(cellType = CellType.NR, band = "n78", rsrp = -100, rsrq = -11, bandwidthKhz = null),
            result
        )
    }

    @Test
    fun mapToServingCell_withUnavailableNrSsRsrpAndSsRsrq_mapsSentinelsToNull() {
        val cellInfo = nrCellInfo(ssRsrp = CellInfo.UNAVAILABLE, ssRsrq = CellInfo.UNAVAILABLE, bands = intArrayOf(78))

        val result = mapper.mapToServingCell(listOf(cellInfo))

        assertNull(result?.rsrp)
        assertNull(result?.rsrq)
    }

    @Test
    fun mapToServingCell_onApi29WithNrCell_returnsNullBand() {
        val api29Mapper = CellInfoMapper(sdkInt = API_29)
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf(78))

        val result = api29Mapper.mapToServingCell(listOf(cellInfo))

        assertNull(result?.band)
    }

    // endregion

    // region serving-cell selection

    @Test
    fun mapToServingCell_withNoRegisteredCell_returnsNull() {
        val unregistered = lteCellInfo(rsrp = -95, rsrq = -12, bandwidthKhz = 20_000, registered = false)

        assertNull(mapper.mapToServingCell(listOf(unregistered)))
        assertNull(mapper.mapToServingCell(emptyList()))
    }

    @Test
    fun mapToServingCell_withRegisteredLteAndNrCells_prefersNrCell() {
        val lte = lteCellInfo(rsrp = -95, rsrq = -12, bandwidthKhz = 20_000, bands = intArrayOf(3))
        val nr = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf(78))

        val result = mapper.mapToServingCell(listOf(lte, nr))

        assertEquals(CellType.NR, result?.cellType)
    }

    // endregion

    private fun lteCellInfo(
        rsrp: Int,
        rsrq: Int,
        bandwidthKhz: Int,
        bands: IntArray = intArrayOf(),
        earfcn: Int = CellInfo.UNAVAILABLE,
        registered: Boolean = true
    ): CellInfoLte {
        val identity = mockk<CellIdentityLte> {
            every { this@mockk.bands } returns bands
            every { this@mockk.earfcn } returns earfcn
            every { bandwidth } returns bandwidthKhz
        }
        val signal = mockk<CellSignalStrengthLte> {
            every { this@mockk.rsrp } returns rsrp
            every { this@mockk.rsrq } returns rsrq
        }
        return mockk {
            every { isRegistered } returns registered
            every { cellIdentity } returns identity
            every { cellSignalStrength } returns signal
        }
    }

    private fun nrCellInfo(
        ssRsrp: Int,
        ssRsrq: Int,
        bands: IntArray = intArrayOf(),
        registered: Boolean = true
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
            every { cellIdentity } returns identity
            every { cellSignalStrength } returns signal
        }
    }

    private companion object {
        const val API_29 = 29
        const val API_34 = 34
    }
}
