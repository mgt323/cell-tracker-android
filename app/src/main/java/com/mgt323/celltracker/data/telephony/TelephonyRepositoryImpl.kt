package com.mgt323.celltracker.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.mgt323.celltracker.domain.model.CellSnapshot
import com.mgt323.celltracker.domain.model.MissingTelephonyPermissionsException
import com.mgt323.celltracker.domain.repository.TelephonyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TelephonyRepository] backed by `TelephonyCallback.CellInfoListener` and
 * [TelephonyManager.requestCellInfoUpdate] (never `PhoneStateListener`, and
 * never `PhysicalChannelConfigListener` — the latter requires
 * `READ_PRECISE_PHONE_STATE` / carrier privileges that a normal app cannot
 * hold). Bandwidth and carrier aggregation are derived from `CellInfo` via
 * [CellInfoMapper].
 *
 * `TelephonyCallback` requires API 31 (`Build.VERSION_CODES.S`), while this
 * project's `minSdk` is 29. Below API 31 the returned [Flow] closes with
 * [MissingTelephonyPermissionsException].
 *
 * Both `ACCESS_FINE_LOCATION` and `READ_PHONE_STATE` are checked immediately
 * before every telephony call site via [ContextCompat.checkSelfPermission],
 * per `.cursor/rules/permissions.mdc` and `.cursor/rules/android-telephony.mdc`.
 */
@Singleton
class TelephonyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TelephonyRepository {

    override fun observeCurrentCell(): Flow<CellSnapshot> = callbackFlow {
        val permissionsGranted = hasRequiredPermissions()
        val apiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        // TEMP: CellTrackerDebug — remove once permission gating is verified
        Log.d(
            DEBUG_TAG,
            "permissionsGranted=$permissionsGranted apiSupported=$apiSupported " +
                "(sdk=${Build.VERSION.SDK_INT})"
        )

        val telephonyManager = if (permissionsGranted && apiSupported) {
            context.getSystemService(TelephonyManager::class.java)
        } else {
            null
        }

        val registeredCallback = when {
            !permissionsGranted -> {
                // TEMP: CellTrackerDebug — remove once permission gating is verified
                Log.d(DEBUG_TAG, "closing with MissingTelephonyPermissionsException (permissions denied)")
                close(MissingTelephonyPermissionsException())
                null
            }
            !apiSupported -> {
                // TEMP: CellTrackerDebug — remove once permission gating is verified
                Log.d(DEBUG_TAG, "closing with MissingTelephonyPermissionsException (API < 31)")
                close(MissingTelephonyPermissionsException())
                null
            }
            telephonyManager == null -> {
                // TEMP: CellTrackerDebug — remove once permission gating is verified
                Log.d(DEBUG_TAG, "closing with MissingTelephonyPermissionsException (TelephonyManager null)")
                close(MissingTelephonyPermissionsException())
                null
            }
            else -> registerCallbackOrNull(telephonyManager)
        }

        awaitClose {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                telephonyManager != null &&
                registeredCallback != null
            ) {
                unregisterCallback(telephonyManager, registeredCallback)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun unregisterCallback(
        telephonyManager: TelephonyManager,
        callback: TelephonyCallback
    ) {
        telephonyManager.unregisterTelephonyCallback(callback)
    }

    /**
     * Registers [TelephonyCallback.CellInfoListener], requests an immediate
     * cell-info update, and returns the callback — or `null` after closing
     * with [MissingTelephonyPermissionsException] if registration fails with
     * [SecurityException].
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun ProducerScope<CellSnapshot>.registerCallbackOrNull(
        telephonyManager: TelephonyManager
    ): TelephonyCallback? {
        fun publish(cellInfo: List<CellInfo>) {
            val snapshot = CellInfoMapper.mapCellSnapshot(cellInfo) ?: return
            trySend(snapshot)
        }

        val callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
            override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                publish(cellInfo.toList())
            }
        }

        return try {
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            // Kick an immediate update; result is also delivered via CellInfoListener
            // on most devices, and via CellInfoCallback as a belt-and-suspenders path.
            if (hasRequiredPermissions()) {
                telephonyManager.requestCellInfoUpdate(
                    context.mainExecutor,
                    object : TelephonyManager.CellInfoCallback() {
                        override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                            publish(cellInfo.toList())
                        }
                    }
                )
            }
            callback
        } catch (e: SecurityException) {
            // TEMP: CellTrackerDebug — remove once permission gating is verified
            Log.e(DEBUG_TAG, "SecurityException details", e)
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

    companion object {
        // TEMP: CellTrackerDebug — remove once permission gating is verified
        private const val DEBUG_TAG = "CellTrackerDebug"
    }
}
