package com.menswear.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "menswear_prefs")

@Singleton
class AppDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store get() = context.dataStore

    // ── Cart ─────────────────────────────────────────────────────────────

    suspend fun saveCartId(cartId: String) {
        store.edit { it[KEY_CART_ID] = cartId }
    }

    suspend fun getCartId(): String? =
        store.data.map { it[KEY_CART_ID] }.first()

    suspend fun clearCartId() {
        store.edit { it.remove(KEY_CART_ID) }
    }

    // ── Customer Token ───────────────────────────────────────────────────

    suspend fun saveCustomerAccessToken(token: String) {
        store.edit { it[KEY_CUSTOMER_TOKEN] = token }
    }

    suspend fun getCustomerAccessToken(): String? =
        store.data.map { it[KEY_CUSTOMER_TOKEN] }.first()

    suspend fun clearCustomerAccessToken() {
        store.edit { it.remove(KEY_CUSTOMER_TOKEN) }
    }

    // ── FCM Token ────────────────────────────────────────────────────────

    suspend fun saveFcmToken(token: String) {
        store.edit { it[KEY_FCM_TOKEN] = token }
    }

    suspend fun getFcmToken(): String? =
        store.data.map { it[KEY_FCM_TOKEN] }.first()

    companion object {
        private val KEY_CART_ID = stringPreferencesKey("cart_id")
        private val KEY_CUSTOMER_TOKEN = stringPreferencesKey("customer_access_token")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }
}
