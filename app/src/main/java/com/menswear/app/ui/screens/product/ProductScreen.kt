package com.menswear.app.ui.screens.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.menswear.app.data.model.ColorOption
import com.menswear.app.ui.components.*
import com.menswear.app.ui.theme.OnSale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    cartCount: Int,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar for add-to-cart feedback
    LaunchedEffect(uiState.addToCartSuccess) {
        if (uiState.addToCartSuccess) {
            snackbarHostState.showSnackbar("Added to cart")
            viewModel.dismissAddToCartMessage()
        }
    }
    LaunchedEffect(uiState.addToCartError) {
        uiState.addToCartError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissAddToCartMessage()
        }
    }

    Scaffold(
        topBar = {
            MenswearTopBar(
                title = "",
                onBackClick = onNavigateBack,
                onCartClick = onNavigateToCart,
                cartCount = cartCount
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            uiState.product?.let {
                AddToCartBottomBar(
                    price = uiState.selectedVariant?.price?.formatted ?: "",
                    isAvailable = uiState.selectedVariant?.availableForSale ?: false,
                    isLoading = uiState.isAddingToCart,
                    onAddToCart = { viewModel.addToCart() }
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadProduct() }
            )
            uiState.product != null -> {
                val product = uiState.product!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Image Gallery
                    ImageGallery(
                        images = product.images.map { it.url },
                        currentIndex = uiState.currentImageIndex,
                        onPageChange = { viewModel.setImageIndex(it) }
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Vendor
                        if (product.vendor.isNotBlank()) {
                            Text(
                                text = product.vendor.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Title
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Price
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val variant = uiState.selectedVariant
                            Text(
                                text = variant?.price?.formatted ?: product.minPrice?.formatted ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (variant?.isOnSale == true) OnSale else MaterialTheme.colorScheme.onSurface
                            )
                            if (variant?.isOnSale == true && variant.compareAtPrice != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = variant.compareAtPrice.formatted,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Color selection
                        val colors = product.availableColors
                        if (colors.isNotEmpty()) {
                            Text(
                                text = "Color: ${uiState.selectedColor ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                colors.forEach { colorOpt ->
                                    ColorSwatchItem(
                                        colorOption = colorOpt,
                                        isSelected = colorOpt.name == uiState.selectedColor,
                                        onClick = { viewModel.selectColor(colorOpt.name) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Size selection
                        val sizes = product.availableSizes
                        if (sizes.isNotEmpty()) {
                            Text(
                                text = "Size: ${uiState.selectedSize ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                sizes.forEach { size ->
                                    val isSelected = size == uiState.selectedSize
                                    Surface(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .defaultMinSize(minWidth = 44.dp)
                                            .clickable { viewModel.selectSize(size) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = size,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Quantity
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        QuantitySelector(
                            quantity = uiState.quantity,
                            onQuantityChange = { viewModel.setQuantity(it) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()

                        // Collapsible sections
                        CollapsibleSection(
                            title = "Description",
                            isExpanded = uiState.expandedSection == "description",
                            onToggle = { viewModel.toggleSection("description") }
                        ) {
                            Text(
                                text = product.description.ifBlank { "No description available." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        CollapsibleSection(
                            title = "Shipping & Returns",
                            isExpanded = uiState.expandedSection == "shipping",
                            onToggle = { viewModel.toggleSection("shipping") }
                        ) {
                            Text(
                                text = "Free standard shipping on orders over \$100. " +
                                        "Returns accepted within 30 days of delivery. " +
                                        "Items must be unworn with tags attached.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        CollapsibleSection(
                            title = "Size Guide",
                            isExpanded = uiState.expandedSection == "sizeguide",
                            onToggle = { viewModel.toggleSection("sizeguide") }
                        ) {
                            Text(
                                text = "Please refer to our size chart to find your perfect fit. " +
                                        "If you're between sizes, we recommend sizing up for a relaxed fit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Bottom bar space
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageGallery(
    images: List<String>,
    currentIndex: Int,
    onPageChange: (Int) -> Unit
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { images.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = "Product image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Page indicator dots
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }

        // Image counter
        if (images.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchItem(
    colorOption: ColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = colorOption.hexColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { Color.Gray }
    } ?: Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = colorOption.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                content()
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun AddToCartBottomBar(
    price: String,
    isAvailable: Boolean,
    isLoading: Boolean,
    onAddToCart: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onAddToCart,
                enabled = isAvailable && !isLoading,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAvailable) "Add to Cart" else "Sold Out",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
