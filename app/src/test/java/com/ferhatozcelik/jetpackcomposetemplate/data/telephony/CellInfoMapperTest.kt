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

    // region LTE

    @Test
    fun mapCellInfoLte_withValidReading_returnsCorrectDomainModel() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf(3))

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(
            PrimaryCellData(cellType = CellType.LTE, band = "B3", rsrp = -95, rsrq = -12),
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
    fun mapCellInfoLte_withUnavailableRsrq_mapsToUnavailableSentinel() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = CellInfo.UNAVAILABLE, bands = intArrayOf(3))

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(-95, result.rsrp)
        assertEquals(CellInfo.UNAVAILABLE, result.rsrq)
    }

    @Test
    fun mapCellInfoLte_withEmptyBandsArray_returnsUnknownBand() {
        val cellInfo = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf())

        val result = CellInfoMapper.mapCellInfoLte(cellInfo)

        assertEquals(CellInfoMapper.BAND_UNKNOWN, result.band)
    }

    // endregion

    // region NR

    @Test
    fun mapCellInfoNr_withValidReading_returnsCorrectDomainModel() {
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf(78))

        val result = CellInfoMapper.mapCellInfoNr(cellInfo)

        assertEquals(
            PrimaryCellData(cellType = CellType.NR, band = "n78", rsrp = -100, rsrq = -11),
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
    fun mapCellInfoNr_withUnavailableSsRsrq_mapsToUnavailableSentinel() {
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = CellInfo.UNAVAILABLE, bands = intArrayOf(78))

        val result = CellInfoMapper.mapCellInfoNr(cellInfo)

        assertEquals(-100, result.rsrp)
        assertEquals(CellInfo.UNAVAILABLE, result.rsrq)
    }

    @Test
    fun mapCellInfoNr_withEmptyBandsArray_returnsUnknownBand() {
        val cellInfo = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf())

        val result = CellInfoMapper.mapCellInfoNr(cellInfo)

        assertEquals(CellInfoMapper.BAND_UNKNOWN, result.band)
    }

    // endregion

    // region primary-cell selection

    @Test
    fun mapPrimaryCell_withNoRegisteredCell_returnsNull() {
        val unregistered = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf(3), registered = false)

        assertNull(CellInfoMapper.mapPrimaryCell(listOf(unregistered)))
        assertNull(CellInfoMapper.mapPrimaryCell(emptyList()))
    }

    @Test
    fun mapPrimaryCell_withRegisteredLteAndNrCells_prefersNrCell() {
        val lte = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf(3))
        val nr = nrCellInfo(ssRsrp = -100, ssRsrq = -11, bands = intArrayOf(78))

        val result = CellInfoMapper.mapPrimaryCell(listOf(lte, nr))

        assertEquals(CellType.NR, result?.cellType)
    }

    @Test
    fun mapPrimaryCell_withOnlyRegisteredLteCell_returnsLteDomainModel() {
        val lte = lteCellInfo(rsrp = -95, rsrq = -12, bands = intArrayOf(3))

        val result = CellInfoMapper.mapPrimaryCell(listOf(lte))

        assertEquals(
            PrimaryCellData(cellType = CellType.LTE, band = "B3", rsrp = -95, rsrq = -12),
            result
        )
    }

    // endregion

    private fun lteCellInfo(
        rsrp: Int,
        rsrq: Int,
        bands: IntArray = intArrayOf(),
        registered: Boolean = true
    ): CellInfoLte {
        val identity = mockk<CellIdentityLte> {
            every { this@mockk.bands } returns bands
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
}
