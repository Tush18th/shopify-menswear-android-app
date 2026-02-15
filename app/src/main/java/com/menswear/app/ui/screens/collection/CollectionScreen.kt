package com.menswear.app.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menswear.app.data.model.*
import com.menswear.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    cartCount: Int,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // Infinite scroll trigger
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6 && uiState.hasNextPage && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            MenswearTopBar(
                title = uiState.collection?.title ?: "Collection",
                onBackClick = onNavigateBack,
                onCartClick = onNavigateToCart,
                cartCount = cartCount
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sort & filter bar
            SortFilterBar(
                sortKey = uiState.sortKey,
                filterCount = uiState.filters.activeCount,
                onSortChange = { viewModel.setSortKey(it) },
                onFilterClick = { viewModel.toggleFilterSheet() }
            )

            // Active filter chips
            if (uiState.filters.isActive) {
                ActiveFilterChips(
                    filters = uiState.filters,
                    onClearAll = { viewModel.clearFilters() },
                    onRemoveSize = { size ->
                        viewModel.updateFilters(uiState.filters.copy(sizes = uiState.filters.sizes - size))
                    },
                    onRemoveColor = { color ->
                        viewModel.updateFilters(uiState.filters.copy(colors = uiState.filters.colors - color))
                    },
                    onRemoveVendor = { vendor ->
                        viewModel.updateFilters(uiState.filters.copy(vendors = uiState.filters.vendors - vendor))
                    },
                    onRemoveType = { type ->
                        viewModel.updateFilters(uiState.filters.copy(productTypes = uiState.filters.productTypes - type))
                    },
                    onRemovePrice = {
                        viewModel.updateFilters(uiState.filters.copy(priceRange = null))
                    }
                )
            }

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadCollection() }
                )
                uiState.products.isEmpty() -> EmptyCollection()
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onNavigateToProduct(product.handle) }
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            availableFilters = uiState.availableFilters,
            currentFilters = uiState.filters,
            onApply = { viewModel.updateFilters(it) },
            onDismiss = { viewModel.toggleFilterSheet() }
        )
    }
}

@Composable
private fun SortFilterBar(
    sortKey: SortKey,
    filterCount: Int,
    onSortChange: (SortKey) -> Unit,
    onFilterClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort dropdown
        Box {
            OutlinedButton(onClick = { showSortMenu = true }) {
                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(sortKey.displayName, style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                SortKey.entries.forEach { key ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                key.displayName,
                                fontWeight = if (key == sortKey) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSortChange(key)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Filter button
        OutlinedButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Filter", style = MaterialTheme.typography.labelLarge)
            if (filterCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Badge { Text(filterCount.toString()) }
            }
        }
    }
}

@Composable
private fun ActiveFilterChips(
    filters: ProductFilters,
    onClearAll: () -> Unit,
    onRemoveSize: (String) -> Unit,
    onRemoveColor: (String) -> Unit,
    onRemoveVendor: (String) -> Unit,
    onRemoveType: (String) -> Unit,
    onRemovePrice: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AssistChip(
                onClick = onClearAll,
                label = { Text("Clear all") },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        items(filters.sizes) { size ->
            InputChip(
                selected = true,
                onClick = { onRemoveSize(size) },
                label = { Text("Size: $size") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        items(filters.colors) { color ->
            InputChip(
                selected = true,
                onClick = { onRemoveColor(color) },
                label = { Text("Color: $color") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        items(filters.vendors) { vendor ->
            InputChip(
                selected = true,
                onClick = { onRemoveVendor(vendor) },
                label = { Text(vendor) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        items(filters.productTypes) { type ->
            InputChip(
                selected = true,
                onClick = { onRemoveType(type) },
                label = { Text(type) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        if (filters.priceRange != null) {
            item {
                InputChip(
                    selected = true,
                    onClick = onRemovePrice,
                    label = {
                        val min = filters.priceRange.min?.let { "${"%.0f".format(it)}" } ?: "0"
                        val max = filters.priceRange.max?.let { "${"%.0f".format(it)}" } ?: "+"
                        Text("$$min - $$max")
                    },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    availableFilters: AvailableFilters,
    currentFilters: ProductFilters,
    onApply: (ProductFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSizes by remember { mutableStateOf(currentFilters.sizes.toSet()) }
    var selectedColors by remember { mutableStateOf(currentFilters.colors.toSet()) }
    var selectedVendors by remember { mutableStateOf(currentFilters.vendors.toSet()) }
    var selectedTypes by remember { mutableStateOf(currentFilters.productTypes.toSet()) }
    var priceMin by remember { mutableStateOf(currentFilters.priceRange?.min?.toString() ?: "") }
    var priceMax by remember { mutableStateOf(currentFilters.priceRange?.max?.toString() ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Filters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Sizes
            if (availableFilters.sizes.isNotEmpty()) {
                Text("Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableFilters.sizes.forEach { size ->
                        FilterChip(
                            selected = size in selectedSizes,
                            onClick = {
                                selectedSizes = if (size in selectedSizes) selectedSizes - size else selectedSizes + size
                            },
                            label = { Text(size) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Colors
            if (availableFilters.colors.isNotEmpty()) {
                Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableFilters.colors.forEach { color ->
                        FilterChip(
                            selected = color in selectedColors,
                            onClick = {
                                selectedColors = if (color in selectedColors) selectedColors - color else selectedColors + color
                            },
                            label = { Text(color) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Vendors
            if (availableFilters.vendors.isNotEmpty()) {
                Text("Brand", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableFilters.vendors.forEach { vendor ->
                        FilterChip(
                            selected = vendor in selectedVendors,
                            onClick = {
                                selectedVendors = if (vendor in selectedVendors) selectedVendors - vendor else selectedVendors + vendor
                            },
                            label = { Text(vendor) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Product Types
            if (availableFilters.productTypes.isNotEmpty()) {
                Text("Product Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableFilters.productTypes.forEach { type ->
                        FilterChip(
                            selected = type in selectedTypes,
                            onClick = {
                                selectedTypes = if (type in selectedTypes) selectedTypes - type else selectedTypes + type
                            },
                            label = { Text(type) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Price range
            Text("Price Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = priceMin,
                    onValueChange = { priceMin = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Min") },
                    singleLine = true,
                    prefix = { Text("$") }
                )
                OutlinedTextField(
                    value = priceMax,
                    onValueChange = { priceMax = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Max") },
                    singleLine = true,
                    prefix = { Text("$") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply button
            Button(
                onClick = {
                    val priceRange = if (priceMin.isNotBlank() || priceMax.isNotBlank()) {
                        PriceRangeFilter(
                            min = priceMin.toDoubleOrNull(),
                            max = priceMax.toDoubleOrNull()
                        )
                    } else null

                    onApply(
                        ProductFilters(
                            sizes = selectedSizes.toList(),
                            colors = selectedColors.toList(),
                            vendors = selectedVendors.toList(),
                            productTypes = selectedTypes.toList(),
                            priceRange = priceRange
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Filters", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
private fun EmptyCollection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No products found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Try adjusting your filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
