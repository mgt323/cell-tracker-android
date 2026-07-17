package com.ferhatozcelik.jetpackcomposetemplate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferhatozcelik.jetpackcomposetemplate.domain.exception.PermissionMissingException
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentConnectionViewModel @Inject constructor(
    private val telephonyRepository: TelephonyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CurrentConnectionUiState>(CurrentConnectionUiState.Loading)
    val uiState: StateFlow<CurrentConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                telephonyRepository.observeCurrentCell().collect { snapshot ->
                    _uiState.value = CurrentConnectionUiState.Content(snapshot)
                }
            } catch (e: PermissionMissingException) {
                _uiState.value = CurrentConnectionUiState.PermissionsRequiredState
            }
        }
    }
}
