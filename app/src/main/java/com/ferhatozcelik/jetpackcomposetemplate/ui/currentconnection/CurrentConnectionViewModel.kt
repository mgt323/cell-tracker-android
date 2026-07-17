package com.ferhatozcelik.jetpackcomposetemplate.ui.currentconnection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.MissingTelephonyPermissionsException
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives [CurrentConnectionScreen] by collecting
 * [TelephonyRepository.observeCurrentCell] and mapping every emission (or
 * failure) to an explicit [CurrentConnectionUiState].
 *
 * Per `.cursor/rules/permissions.mdc`, a [MissingTelephonyPermissionsException]
 * emitted by the repository is caught specifically and mapped to
 * [CurrentConnectionUiState.PermissionsRequired] — it is never allowed to
 * crash this ViewModel or collapse into a silent/empty state. Any other
 * failure is rethrown rather than swallowed, since it isn't the agreed
 * permissions-denial signal.
 */
@HiltViewModel
class CurrentConnectionViewModel @Inject constructor(
    private val telephonyRepository: TelephonyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CurrentConnectionUiState>(CurrentConnectionUiState.Loading)
    val uiState: StateFlow<CurrentConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshots: Flow<CurrentConnectionUiState> =
                telephonyRepository.observeCurrentCell()
                    .map { snapshot -> CurrentConnectionUiState.Content(snapshot) }

            snapshots
                .catch { e ->
                    if (e is MissingTelephonyPermissionsException) {
                        emit(CurrentConnectionUiState.PermissionsRequired)
                    } else {
                        throw e
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }
}
