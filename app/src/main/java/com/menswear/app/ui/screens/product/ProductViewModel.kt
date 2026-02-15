package com.menswear.app.ui.screens.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menswear.app.data.model.Product
import com.menswear.app.data.model.Variant
import com.menswear.app.data.repository.CartRepository
import com.menswear.app.data.repository.ProductRepository
import com.menswear.app.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val product: Product? = null,
    val selectedVariant: Variant? = null,
    val selectedSize: String? = null,
    val selectedColor: String? = null,
    val quantity: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null,
    val addToCartSuccess: Boolean = false,
    val addToCartError: String? = null,
    val isAddingToCart: Boolean = false,
    val currentImageIndex: Int = 0,
    val expandedSection: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val handle: String = savedStateHandle["handle"] ?: ""

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        if (handle.isNotEmpty()) {
            loadProduct()
        }
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = productRepository.getProductByHandle(handle)) {
                is AppResult.Success -> {
                    val product = result.data
                    val defaultVariant = product.variants.firstOrNull { it.availableForSale }
                        ?: product.variants.firstOrNull()
                    val defaultSize = defaultVariant?.selectedOptions
                        ?.find { it.name.equals("Size", ignoreCase = true) }?.value
                    val defaultColor = defaultVariant?.selectedOptions
                        ?.find { it.name.equals("Color", ignoreCase = true) }?.value

                    _uiState.update {
                        it.copy(
                            product = product,
                            selectedVariant = defaultVariant,
                            selectedSize = defaultSize,
                            selectedColor = defaultColor,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    fun selectSize(size: String) {
        _uiState.update { it.copy(selectedSize = size) }
        resolveVariant()
    }

    fun selectColor(color: String) {
        _uiState.update { it.copy(selectedColor = color) }
        resolveVariant()
    }

    private fun resolveVariant() {
        val state = _uiState.value
        val product = state.product ?: return
        val selectedSize = state.selectedSize
        val selectedColor = state.selectedColor

        val variant = product.variants.firstOrNull { v ->
            val sizeMatch = selectedSize == null ||
                    v.selectedOptions.any { it.name.equals("Size", ignoreCase = true) && it.value == selectedSize }
            val colorMatch = selectedColor == null ||
                    v.selectedOptions.any { it.name.equals("Color", ignoreCase = true) && it.value == selectedColor }
            sizeMatch && colorMatch && v.availableForSale
        } ?: product.variants.firstOrNull { v ->
            val sizeMatch = selectedSize == null ||
                    v.selectedOptions.any { it.name.equals("Size", ignoreCase = true) && it.value == selectedSize }
            val colorMatch = selectedColor == null ||
                    v.selectedOptions.any { it.name.equals("Color", ignoreCase = true) && it.value == selectedColor }
            sizeMatch && colorMatch
        }

        _uiState.update { it.copy(selectedVariant = variant) }
    }

    fun setQuantity(quantity: Int) {
        if (quantity in 1..99) {
            _uiState.update { it.copy(quantity = quantity) }
        }
    }

    fun setImageIndex(index: Int) {
        _uiState.update { it.copy(currentImageIndex = index) }
    }

    fun toggleSection(section: String) {
        _uiState.update {
            it.copy(expandedSection = if (it.expandedSection == section) null else section)
        }
    }

    fun addToCart() {
        val state = _uiState.value
        val variantId = state.selectedVariant?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToCart = true, addToCartSuccess = false, addToCartError = null) }
            when (val result = cartRepository.addToCart(variantId, state.quantity)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isAddingToCart = false, addToCartSuccess = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isAddingToCart = false, addToCartError = result.message) }
                }
            }
        }
    }

    fun dismissAddToCartMessage() {
        _uiState.update { it.copy(addToCartSuccess = false, addToCartError = null) }
    }
}
