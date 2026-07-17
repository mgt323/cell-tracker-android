package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Strict runtime permission check that must run immediately before every
 * `TelephonyManager` / `TelephonyCallback` / `CellInfo` call site, per
 * `.cursor/rules/permissions.mdc`. Both `ACCESS_FINE_LOCATION` and
 * `READ_PHONE_STATE` are required on API 29+ — a manifest declaration alone
 * is never sufficient.
 */
class TelephonyPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Returns the subset of [REQUIRED_PERMISSIONS] that is currently not granted. */
    fun missingPermissions(): List<String> = REQUIRED_PERMISSIONS.filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
    }
}
