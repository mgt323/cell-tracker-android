package com.mgt323.celltracker.ui.activitys

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.mgt323.celltracker.navigation.NavGraph
import com.mgt323.celltracker.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Requests `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, and
 * `READ_PHONE_STATE` before composing [NavGraph]. Fine + phone state are
 * required by
 * [com.mgt323.celltracker.data.telephony.TelephonyRepositoryImpl]
 * (per `.cursor/rules/permissions.mdc` / `.cursor/rules/android-telephony.mdc`);
 * coarse is requested alongside fine as required on Android 12+.
 * This guarantees that by the time `CurrentConnectionViewModel`'s
 * Hilt-injected `TelephonyRepository` starts collecting (its `init` block
 * runs once, on first composition), the permission check inside
 * `TelephonyRepositoryImpl.observeCurrentCell()` already sees the up-to-date
 * grant state. If the user denies, composition proceeds anyway; the screen's
 * own `PermissionsRequired` fallback state (driven by
 * `MissingTelephonyPermissionsException`) takes over instead of crashing.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var permissionsResolved by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // TEMP: CellTrackerDebug — remove once permission flow is verified
        results.forEach { (permission, granted) ->
            Log.d(DEBUG_TAG, "permission result: $permission = $granted")
        }
        permissionsResolved = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasRequiredPermissions()) {
            permissionsResolved = true
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (permissionsResolved) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return hasFineLocation && hasPhoneState
    }

    companion object {
        // TEMP: CellTrackerDebug — remove once permission flow is verified
        private const val DEBUG_TAG = "CellTrackerDebug"
    }
}