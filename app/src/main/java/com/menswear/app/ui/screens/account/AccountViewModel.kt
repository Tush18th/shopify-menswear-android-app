package com.menswear.app.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menswear.app.data.model.Customer
import com.menswear.app.data.repository.CustomerRepository
import com.menswear.app.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val customer: Customer? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val logoutSuccess: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customerRepository.customer.collect { customer ->
                _uiState.update { it.copy(customer = customer, isLoading = false) }
            }
        }
    }

    fun refreshAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = customerRepository.refreshCustomer()) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (customerRepository.logout()) {
                is AppResult.Success -> _uiState.update { it.copy(logoutSuccess = true, isLoading = false) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = "Logout failed") }
            }
        }
    }
}
