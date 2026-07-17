package com.ferhatozcelik.jetpackcomposetemplate.ui.activitys

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.ferhatozcelik.jetpackcomposetemplate.navigation.NavGraph
import com.ferhatozcelik.jetpackcomposetemplate.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Requests `ACCESS_FINE_LOCATION` and `READ_PHONE_STATE` (required by
 * [com.ferhatozcelik.jetpackcomposetemplate.data.telephony.TelephonyRepositoryImpl],
 * per `.cursor/rules/permissions.mdc` / `.cursor/rules/android-telephony.mdc`)
 * before composing [NavGraph]. This guarantees that by the time
 * `CurrentConnectionViewModel`'s Hilt-injected `TelephonyRepository` starts
 * collecting (its `init` block runs once, on first composition), the
 * permission check inside `TelephonyRepositoryImpl.observeCurrentCell()`
 * already sees the up-to-date grant state — so a fresh grant via this
 * runtime prompt is picked up immediately, with no separate "switch to the
 * real repository" step needed. If the user denies, composition proceeds
 * anyway; the screen's own `PermissionsRequired` fallback state (driven by
 * `MissingTelephonyPermissionsException`) takes over instead of crashing or
 * showing a blank screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var permissionsResolved by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionsResolved = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasRequiredPermissions()) {
            permissionsResolved = true
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE)
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
}