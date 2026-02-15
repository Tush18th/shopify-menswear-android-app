package com.menswear.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MoneyV2(
    val amount: String,
    val currencyCode: String
) {
    val amountAsDouble: Double
        get() = amount.toDoubleOrNull() ?: 0.0

    val formatted: String
        get() {
            val symbol = currencySymbols[currencyCode] ?: currencyCode
            return "$symbol${"%.2f".format(amountAsDouble)}"
        }

    companion object {
        private val currencySymbols = mapOf(
            "USD" to "$",
            "EUR" to "\u20AC",
            "GBP" to "\u00A3",
            "CAD" to "CA$",
            "AUD" to "A$",
            "INR" to "\u20B9",
            "JPY" to "\u00A5"
        )
    }
}
