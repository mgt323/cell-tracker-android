package com.ferhatozcelik.jetpackcomposetemplate.ui.home

import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot

/**
 * UI state for [CurrentConnectionScreen]/[CurrentConnectionViewModel].
 *
 * Modeled as an explicit sealed hierarchy so that "permissions missing" is
 * never collapsed into a generic error/null state, per
 * `.cursor/rules/permissions.mdc` — the UI must be able to render a
 * dedicated fallback for it instead of the dashboard.
 */
sealed interface CurrentConnectionUiState {

    /** Initial state, shown before the first [CellSnapshot] emission arrives. */
    data object Loading : CurrentConnectionUiState

    /** A [CellSnapshot] was successfully observed and can be rendered. */
    data class Content(val snapshot: CellSnapshot) : CurrentConnectionUiState

    /**
     * `ACCESS_FINE_LOCATION` and/or `READ_PHONE_STATE` are not granted, so
     * the current cell cannot be observed. The UI must render a dedicated
     * "Permissions Required" fallback instead of the dashboard while this
     * state is active.
     */
    data object PermissionsRequiredState : CurrentConnectionUiState
}
