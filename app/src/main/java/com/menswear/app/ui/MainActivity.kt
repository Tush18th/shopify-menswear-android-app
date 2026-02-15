package com.menswear.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.menswear.app.data.datastore.AppDataStore
import com.menswear.app.data.repository.CartRepository
import com.menswear.app.data.repository.CustomerRepository
import com.menswear.app.ui.navigation.AppNavGraph
import com.menswear.app.ui.navigation.DeepLinkHandler
import com.menswear.app.ui.theme.MenswearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var cartRepository: CartRepository
    @Inject lateinit var customerRepository: CustomerRepository
    @Inject lateinit var dataStore: AppDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repositories
        lifecycleScope.launch {
            cartRepository.initialize()
            customerRepository.initialize()
        }

        // Request notification permission (Android 13+)
        requestNotificationPermission()

        // Retrieve and log FCM token
        retrieveFcmToken()

        setContent {
            MenswearTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Handle deep link from intent
                    LaunchedEffect(Unit) {
                        intent?.let { DeepLinkHandler.handleIntent(it, navController) }
                    }

                    AppNavGraph(
                        navController = navController,
                        cartRepository = cartRepository,
                        customerRepository = customerRepository
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun retrieveFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM token retrieval failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
                lifecycleScope.launch {
                    dataStore.saveFcmToken(token)
                }
            }
        } catch (e: Exception) {
            // Firebase not configured — skip silently in dev
            Log.w(TAG, "Firebase not initialized, skipping FCM token retrieval")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
