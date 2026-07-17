package com.mgt323.celltracker.domain.repository

import com.mgt323.celltracker.domain.model.CellSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Contract for observing the device's current cellular connection state.
 *
 * Implementations are expected to source emissions from
 * `TelephonyCallback.CellInfoListener` (and optionally
 * `TelephonyManager.requestCellInfoUpdate` for an immediate refresh), mapping
 * serving-cell identity, band, RSRP/RSRQ, bandwidth, and carrier aggregation
 * from `CellInfo` — not from `PhysicalChannelConfigListener`, which requires
 * privileges unavailable to a normal app. Per
 * `.cursor/rules/android-telephony.mdc` and `.cursor/rules/permissions.mdc`,
 * both `ACCESS_FINE_LOCATION` and `READ_PHONE_STATE` must be granted at
 * registration time; implementations must fail gracefully (not throw an
 * uncaught exception) when permissions are missing.
 */
interface TelephonyRepository {

    /**
     * Emits a new [CellSnapshot] whenever the currently connected cell's
     * radio state changes. This reflects only the current state, not a
     * history of past measurements.
     *
     * If `ACCESS_FINE_LOCATION` and/or `READ_PHONE_STATE` are not granted,
     * the returned [Flow] terminates with a
     * [com.mgt323.celltracker.domain.model.MissingTelephonyPermissionsException]
     * instead of emitting — collectors must catch this specifically to
     * surface an explicit permissions-required UI state, per
     * `.cursor/rules/permissions.mdc`.
     */
    fun observeCurrentCell(): Flow<CellSnapshot>
}
