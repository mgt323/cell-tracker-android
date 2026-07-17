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
 * registration time; implementations must fail gracefully (not throw) when
 * permissions are missing, surfacing that as an explicit state further up
 * the stack rather than through this Flow.
 */
interface TelephonyRepository {

    /**
     * Emits a new [CellSnapshot] whenever the currently connected cell's
     * radio state changes. This reflects only the current state, not a
     * history of past measurements.
     */
    fun observeCurrentCell(): Flow<CellSnapshot>
}
