package com.ferhatozcelik.jetpackcomposetemplate.domain.repository

import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Contract for observing the device's current cellular connection state.
 *
 * Implementations are expected to source emissions from
 * `TelephonyCallback.PhysicalChannelConfigListener` (carrier aggregation /
 * bandwidth) combined with `TelephonyCallback.CellInfoListener` (serving
 * cell identity, band, RSRP/RSRQ), per `.cursor/rules/android-telephony.mdc`
 * and `.cursor/rules/permissions.mdc`. Both listeners require
 * `ACCESS_FINE_LOCATION` and `READ_PHONE_STATE` to be granted at
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
     * [com.ferhatozcelik.jetpackcomposetemplate.domain.model.MissingTelephonyPermissionsException]
     * instead of emitting — collectors must catch this specifically to
     * surface an explicit permissions-required UI state, per
     * `.cursor/rules/permissions.mdc`.
     */
    fun observeCurrentCell(): Flow<CellSnapshot>
}
