package com.menswear.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController

/**
 * Handles deep links from push notifications and external intents.
 *
 * Supported URI patterns:
 *   - https://{domain}/products/{handle}
 *   - https://{domain}/collections/{handle}
 *   - menswear://product/{handle}
 *   - menswear://collection/{handle}
 */
object DeepLinkHandler {

    fun handleIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: return
        handleUri(uri, navController)
    }

    fun handleUri(uri: Uri, navController: NavHostController) {
        val pathSegments = uri.pathSegments

        when (uri.scheme) {
            "menswear" -> {
                when (uri.host) {
                    "product" -> {
                        val handle = pathSegments.firstOrNull() ?: return
                        navController.navigate(Routes.product(handle))
                    }
                    "collection" -> {
                        val handle = pathSegments.firstOrNull() ?: return
                        navController.navigate(Routes.collection(handle))
                    }
                }
            }
            "https", "http" -> {
                when {
                    pathSegments.size >= 2 && pathSegments[0] == "products" -> {
                        navController.navigate(Routes.product(pathSegments[1]))
                    }
                    pathSegments.size >= 2 && pathSegments[0] == "collections" -> {
                        navController.navigate(Routes.collection(pathSegments[1]))
                    }
                }
            }
        }
    }
}
