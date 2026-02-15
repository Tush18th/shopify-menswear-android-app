package com.menswear.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menswear.app.data.repository.CustomerRepository
import com.menswear.app.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val registerSuccess: Boolean = false,
    val recoverSuccess: Boolean = false,
    // Login fields
    val loginEmail: String = "",
    val loginPassword: String = "",
    // Register fields
    val registerFirstName: String = "",
    val registerLastName: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    // Recover
    val recoverEmail: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Login field updates
    fun updateLoginEmail(value: String) = _uiState.update { it.copy(loginEmail = value, error = null) }
    fun updateLoginPassword(value: String) = _uiState.update { it.copy(loginPassword = value, error = null) }

    // Register field updates
    fun updateRegisterFirstName(value: String) = _uiState.update { it.copy(registerFirstName = value, error = null) }
    fun updateRegisterLastName(value: String) = _uiState.update { it.copy(registerLastName = value, error = null) }
    fun updateRegisterEmail(value: String) = _uiState.update { it.copy(registerEmail = value, error = null) }
    fun updateRegisterPassword(value: String) = _uiState.update { it.copy(registerPassword = value, error = null) }
    fun updateRegisterConfirmPassword(value: String) = _uiState.update { it.copy(registerConfirmPassword = value, error = null) }

    // Recover field updates
    fun updateRecoverEmail(value: String) = _uiState.update { it.copy(recoverEmail = value, error = null) }

    fun login() {
        val state = _uiState.value
        if (state.loginEmail.isBlank() || state.loginPassword.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = customerRepository.login(state.loginEmail, state.loginPassword)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.registerFirstName.isBlank() || state.registerEmail.isBlank() || state.registerPassword.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }
        if (state.registerPassword != state.registerConfirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (state.registerPassword.length < 5) {
            _uiState.update { it.copy(error = "Password must be at least 5 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = customerRepository.register(
                firstName = state.registerFirstName,
                lastName = state.registerLastName,
                email = state.registerEmail,
                password = state.registerPassword
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun recoverPassword() {
        val state = _uiState.value
        if (state.recoverEmail.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = customerRepository.recoverPassword(state.recoverEmail)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, recoverSuccess = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
