package com.mgt323.celltracker.domain.model

/**
 * Signals that [com.mgt323.celltracker.domain.repository.TelephonyRepository.observeCurrentCell]
 * could not (re)register its telephony callbacks because `ACCESS_FINE_LOCATION`
 * and/or `READ_PHONE_STATE` are not granted.
 *
 * This is the agreed-upon way `data/telephony` communicates permission
 * denial up through the `Flow<CellSnapshot>` contract without changing its
 * declared type: implementations should terminate the `callbackFlow` with
 * this exception (e.g. via `close(MissingTelephonyPermissionsException())`)
 * instead of registering a callback, and collectors (ViewModels) should
 * catch it specifically to surface an explicit permissions-required UI
 * state, per `.cursor/rules/permissions.mdc`. It must never be allowed to
 * escape as an uncaught exception/crash.
 */
class MissingTelephonyPermissionsException :
    Exception("ACCESS_FINE_LOCATION and READ_PHONE_STATE must both be granted to observe cellular state.")
