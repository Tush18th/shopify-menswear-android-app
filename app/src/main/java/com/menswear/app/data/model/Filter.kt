package com.menswear.app.data.model

/** Represents the set of active product filters. */
data class ProductFilters(
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val vendors: List<String> = emptyList(),
    val productTypes: List<String> = emptyList(),
    val priceRange: PriceRangeFilter? = null
) {
    val isActive: Boolean
        get() = sizes.isNotEmpty() || colors.isNotEmpty() || vendors.isNotEmpty() ||
                productTypes.isNotEmpty() || priceRange != null

    val activeCount: Int
        get() = sizes.size + colors.size + vendors.size + productTypes.size +
                (if (priceRange != null) 1 else 0)

    /** Convert to Storefront API filter variables. */
    fun toGraphQLFilters(): List<Map<String, Any>> {
        val filters = mutableListOf<Map<String, Any>>()

        sizes.forEach { size ->
            filters.add(mapOf("variantOption" to mapOf("name" to "Size", "value" to size)))
        }

        colors.forEach { color ->
            filters.add(mapOf("variantOption" to mapOf("name" to "Color", "value" to color)))
        }

        vendors.forEach { vendor ->
            filters.add(mapOf("productVendor" to vendor))
        }

        productTypes.forEach { type ->
            filters.add(mapOf("productType" to type))
        }

        priceRange?.let { range ->
            val priceMap = mutableMapOf<String, Any>()
            range.min?.let { priceMap["min"] = it }
            range.max?.let { priceMap["max"] = it }
            if (priceMap.isNotEmpty()) {
                filters.add(mapOf("price" to priceMap))
            }
        }

        return filters
    }
}

data class PriceRangeFilter(
    val min: Double? = null,
    val max: Double? = null
)

enum class SortKey(val displayName: String, val storefrontValue: String, val reverse: Boolean) {
    BEST_SELLING("Best Selling", "BEST_SELLING", false),
    NEWEST("Newest", "CREATED", true),
    PRICE_LOW_HIGH("Price: Low to High", "PRICE", false),
    PRICE_HIGH_LOW("Price: High to Low", "PRICE", true);
}

/** Available filter options extracted from collection products. */
data class AvailableFilters(
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val vendors: List<String> = emptyList(),
    val productTypes: List<String> = emptyList(),
    val minPrice: Double = 0.0,
    val maxPrice: Double = 1000.0
)
