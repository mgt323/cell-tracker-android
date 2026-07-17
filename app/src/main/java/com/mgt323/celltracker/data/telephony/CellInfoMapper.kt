package com.mgt323.celltracker.data.telephony

import android.os.Build
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import androidx.annotation.RequiresApi
import com.mgt323.celltracker.domain.model.CellSnapshot
import com.mgt323.celltracker.domain.model.CellType

/**
 * Pure mapping from raw `CellInfo` lists into [CellSnapshot].
 *
 * Sourced exclusively from `TelephonyCallback.CellInfoListener` /
 * [android.telephony.TelephonyManager.requestCellInfoUpdate] — never from
 * `PhysicalChannelConfigListener`, which requires `READ_PRECISE_PHONE_STATE`
 * or carrier privileges unavailable to a normal third-party app.
 *
 * ### Bandwidth
 * Primary LTE bandwidth comes from [CellIdentityLte.getBandwidth] (kHz).
 * Public [CellIdentityNr] has no bandwidth getter on compileSdk 35, so NR
 * bandwidth is reported as [CellInfo.UNAVAILABLE]. Unavailable LTE values
 * (`Int.MAX_VALUE` / [CellInfo.UNAVAILABLE]) are normalized to
 * [CellInfo.UNAVAILABLE].
 *
 * ### Carrier aggregation
 * Cells with [CellInfo.CONNECTION_PRIMARY_SERVING] or
 * [CellInfo.CONNECTION_SECONDARY_SERVING] are treated as serving.
 * [CellSnapshot.isCarrierAggregationActive] is `true` when more than one
 * such cell is present. [CellSnapshot.secondaryBands] lists band labels of
 * cells with [CellInfo.CONNECTION_SECONDARY_SERVING] only.
 *
 * ### Sentinel handling (RSRP / RSRQ)
 * Unavailable modem readings (`Int.MAX_VALUE`) are normalized to
 * [CellInfo.UNAVAILABLE] so UI can check a single documented sentinel.
 */
object CellInfoMapper {

    /** Documented "unknown" representation for band labels. */
    const val BAND_UNKNOWN = "unknown"

    /**
     * Maps [cellInfoList] into a [CellSnapshot], or `null` when no usable
     * primary LTE/NR serving cell is present.
     *
     * Uses [CellIdentityLte.getBands]/[CellIdentityNr.getBands] (API 30+)
     * and connection-status / bandwidth APIs available by API 28+. Only
     * called from [TelephonyRepositoryImpl] after an API 31+ gate.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun mapCellSnapshot(cellInfoList: List<CellInfo>): CellSnapshot? {
        val primary = findPrimaryCell(cellInfoList) ?: return null
        val primaryData = mapPrimaryCellData(primary)

        val servingCount = cellInfoList.count { cell ->
            val status = cell.cellConnectionStatus
            status == CellInfo.CONNECTION_PRIMARY_SERVING ||
                status == CellInfo.CONNECTION_SECONDARY_SERVING
        }
        val isCarrierAggregationActive = servingCount > 1
        val secondaryBands = if (isCarrierAggregationActive) {
            cellInfoList
                .filter { it.cellConnectionStatus == CellInfo.CONNECTION_SECONDARY_SERVING }
                .mapNotNull(::bandLabel)
        } else {
            emptyList()
        }

        return CellSnapshot(
            cellType = primaryData.cellType,
            band = primaryData.band,
            rsrp = primaryData.rsrp,
            rsrq = primaryData.rsrq,
            bandwidthKhz = primaryData.bandwidthKhz,
            isCarrierAggregationActive = isCarrierAggregationActive,
            secondaryBands = secondaryBands
        )
    }

    /**
     * Prefers an explicit [CellInfo.CONNECTION_PRIMARY_SERVING] LTE/NR cell;
     * falls back to a registered NR then LTE cell (5G NSA / legacy paths).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun findPrimaryCell(cellInfoList: List<CellInfo>): CellInfo? {
        val primaryServing = cellInfoList.filter {
            it.cellConnectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING
        }
        primaryServing.firstOrNull { it is CellInfoNr }?.let { return it }
        primaryServing.firstOrNull { it is CellInfoLte }?.let { return it }

        val registered = cellInfoList.filter { it.isRegistered }
        registered.firstOrNull { it is CellInfoNr }?.let { return it }
        return registered.firstOrNull { it is CellInfoLte }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun mapPrimaryCellData(cellInfo: CellInfo): PrimaryCellData = when (cellInfo) {
        is CellInfoLte -> mapCellInfoLte(cellInfo)
        is CellInfoNr -> mapCellInfoNr(cellInfo)
        else -> error("findPrimaryCell must only return LTE/NR cells")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun mapCellInfoLte(cellInfo: CellInfoLte): PrimaryCellData {
        val identity: CellIdentityLte = cellInfo.cellIdentity
        val signal: CellSignalStrengthLte = cellInfo.cellSignalStrength
        return PrimaryCellData(
            cellType = CellType.LTE,
            band = identity.bands.firstOrNull()?.let { "B$it" } ?: BAND_UNKNOWN,
            rsrp = signal.rsrp.toUnavailableSentinelSafe(),
            rsrq = signal.rsrq.toUnavailableSentinelSafe(),
            bandwidthKhz = identity.bandwidth.toUnavailableSentinelSafe()
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun mapCellInfoNr(cellInfo: CellInfoNr): PrimaryCellData {
        val identity = cellInfo.cellIdentity as CellIdentityNr
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
        return PrimaryCellData(
            cellType = CellType.NR,
            band = identity.bands.firstOrNull()?.let { "n$it" } ?: BAND_UNKNOWN,
            rsrp = signal.ssRsrp.toUnavailableSentinelSafe(),
            rsrq = signal.ssRsrq.toUnavailableSentinelSafe(),
            // Public CellIdentityNr has no getBandwidth()/getChannelBandwidthKhz()
            // on compileSdk 35 (unlike CellIdentityLte.getBandwidth()). Mark unknown.
            bandwidthKhz = CellInfo.UNAVAILABLE
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun bandLabel(cellInfo: CellInfo): String? = when (cellInfo) {
        is CellInfoLte -> cellInfo.cellIdentity.bands.firstOrNull()?.let { "B$it" }
        is CellInfoNr -> (cellInfo.cellIdentity as CellIdentityNr).bands.firstOrNull()?.let { "n$it" }
        else -> null
    }

    private fun Int.toUnavailableSentinelSafe(): Int =
        if (this == Int.MAX_VALUE || this == CellInfo.UNAVAILABLE) CellInfo.UNAVAILABLE else this
}

/**
 * Intermediate primary-cell fields extracted from a single [CellInfoLte] /
 * [CellInfoNr] before carrier-aggregation fields are attached.
 */
data class PrimaryCellData(
    val cellType: CellType,
    val band: String,
    val rsrp: Int,
    val rsrq: Int,
    val bandwidthKhz: Int
)
