package com.menswear.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val title: String,
    val handle: String,
    val description: String = "",
    val descriptionHtml: String = "",
    val vendor: String = "",
    val productType: String = "",
    val tags: List<String> = emptyList(),
    val images: List<ProductImage> = emptyList(),
    val variants: List<Variant> = emptyList(),
    val priceRange: PriceRange? = null,
    val compareAtPriceRange: CompareAtPriceRange? = null,
    val availableForSale: Boolean = true
) {
    val primaryImage: ProductImage?
        get() = images.firstOrNull()

    val minPrice: MoneyV2?
        get() = priceRange?.minVariantPrice

    val maxPrice: MoneyV2?
        get() = priceRange?.maxVariantPrice

    val compareAtPrice: MoneyV2?
        get() = compareAtPriceRange?.minVariantPrice

    val isOnSale: Boolean
        get() {
            val price = minPrice?.amountAsDouble ?: return false
            val compareAt = compareAtPrice?.amountAsDouble ?: return false
            return compareAt > price
        }

    val availableSizes: List<String>
        get() = variants
            .filter { it.availableForSale }
            .mapNotNull { v -> v.selectedOptions.find { it.name.equals("Size", ignoreCase = true) }?.value }
            .distinct()

    val availableColors: List<ColorOption>
        get() = variants
            .filter { it.availableForSale }
            .mapNotNull { v ->
                val colorName = v.selectedOptions.find { it.name.equals("Color", ignoreCase = true) }?.value
                    ?: return@mapNotNull null
                ColorOption(
                    name = colorName,
                    hexColor = v.swatchHex ?: ColorOption.fallbackHex(colorName)
                )
            }
            .distinctBy { it.name }
}

@Serializable
data class PriceRange(
    val minVariantPrice: MoneyV2,
    val maxVariantPrice: MoneyV2
)

@Serializable
data class CompareAtPriceRange(
    val minVariantPrice: MoneyV2,
    val maxVariantPrice: MoneyV2
)

@Serializable
data class ProductImage(
    val id: String? = null,
    val url: String,
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class Variant(
    val id: String,
    val title: String,
    val availableForSale: Boolean = true,
    val price: MoneyV2,
    val compareAtPrice: MoneyV2? = null,
    val selectedOptions: List<SelectedOption> = emptyList(),
    val image: ProductImage? = null,
    val swatchHex: String? = null
) {
    val isOnSale: Boolean
        get() {
            val compareAt = compareAtPrice?.amountAsDouble ?: return false
            return compareAt > price.amountAsDouble
        }
}

@Serializable
data class SelectedOption(
    val name: String,
    val value: String
)

data class ColorOption(
    val name: String,
    val hexColor: String?
) {
    companion object {
        private val colorMap = mapOf(
            "black" to "#000000",
            "white" to "#FFFFFF",
            "navy" to "#000080",
            "blue" to "#0000FF",
            "red" to "#FF0000",
            "green" to "#008000",
            "grey" to "#808080",
            "gray" to "#808080",
            "beige" to "#F5F5DC",
            "brown" to "#8B4513",
            "khaki" to "#C3B091",
            "olive" to "#808000",
            "tan" to "#D2B48C",
            "charcoal" to "#36454F",
            "burgundy" to "#800020",
            "cream" to "#FFFDD0",
            "pink" to "#FFC0CB",
            "orange" to "#FFA500",
            "yellow" to "#FFFF00",
            "purple" to "#800080",
            "maroon" to "#800000",
            "teal" to "#008080",
            "coral" to "#FF7F50",
            "ivory" to "#FFFFF0",
            "lavender" to "#E6E6FA",
            "indigo" to "#4B0082",
            "silver" to "#C0C0C0",
            "gold" to "#FFD700"
        )

        fun fallbackHex(colorName: String): String? {
            return colorMap[colorName.lowercase().trim()]
        }
    }
}
