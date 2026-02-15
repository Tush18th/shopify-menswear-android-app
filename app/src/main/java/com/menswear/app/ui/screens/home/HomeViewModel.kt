package com.menswear.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menswear.app.data.model.Collection
import com.menswear.app.data.model.Product
import com.menswear.app.data.repository.ProductRepository
import com.menswear.app.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val collections: List<Collection> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val bestSellers: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load collections
            launch {
                when (val result = productRepository.getCollections()) {
                    is AppResult.Success -> _uiState.update { it.copy(collections = result.data) }
                    is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                }
            }

            // Load new arrivals from a "new-arrivals" collection or use first collection with sorting
            launch {
                val result = productRepository.getCollectionByHandle(
                    handle = "new-arrivals",
                    first = 10,
                    sortKey = com.menswear.app.data.model.SortKey.NEWEST
                )
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(newArrivals = result.data.second.items) }
                    is AppResult.Error -> {
                        // Fallback: try t-shirts collection sorted by newest
                        val fallback = productRepository.getCollectionByHandle(
                            handle = "t-shirts",
                            first = 10,
                            sortKey = com.menswear.app.data.model.SortKey.NEWEST
                        )
                        if (fallback is AppResult.Success) {
                            _uiState.update { it.copy(newArrivals = fallback.data.second.items) }
                        }
                    }
                }
            }

            // Load best sellers
            launch {
                val result = productRepository.getCollectionByHandle(
                    handle = "best-sellers",
                    first = 10,
                    sortKey = com.menswear.app.data.model.SortKey.BEST_SELLING
                )
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(bestSellers = result.data.second.items) }
                    is AppResult.Error -> {
                        val fallback = productRepository.getCollectionByHandle(
                            handle = "shirts",
                            first = 10,
                            sortKey = com.menswear.app.data.model.SortKey.BEST_SELLING
                        )
                        if (fallback is AppResult.Success) {
                            _uiState.update { it.copy(bestSellers = fallback.data.second.items) }
                        }
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
