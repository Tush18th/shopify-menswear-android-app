package com.menswear.app.ui.navigation

/**
 * Defines all navigation routes in the app.
 */
object Routes {
    const val HOME = "home"
    const val COLLECTION = "collection/{handle}"
    const val PRODUCT = "product/{handle}"
    const val CART = "cart"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ACCOUNT = "account"
    const val SEARCH = "search/{query}"

    fun collection(handle: String) = "collection/$handle"
    fun product(handle: String) = "product/$handle"
    fun search(query: String) = "search/$query"
}
