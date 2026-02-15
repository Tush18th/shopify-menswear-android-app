package com.menswear.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Cart(
    val id: String,
    val checkoutUrl: String = "",
    val lines: List<CartLine> = emptyList(),
    val cost: CartCost? = null,
    val totalQuantity: Int = 0,
    val discountCodes: List<CartDiscountCode> = emptyList()
) {
    val subtotal: MoneyV2?
        get() = cost?.subtotalAmount

    val total: MoneyV2?
        get() = cost?.totalAmount
}

@Serializable
data class CartLine(
    val id: String,
    val quantity: Int,
    val merchandise: CartMerchandise,
    val cost: CartLineCost? = null
)

@Serializable
data class CartMerchandise(
    val id: String,
    val title: String,
    val product: CartProduct,
    val selectedOptions: List<SelectedOption> = emptyList(),
    val image: ProductImage? = null,
    val price: MoneyV2
)

@Serializable
data class CartProduct(
    val id: String,
    val title: String,
    val handle: String
)

@Serializable
data class CartLineCost(
    val totalAmount: MoneyV2,
    val amountPerQuantity: MoneyV2
)

@Serializable
data class CartCost(
    val subtotalAmount: MoneyV2,
    val totalAmount: MoneyV2,
    val totalTaxAmount: MoneyV2? = null
)

@Serializable
data class CartDiscountCode(
    val code: String,
    val applicable: Boolean
)
