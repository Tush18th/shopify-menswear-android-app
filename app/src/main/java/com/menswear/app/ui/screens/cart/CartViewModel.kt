package com.menswear.app.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menswear.app.data.model.Cart
import com.menswear.app.data.repository.CartRepository
import com.menswear.app.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val cart: Cart? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val discountCode: String = "",
    val discountError: String? = null,
    val isApplyingDiscount: Boolean = false
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.cart.collect { cart ->
                _uiState.update { it.copy(cart = cart, isLoading = false) }
            }
        }
    }

    fun updateQuantity(lineId: String, quantity: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = cartRepository.updateLineQuantity(lineId, quantity)) {
                is AppResult.Success -> {} // Cart flow will update
                is AppResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            }
        }
    }

    fun removeLine(lineId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = cartRepository.removeLine(lineId)) {
                is AppResult.Success -> {}
                is AppResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            }
        }
    }

    fun updateDiscountCode(code: String) {
        _uiState.update { it.copy(discountCode = code, discountError = null) }
    }

    fun applyDiscount() {
        val code = _uiState.value.discountCode.trim()
        if (code.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingDiscount = true, discountError = null) }
            when (val result = cartRepository.applyDiscountCode(code)) {
                is AppResult.Success -> {
                    val cart = result.data
                    val applied = cart.discountCodes.any { it.code == code && it.applicable }
                    _uiState.update {
                        it.copy(
                            isApplyingDiscount = false,
                            discountCode = if (applied) "" else code,
                            discountError = if (!applied) "Discount code is not applicable" else null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isApplyingDiscount = false, discountError = result.message) }
                }
            }
        }
    }

    fun getCheckoutUrl(): String? = cartRepository.getCheckoutUrl()

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
