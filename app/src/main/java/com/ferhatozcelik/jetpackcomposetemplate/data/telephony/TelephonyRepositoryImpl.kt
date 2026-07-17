package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ferhatozcelik.jetpackcomposetemplate.domain.exception.PermissionMissingException
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CarrierAggregationState
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TelephonyRepository] backed by [TelephonyCallback] (never
 * `PhoneStateListener`), registered via
 * [TelephonyManager.registerTelephonyCallback].
 *
 * - On API 31+ a single callback implements both
 *   [TelephonyCallback.CellInfoListener] (serving cell identity, band,
 *   RSRP/RSRQ) and [TelephonyCallback.PhysicalChannelConfigListener]
 *   (bandwidth, carrier aggregation). An initial
 *   [TelephonyManager.requestCellInfoUpdate] seeds the first emission.
 * - On API 29–30 (`TelephonyCallback` requires API 31) the repository
 *   degrades gracefully: it polls [TelephonyManager.requestCellInfoUpdate]
 *   at a conservative interval, with no carrier-aggregation information.
 *
 * Both `ACCESS_FINE_LOCATION` and `READ_PHONE_STATE` are checked immediately
 * before every telephony call site; a missing permission closes the Flow with
 * a typed [PermissionMissingException] (never an uncaught
 * [SecurityException]), so upstream layers can map it to an explicit
 * permissions-required state.
 */
@Singleton
class TelephonyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val permissionChecker: TelephonyPermissionChecker
) : TelephonyRepository {

    private val cellInfoMapper = CellInfoMapper()

    override fun observeCurrentCell(): Flow<CellSnapshot> = callbackFlow {
        val missingAtStart = permissionChecker.missingPermissions()
        if (missingAtStart.isNotEmpty()) {
            close(PermissionMissingException(missingAtStart))
            awaitClose { }
            return@callbackFlow
        }

        // Callbacks are all dispatched on the main executor, so the mutable
        // combine-state below is only touched from a single thread.
        val executor = ContextCompat.getMainExecutor(context)
        val state = CombineState()

        val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerCallbackOrNull(executor, state)
        } else {
            null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The registered CellInfoListener only fires on changes; request
            // one update up front so collectors get a prompt first emission.
            requestCellInfoUpdateSafely(executor, state)
        } else {
            // No TelephonyCallback below API 31 (and PhoneStateListener is
            // forbidden) — fall back to conservative polling.
            launch {
                while (isActive) {
                    requestCellInfoUpdateSafely(executor, state)
                    delay(FALLBACK_POLL_INTERVAL_MS)
                }
            }
        }

        awaitClose {
            if (telephonyCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.unregisterTelephonyCallback(telephonyCallback)
            }
        }
    }.distinctUntilChanged()

    /**
     * Registers the combined CellInfo + PhysicalChannelConfig callback.
     * Returns `null` (after closing the flow with a typed exception) if
     * permissions are missing or the framework rejects the registration.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun ProducerScope<CellSnapshot>.registerCallbackOrNull(
        executor: Executor,
        state: CombineState
    ): TelephonyCallback? {
        val physicalChannelConfigMapper = PhysicalChannelConfigMapper()
        val callback = object :
            TelephonyCallback(),
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.PhysicalChannelConfigListener {

            override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                state.servingCell = cellInfoMapper.mapToServingCell(cellInfo)
                publish(state)
            }

            override fun onPhysicalChannelConfigChanged(configs: MutableList<PhysicalChannelConfig>) {
                state.carrierAggregation = physicalChannelConfigMapper.mapToCarrierAggregationState(configs)
                publish(state)
            }
        }

        val missing = permissionChecker.missingPermissions()
        if (missing.isNotEmpty()) {
            close(PermissionMissingException(missing))
            return null
        }
        return try {
            telephonyManager.registerTelephonyCallback(executor, callback)
            callback
        } catch (e: SecurityException) {
            // Defensive: never let a SecurityException propagate uncaught.
            close(PermissionMissingException(permissionChecker.missingPermissions(), e))
            null
        }
    }

    /**
     * One-shot cell info refresh. Checks both permissions immediately before
     * the [TelephonyManager.requestCellInfoUpdate] call and closes the flow
     * with a typed [PermissionMissingException] on denial (e.g. revoked
     * mid-collection) instead of throwing.
     */
    private fun ProducerScope<CellSnapshot>.requestCellInfoUpdateSafely(
        executor: Executor,
        state: CombineState
    ) {
        val missing = permissionChecker.missingPermissions()
        if (missing.isNotEmpty()) {
            close(PermissionMissingException(missing))
            return
        }
        try {
            telephonyManager.requestCellInfoUpdate(
                executor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        state.servingCell = cellInfoMapper.mapToServingCell(cellInfo)
                        publish(state)
                    }
                }
            )
        } catch (e: SecurityException) {
            close(PermissionMissingException(permissionChecker.missingPermissions(), e))
        }
    }

    /**
     * Combines the latest serving-cell and carrier-aggregation data into a
     * [CellSnapshot] emission. Because the domain model's fields are
     * non-nullable, snapshots are only emitted once RSRP and RSRQ are
     * actually available (sentinel values were already mapped to `null` by
     * the mappers and are never passed through).
     */
    private fun ProducerScope<CellSnapshot>.publish(state: CombineState) {
        val serving = state.servingCell ?: return
        val rsrp = serving.rsrp ?: return
        val rsrq = serving.rsrq ?: return
        val ca = state.carrierAggregation
        trySend(
            CellSnapshot(
                cellType = serving.cellType,
                band = serving.band ?: BAND_UNKNOWN,
                rsrp = rsrp,
                rsrq = rsrq,
                bandwidthKhz = ca.primaryBandwidthKhz ?: serving.bandwidthKhz ?: BANDWIDTH_UNKNOWN_KHZ,
                isCarrierAggregationActive = ca.isActive,
                secondaryBands = ca.secondaryBands
            )
        )
    }

    /** Latest values from the two listeners, combined on each callback. */
    private class CombineState {
        var servingCell: ServingCell? = null
        var carrierAggregation: CarrierAggregationState = CarrierAggregationState.Inactive
    }

    private companion object {
        /**
         * Poll interval for the API 29–30 fallback. Deliberately conservative
         * to limit battery impact of repeated cell info requests.
         */
        const val FALLBACK_POLL_INTERVAL_MS = 10_000L

        /** Documented "unknown" representation for [CellSnapshot.band]. */
        const val BAND_UNKNOWN = "unknown"

        /**
         * Documented "unknown" representation for [CellSnapshot.bandwidthKhz],
         * forced by the field being non-nullable in the domain model.
         */
        const val BANDWIDTH_UNKNOWN_KHZ = 0
    }
}
