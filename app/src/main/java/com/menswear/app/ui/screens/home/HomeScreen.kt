package com.menswear.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.menswear.app.data.model.Category
import com.menswear.app.data.model.Product
import com.menswear.app.ui.components.ErrorState
import com.menswear.app.ui.components.LoadingIndicator
import com.menswear.app.ui.components.ProductCard
import com.menswear.app.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCollection: (String) -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    cartCount: Int,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MENSWEAR",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(Icons.Default.Person, contentDescription = "Account")
                    }
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge { Text(cartCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = "Cart")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.collections.isEmpty() -> LoadingIndicator()
            uiState.error != null && uiState.collections.isEmpty() -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadHomeData() }
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Search bar
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearch = { onNavigateToSearch(it) }
                    )

                    // Hero banner
                    HeroBanner(onClick = { onNavigateToCollection("new-arrivals") })

                    Spacer(modifier = Modifier.height(24.dp))

                    // Category grid
                    SectionHeader(title = "Shop by Category")
                    CategoryGrid(onCategoryClick = { onNavigateToCollection(it.handle) })

                    Spacer(modifier = Modifier.height(24.dp))

                    // New Arrivals rail
                    if (uiState.newArrivals.isNotEmpty()) {
                        SectionHeader(
                            title = "New Arrivals",
                            onSeeAll = { onNavigateToCollection("new-arrivals") }
                        )
                        ProductRail(
                            products = uiState.newArrivals,
                            onProductClick = { onNavigateToProduct(it.handle) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Best Sellers rail
                    if (uiState.bestSellers.isNotEmpty()) {
                        SectionHeader(
                            title = "Best Sellers",
                            onSeeAll = { onNavigateToCollection("best-sellers") }
                        )
                        ProductRail(
                            products = uiState.bestSellers,
                            onProductClick = { onNavigateToProduct(it.handle) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search products...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = {
                if (query.isNotBlank()) onSearch(query)
            }
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        )
    )
}

@Composable
private fun HeroBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF333333)
                    )
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NEW SEASON",
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Explore the\nLatest Collection",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Shop Now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See All")
            }
        }
    }
}

@Composable
private fun CategoryGrid(onCategoryClick: (Category) -> Unit) {
    val categories = Category.entries
    val icons = mapOf(
        Category.T_SHIRTS to Icons.Default.Checkroom,
        Category.SHIRTS to Icons.Default.Checkroom,
        Category.DENIM to Icons.Default.Checkroom,
        Category.TROUSERS to Icons.Default.Checkroom,
        Category.SUITS to Icons.Default.Checkroom,
        Category.JACKETS to Icons.Default.Checkroom,
        Category.FOOTWEAR to Icons.Default.Checkroom,
        Category.ACCESSORIES to Icons.Default.Watch
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp),
        userScrollEnabled = false
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                icon = icons[category] ?: Icons.Default.Checkroom,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.displayName,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ProductRail(
    products: List<Product>,
    onProductClick: (Product) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                modifier = Modifier.width(160.dp)
            )
        }
    }
}
