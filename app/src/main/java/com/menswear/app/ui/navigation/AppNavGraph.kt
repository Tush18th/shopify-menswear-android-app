package com.menswear.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.menswear.app.data.repository.CartRepository
import com.menswear.app.data.repository.CustomerRepository
import com.menswear.app.ui.screens.account.AccountScreen
import com.menswear.app.ui.screens.auth.ForgotPasswordScreen
import com.menswear.app.ui.screens.auth.LoginScreen
import com.menswear.app.ui.screens.auth.RegisterScreen
import com.menswear.app.ui.screens.cart.CartScreen
import com.menswear.app.ui.screens.collection.CollectionScreen
import com.menswear.app.ui.screens.home.HomeScreen
import com.menswear.app.ui.screens.product.ProductScreen
import com.menswear.app.ui.screens.search.SearchScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    cartRepository: CartRepository,
    customerRepository: CustomerRepository
) {
    val cart by cartRepository.cart.collectAsState()
    val isLoggedIn by customerRepository.isLoggedIn.collectAsState()
    val cartCount = cart?.totalQuantity ?: 0

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // Home
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToCollection = { handle ->
                    navController.navigate(Routes.collection(handle))
                },
                onNavigateToProduct = { handle ->
                    navController.navigate(Routes.product(handle))
                },
                onNavigateToCart = { navController.navigate(Routes.CART) },
                onNavigateToAccount = {
                    if (isLoggedIn) navController.navigate(Routes.ACCOUNT)
                    else navController.navigate(Routes.LOGIN)
                },
                onNavigateToSearch = { query ->
                    navController.navigate(Routes.search(query))
                },
                cartCount = cartCount
            )
        }

        // Collection
        composable(
            route = Routes.COLLECTION,
            arguments = listOf(navArgument("handle") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://{domain}/collections/{handle}" },
                navDeepLink { uriPattern = "menswear://collection/{handle}" }
            )
        ) {
            CollectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProduct = { handle ->
                    navController.navigate(Routes.product(handle))
                },
                onNavigateToCart = { navController.navigate(Routes.CART) },
                cartCount = cartCount
            )
        }

        // Product
        composable(
            route = Routes.PRODUCT,
            arguments = listOf(navArgument("handle") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://{domain}/products/{handle}" },
                navDeepLink { uriPattern = "menswear://product/{handle}" }
            )
        ) {
            ProductScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(Routes.CART) },
                cartCount = cartCount
            )
        }

        // Cart
        composable(Routes.CART) {
            CartScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProduct = { handle ->
                    navController.navigate(Routes.product(handle))
                }
            )
        }

        // Login
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onLoginSuccess = {
                    navController.navigate(Routes.ACCOUNT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Register
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Routes.ACCOUNT) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        // Forgot Password
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Account
        composable(Routes.ACCOUNT) {
            AccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME)
                    }
                },
                isLoggedIn = isLoggedIn
            )
        }

        // Search
        composable(
            route = Routes.SEARCH,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProduct = { handle ->
                    navController.navigate(Routes.product(handle))
                },
                onNavigateToCart = { navController.navigate(Routes.CART) },
                cartCount = cartCount
            )
        }
    }
}
