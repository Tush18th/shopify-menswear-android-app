package com.menswear.app.data.repository

import com.menswear.app.data.api.ResponseParser
import com.menswear.app.data.api.ShopifyQueries
import com.menswear.app.data.api.StorefrontClient
import com.menswear.app.data.datastore.AppDataStore
import com.menswear.app.data.model.Cart
import com.menswear.app.data.model.GraphQLResponse
import com.menswear.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val client: StorefrontClient,
    private val dataStore: AppDataStore
) {
    private val _cart = MutableStateFlow<Cart?>(null)
    val cart: StateFlow<Cart?> = _cart.asStateFlow()

    val cartCount: Int get() = _cart.value?.totalQuantity ?: 0

    suspend fun initialize() {
        val cartId = dataStore.getCartId() ?: return
        when (val result = fetchCart(cartId)) {
            is AppResult.Success -> _cart.value = result.data
            is AppResult.Error -> {
                // Cart may have expired, clear it
                dataStore.clearCartId()
                _cart.value = null
            }
        }
    }

    suspend fun addToCart(variantId: String, quantity: Int = 1): AppResult<Cart> {
        val cartId = dataStore.getCartId()
        return if (cartId == null) {
            createCart(variantId, quantity)
        } else {
            addLineToCart(cartId, variantId, quantity)
        }
    }

    private suspend fun createCart(variantId: String, quantity: Int): AppResult<Cart> =
        AppResult.runCatching {
            val variables = buildJsonObject {
                putJsonArray("lines") {
                    addJsonObject {
                        put("merchandiseId", variantId)
                        put("quantity", quantity)
                    }
                }
            }
            val response = client.execute(ShopifyQueries.CREATE_CART, variables)
            checkErrors(response)
            val cartData = response.data!!.jsonObject["cartCreate"]!!.jsonObject
            checkUserErrors(cartData)
            val cart = ResponseParser.parseCart(cartData["cart"]!!)
            dataStore.saveCartId(cart.id)
            _cart.value = cart
            cart
        }

    private suspend fun addLineToCart(cartId: String, variantId: String, quantity: Int): AppResult<Cart> =
        AppResult.runCatching {
            val variables = buildJsonObject {
                put("cartId", cartId)
                putJsonArray("lines") {
                    addJsonObject {
                        put("merchandiseId", variantId)
                        put("quantity", quantity)
                    }
                }
            }
            val response = client.execute(ShopifyQueries.ADD_TO_CART, variables)
            checkErrors(response)
            val cartData = response.data!!.jsonObject["cartLinesAdd"]!!.jsonObject
            checkUserErrors(cartData)
            val cart = ResponseParser.parseCart(cartData["cart"]!!)
            _cart.value = cart
            cart
        }

    suspend fun updateLineQuantity(lineId: String, quantity: Int): AppResult<Cart> =
        AppResult.runCatching {
            val cartId = dataStore.getCartId() ?: throw Exception("No active cart")
            val variables = buildJsonObject {
                put("cartId", cartId)
                putJsonArray("lines") {
                    addJsonObject {
                        put("id", lineId)
                        put("quantity", quantity)
                    }
                }
            }
            val response = client.execute(ShopifyQueries.UPDATE_CART_LINE, variables)
            checkErrors(response)
            val cartData = response.data!!.jsonObject["cartLinesUpdate"]!!.jsonObject
            checkUserErrors(cartData)
            val cart = ResponseParser.parseCart(cartData["cart"]!!)
            _cart.value = cart
            cart
        }

    suspend fun removeLine(lineId: String): AppResult<Cart> =
        AppResult.runCatching {
            val cartId = dataStore.getCartId() ?: throw Exception("No active cart")
            val variables = buildJsonObject {
                put("cartId", cartId)
                putJsonArray("lineIds") { add(lineId) }
            }
            val response = client.execute(ShopifyQueries.REMOVE_CART_LINE, variables)
            checkErrors(response)
            val cartData = response.data!!.jsonObject["cartLinesRemove"]!!.jsonObject
            checkUserErrors(cartData)
            val cart = ResponseParser.parseCart(cartData["cart"]!!)
            _cart.value = cart
            cart
        }

    suspend fun applyDiscountCode(code: String): AppResult<Cart> =
        AppResult.runCatching {
            val cartId = dataStore.getCartId() ?: throw Exception("No active cart")
            val variables = buildJsonObject {
                put("cartId", cartId)
                putJsonArray("discountCodes") { add(code) }
            }
            val response = client.execute(ShopifyQueries.APPLY_DISCOUNT, variables)
            checkErrors(response)
            val cartData = response.data!!.jsonObject["cartDiscountCodesUpdate"]!!.jsonObject
            checkUserErrors(cartData)
            val cart = ResponseParser.parseCart(cartData["cart"]!!)
            _cart.value = cart
            cart
        }

    private suspend fun fetchCart(cartId: String): AppResult<Cart> =
        AppResult.runCatching {
            val variables = buildJsonObject { put("cartId", cartId) }
            val response = client.execute(ShopifyQueries.GET_CART, variables)
            checkErrors(response)
            val cartData = response.data!!.jsonObject["cart"]
                ?: throw Exception("Cart not found")
            ResponseParser.parseCart(cartData)
        }

    fun getCheckoutUrl(): String? = _cart.value?.checkoutUrl

    suspend fun clearCart() {
        dataStore.clearCartId()
        _cart.value = null
    }

    private fun checkErrors(response: GraphQLResponse) {
        response.errors?.let { errors ->
            if (errors.isNotEmpty()) {
                throw Exception(errors.joinToString("; ") { it.message })
            }
        }
    }

    private fun checkUserErrors(data: JsonObject) {
        val userErrors = data["userErrors"]?.jsonArray
        if (userErrors != null && userErrors.isNotEmpty()) {
            val messages = userErrors.map { it.jsonObject["message"]?.jsonPrimitive?.content ?: "" }
            throw Exception(messages.joinToString("; "))
        }
    }
}
