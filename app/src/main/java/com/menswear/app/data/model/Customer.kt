package com.menswear.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String? = null,
    val defaultAddress: Address? = null,
    val addresses: List<Address> = emptyList(),
    val orders: List<Order> = emptyList()
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifEmpty { email }
}

@Serializable
data class Address(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val address1: String = "",
    val address2: String? = null,
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val zip: String = "",
    val phone: String? = null
) {
    val formatted: String
        get() = listOfNotNull(address1, address2, city, "$province $zip", country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

@Serializable
data class Order(
    val id: String,
    val orderNumber: Int,
    val name: String = "",
    val processedAt: String = "",
    val financialStatus: String = "",
    val fulfillmentStatus: String = "",
    val totalPrice: MoneyV2,
    val lineItems: List<OrderLineItem> = emptyList(),
    val statusUrl: String = ""
)

@Serializable
data class OrderLineItem(
    val title: String,
    val quantity: Int,
    val variant: OrderVariant? = null
)

@Serializable
data class OrderVariant(
    val title: String,
    val image: ProductImage? = null,
    val price: MoneyV2
)
