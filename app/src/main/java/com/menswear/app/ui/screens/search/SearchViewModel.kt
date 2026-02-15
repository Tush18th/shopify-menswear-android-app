package com.menswear.app.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val initialQuery: String = savedStateHandle["query"] ?: ""

    private val _uiState = MutableStateFlow(SearchUiState(query = initialQuery))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        if (initialQuery.isNotBlank()) {
            search(initialQuery)
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search(query: String = _uiState.value.query) {
        if (query.isBlank()) return
        _uiState.update { it.copy(query = query, isLoading = true, error = null, products = emptyList(), endCursor = null) }

        viewModelScope.launch {
            when (val result = productRepository.searchProducts(query, first = 20)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            products = result.data.items,
                            hasNextPage = result.data.pageInfo.hasNextPage,
                            endCursor = result.data.pageInfo.endCursor,
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

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasNextPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = productRepository.searchProducts(state.query, first = 20, after = state.endCursor)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            products = it.products + result.data.items,
                            hasNextPage = result.data.pageInfo.hasNextPage,
                            endCursor = result.data.pageInfo.endCursor,
                            isLoadingMore = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }
}
