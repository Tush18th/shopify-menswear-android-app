package com.menswear.app.ui.screens.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menswear.app.data.model.*
import com.menswear.app.data.repository.ProductRepository
import com.menswear.app.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val collection: Collection? = null,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val sortKey: SortKey = SortKey.BEST_SELLING,
    val filters: ProductFilters = ProductFilters(),
    val availableFilters: AvailableFilters = AvailableFilters(),
    val showFilterSheet: Boolean = false
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val handle: String = savedStateHandle["handle"] ?: ""

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        if (handle.isNotEmpty()) {
            loadCollection()
        }
    }

    fun loadCollection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, products = emptyList(), endCursor = null) }
            val state = _uiState.value
            when (val result = productRepository.getCollectionByHandle(
                handle = handle,
                first = PAGE_SIZE,
                sortKey = state.sortKey,
                filters = state.filters
            )) {
                is AppResult.Success -> {
                    val (collection, paginatedProducts, availableFilters) = result.data
                    _uiState.update {
                        it.copy(
                            collection = collection,
                            products = paginatedProducts.items,
                            hasNextPage = paginatedProducts.pageInfo.hasNextPage,
                            endCursor = paginatedProducts.pageInfo.endCursor,
                            availableFilters = availableFilters,
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
            when (val result = productRepository.getCollectionByHandle(
                handle = handle,
                first = PAGE_SIZE,
                after = state.endCursor,
                sortKey = state.sortKey,
                filters = state.filters
            )) {
                is AppResult.Success -> {
                    val (_, paginatedProducts, _) = result.data
                    _uiState.update {
                        it.copy(
                            products = it.products + paginatedProducts.items,
                            hasNextPage = paginatedProducts.pageInfo.hasNextPage,
                            endCursor = paginatedProducts.pageInfo.endCursor,
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

    fun setSortKey(sortKey: SortKey) {
        _uiState.update { it.copy(sortKey = sortKey) }
        loadCollection()
    }

    fun updateFilters(filters: ProductFilters) {
        _uiState.update { it.copy(filters = filters, showFilterSheet = false) }
        loadCollection()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = ProductFilters(), showFilterSheet = false) }
        loadCollection()
    }

    fun toggleFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = !it.showFilterSheet) }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
