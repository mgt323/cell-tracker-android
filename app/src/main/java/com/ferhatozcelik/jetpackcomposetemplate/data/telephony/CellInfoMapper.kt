package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.os.Build
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType

/**
 * Maps raw `CellInfo` framework objects into the data-layer [ServingCell]
 * model. Modem sentinel values ([CellInfo.UNAVAILABLE] / `Int.MAX_VALUE`) are
 * mapped to `null`, never passed through.
 *
 * @param sdkInt Injected for unit-testability; defaults to the device SDK.
 */
class CellInfoMapper(private val sdkInt: Int = Build.VERSION.SDK_INT) {

    /**
     * Picks the registered LTE/NR serving cell out of [cellInfoList] and maps
     * it. When both an LTE and an NR cell are registered (e.g. 5G NSA), the
     * NR cell is preferred. Returns `null` when no registered LTE/NR cell is
     * present.
     */
    fun mapToServingCell(cellInfoList: List<CellInfo>): ServingCell? {
        val registered = cellInfoList.filter { it.isRegistered }
        val serving = registered.firstOrNull { it is CellInfoNr }
            ?: registered.firstOrNull { it is CellInfoLte }
            ?: return null
        return when (serving) {
            is CellInfoNr -> mapNr(serving)
            is CellInfoLte -> mapLte(serving)
            else -> null
        }
    }

    private fun mapLte(cellInfo: CellInfoLte): ServingCell {
        val identity = cellInfo.cellIdentity
        val signal = cellInfo.cellSignalStrength
        return ServingCell(
            cellType = CellType.LTE,
            band = lteBandLabel(identity),
            rsrp = signal.rsrp.sanitized(),
            rsrq = signal.rsrq.sanitized(),
            bandwidthKhz = identity.bandwidth.sanitized()
        )
    }

    private fun mapNr(cellInfo: CellInfoNr): ServingCell {
        val identity = cellInfo.cellIdentity as CellIdentityNr
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
        return ServingCell(
            cellType = CellType.NR,
            band = nrBandLabel(identity),
            rsrp = signal.ssRsrp.sanitized(),
            rsrq = signal.ssRsrq.sanitized(),
            // CellIdentityNr does not expose channel bandwidth; on API 31+ it
            // comes from PhysicalChannelConfig instead.
            bandwidthKhz = null
        )
    }

    private fun lteBandLabel(identity: CellIdentityLte): String? {
        if (sdkInt >= Build.VERSION_CODES.R) {
            identity.bands.firstOrNull()?.let { return "B$it" }
        }
        return identity.earfcn.sanitized()?.let(::lteBandFromEarfcn)
    }

    private fun nrBandLabel(identity: CellIdentityNr): String? {
        if (sdkInt >= Build.VERSION_CODES.R) {
            identity.bands.firstOrNull()?.let { return "n$it" }
        }
        // NR-ARFCN ranges overlap across bands, so no reliable fallback below API 30.
        return null
    }

    /**
     * Best-effort EARFCN-to-band fallback for API 29 devices (where
     * `CellIdentityLte.getBands()` is unavailable), covering common European
     * LTE bands. Returns `null` for EARFCNs outside the known ranges.
     */
    private fun lteBandFromEarfcn(earfcn: Int): String? = when (earfcn) {
        in 0..599 -> "B1"
        in 1200..1949 -> "B3"
        in 2750..3449 -> "B7"
        in 3450..3799 -> "B8"
        in 6150..6449 -> "B20"
        in 9210..9659 -> "B28"
        in 37750..38249 -> "B38"
        in 38650..39649 -> "B40"
        else -> null
    }

    private fun Int.sanitized(): Int? = takeUnless { it == CellInfo.UNAVAILABLE }
}
