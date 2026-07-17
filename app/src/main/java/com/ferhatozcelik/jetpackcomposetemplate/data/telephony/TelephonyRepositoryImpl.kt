package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.MissingTelephonyPermissionsException
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TelephonyRepository] backed by `TelephonyCallback` (never
 * `PhoneStateListener`, which is banned by `.cursor/rules/android-telephony.mdc`),
 * registered via [TelephonyManager.registerTelephonyCallback]. A single
 * callback implements both [TelephonyCallback.CellInfoListener] (serving
 * cell identity, band, RSRP/RSRQ, via [CellInfoMapper]) and
 * [TelephonyCallback.PhysicalChannelConfigListener] (bandwidth, carrier
 * aggregation, via [PhysicalChannelConfigMapper]); the two are combined into
 * a [CellSnapshot] on every callback firing.
 *
 * `TelephonyCallback` requires API 31 (`Build.VERSION_CODES.S`), while this
 * project's `minSdk` is 29. Since `PhoneStateListener` cannot be used as a
 * fallback, telephony observation is simply unsupported below API 31: the
 * returned [Flow] closes with [MissingTelephonyPermissionsException] on
 * those devices — the same signal used for an actual permission denial,
 * per that exception's documented contract.
 *
 * Both `ACCESS_FINE_LOCATION` and `READ_PHONE_STATE` are checked immediately
 * before every telephony call site via [ContextCompat.checkSelfPermission],
 * per `.cursor/rules/permissions.mdc` and `.cursor/rules/android-telephony.mdc`
 * — never assumed from a manifest declaration alone. A missing permission
 * (initially, or revoked mid-collection) closes the flow with
 * [MissingTelephonyPermissionsException] instead of throwing an uncaught
 * `SecurityException`.
 */
@Singleton
class TelephonyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TelephonyRepository {

    override fun observeCurrentCell(): Flow<CellSnapshot> = callbackFlow {
        val permissionsGranted = hasRequiredPermissions()
        val apiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val telephonyManager = if (permissionsGranted && apiSupported) {
            context.getSystemService(TelephonyManager::class.java)
        } else {
            null
        }

        val registeredCallback = when {
            !permissionsGranted -> {
                close(MissingTelephonyPermissionsException())
                null
            }
            !apiSupported -> {
                // TelephonyCallback requires API 31+; PhoneStateListener is
                // banned (`.cursor/rules/android-telephony.mdc`), so there is
                // no supported way to observe cellular state below API 31 in
                // this project.
                close(MissingTelephonyPermissionsException())
                null
            }
            telephonyManager == null -> {
                close(MissingTelephonyPermissionsException())
                null
            }
            else -> registerCallbackOrNull(telephonyManager)
        }

        awaitClose {
            if (telephonyManager != null && registeredCallback != null) {
                telephonyManager.unregisterTelephonyCallback(registeredCallback)
            }
        }
    }

    /**
     * Registers the combined `CellInfo` + `PhysicalChannelConfig` callback
     * and returns it, or `null` (after closing the flow with
     * [MissingTelephonyPermissionsException]) if the framework rejects the
     * registration with a [SecurityException] — e.g. a permission revoked in
     * a TOCTOU race between the check in [observeCurrentCell] and this call.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun ProducerScope<CellSnapshot>.registerCallbackOrNull(
        telephonyManager: TelephonyManager
    ): TelephonyCallback? {
        var latestCellInfo: List<CellInfo> = emptyList()
        var latestConfigs: List<PhysicalChannelConfig> = emptyList()

        fun publishIfPossible() {
            val primaryCellData = CellInfoMapper.mapPrimaryCell(latestCellInfo) ?: return
            val carrierAggregation = PhysicalChannelConfigMapper.map(latestConfigs)
            trySend(
                CellSnapshot(
                    cellType = primaryCellData.cellType,
                    band = primaryCellData.band,
                    rsrp = primaryCellData.rsrp,
                    rsrq = primaryCellData.rsrq,
                    bandwidthKhz = carrierAggregation.bandwidthKhz,
                    isCarrierAggregationActive = carrierAggregation.isCarrierAggregationActive,
                    secondaryBands = carrierAggregation.secondaryBands
                )
            )
        }

        val callback = object :
            TelephonyCallback(),
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.PhysicalChannelConfigListener {

            override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                latestCellInfo = cellInfo.toList()
                publishIfPossible()
            }

            override fun onPhysicalChannelConfigChanged(configs: MutableList<PhysicalChannelConfig>) {
                latestConfigs = configs.toList()
                publishIfPossible()
            }
        }

        return try {
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            callback
        } catch (e: SecurityException) {
            // Defensive: never let a SecurityException escape uncaught.
            close(MissingTelephonyPermissionsException())
            null
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return hasFineLocation && hasPhoneState
    }
}
