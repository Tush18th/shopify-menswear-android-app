package com.menswear.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: String,
    val title: String,
    val handle: String,
    val description: String = "",
    val image: ProductImage? = null,
    val products: List<Product> = emptyList()
)

/** Predefined categories used in the app. */
enum class Category(
    val handle: String,
    val displayName: String,
    val iconRes: String
) {
    T_SHIRTS("t-shirts", "T-Shirts", "tshirt"),
    SHIRTS("shirts", "Shirts", "shirt"),
    DENIM("denim", "Denim", "denim"),
    TROUSERS("trousers", "Trousers", "trousers"),
    SUITS("suits", "Suits", "suit"),
    JACKETS("jackets", "Jackets", "jacket"),
    FOOTWEAR("footwear", "Footwear", "footwear"),
    ACCESSORIES("accessories", "Accessories", "accessories");

    companion object {
        fun fromHandle(handle: String): Category? =
            entries.find { it.handle == handle }
    }
}
