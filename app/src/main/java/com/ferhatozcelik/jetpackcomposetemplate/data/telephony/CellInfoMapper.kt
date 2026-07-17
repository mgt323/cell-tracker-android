package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType

/**
 * Data-layer piece of
 * [com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot]
 * produced from a raw `CellInfoLte`/`CellInfoNr`, before
 * [TelephonyRepositoryImpl] combines it with [PhysicalChannelConfigMapper]'s
 * carrier-aggregation/bandwidth data to build a complete `CellSnapshot`.
 */
data class PrimaryCellData(
    val cellType: CellType,
    val band: String,
    val rsrp: Int,
    val rsrq: Int
)

/**
 * Pure mapping functions from raw `CellInfo` framework subtypes into
 * [PrimaryCellData]. Sourced from `TelephonyCallback.CellInfoListener`
 * (`onCellInfoChanged`), per `.cursor/rules/android-telephony.mdc`.
 *
 * [TelephonyRepositoryImpl] never invokes these below API 31 (`TelephonyCallback`
 * requires it), so [CellIdentityLte.getBands]/[CellIdentityNr.getBands]
 * (both added in API 30) are always safe to call here despite this
 * project's `minSdk` of 29.
 *
 * ### Sentinel handling
 * [com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot.rsrp]
 * and `.rsrq` are declared as non-nullable `Int` in the fixed domain
 * contract, so an "unavailable" modem reading cannot be represented as
 * `null`. `CellSignalStrengthLte`/`CellSignalStrengthNr` report unavailable
 * RSRP/RSRQ as `Int.MAX_VALUE`. Rather than inventing an ad hoc fallback
 * (e.g. `0`) that would silently misrepresent a real reading, that sentinel
 * is normalized to [CellInfo.UNAVAILABLE] — Android's own named constant for
 * "unknown" (numerically equal to `Integer.MAX_VALUE`) — so callers/UI can
 * reliably check `value == CellInfo.UNAVAILABLE` and know it is documented,
 * not a magic number.
 */
object CellInfoMapper {

    /** Documented "unknown" representation for [PrimaryCellData.band]. */
    const val BAND_UNKNOWN = "unknown"

    /**
     * Finds the primary registered LTE/NR serving cell in [cellInfoList] and
     * maps it to [PrimaryCellData]. Prefers an NR cell when both an LTE and
     * an NR cell are registered (5G NSA). Returns `null` when no registered
     * LTE/NR cell is present (e.g. only GSM/WCDMA cells are registered, or
     * the list is empty) — callers must not emit a `CellSnapshot` in that
     * case.
     */
    fun mapPrimaryCell(cellInfoList: List<CellInfo>): PrimaryCellData? {
        val registered = cellInfoList.filter { it.isRegistered }
        val nr = registered.firstOrNull { it is CellInfoNr } as? CellInfoNr
        if (nr != null) return mapCellInfoNr(nr)
        val lte = registered.firstOrNull { it is CellInfoLte } as? CellInfoLte
        return lte?.let(::mapCellInfoLte)
    }

    fun mapCellInfoLte(cellInfo: CellInfoLte): PrimaryCellData {
        val identity: CellIdentityLte = cellInfo.cellIdentity
        val signal: CellSignalStrengthLte = cellInfo.cellSignalStrength
        return PrimaryCellData(
            cellType = CellType.LTE,
            band = identity.bands.firstOrNull()?.let { "B$it" } ?: BAND_UNKNOWN,
            rsrp = signal.rsrp.toUnavailableSentinelSafe(),
            rsrq = signal.rsrq.toUnavailableSentinelSafe()
        )
    }

    fun mapCellInfoNr(cellInfo: CellInfoNr): PrimaryCellData {
        val identity = cellInfo.cellIdentity as CellIdentityNr
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
        return PrimaryCellData(
            cellType = CellType.NR,
            band = identity.bands.firstOrNull()?.let { "n$it" } ?: BAND_UNKNOWN,
            rsrp = signal.ssRsrp.toUnavailableSentinelSafe(),
            rsrq = signal.ssRsrq.toUnavailableSentinelSafe()
        )
    }

    private fun Int.toUnavailableSentinelSafe(): Int =
        if (this == Int.MAX_VALUE) CellInfo.UNAVAILABLE else this
}
