package com.ferhatozcelik.jetpackcomposetemplate.domain.exception

/**
 * Signals that [com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository.observeCurrentCell]
 * could not read cellular state because `ACCESS_FINE_LOCATION` and/or
 * `READ_PHONE_STATE` are not granted.
 *
 * Per `.cursor/rules/permissions.mdc`, a missing permission must never
 * crash the app. `data/telephony` implementations must emit this via the
 * `Flow` (e.g. by closing/throwing it from within a `callbackFlow` builder)
 * instead of throwing an uncaught `SecurityException`, letting collectors
 * (typically a ViewModel) catch it and surface an explicit
 * permissions-required UI state rather than a silent no-op or crash.
 *
 * This lives in `domain/exception` (not `data/telephony`) because it is
 * part of the shared `TelephonyRepository` contract: thrown by `data/telephony`
 * and caught by `ui/`, so both layers must depend on the same class.
 *
 * @property missingPermissions The runtime permissions that were not
 *   granted, when known. May be empty (e.g. from a fake/preview repository
 *   that doesn't track specific permission names).
 */
class PermissionMissingException(
    val missingPermissions: List<String> = emptyList(),
    cause: Throwable? = null
) : Exception(
    if (missingPermissions.isEmpty()) {
        "ACCESS_FINE_LOCATION and/or READ_PHONE_STATE permission is not granted."
    } else {
        "Required telephony permissions not granted: ${missingPermissions.joinToString()}"
    },
    cause
)
