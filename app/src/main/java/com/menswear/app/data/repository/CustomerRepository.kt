package com.menswear.app.data.repository

import com.menswear.app.data.api.ResponseParser
import com.menswear.app.data.api.ShopifyQueries
import com.menswear.app.data.api.StorefrontClient
import com.menswear.app.data.datastore.AppDataStore
import com.menswear.app.data.model.Customer
import com.menswear.app.data.model.GraphQLResponse
import com.menswear.app.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val client: StorefrontClient,
    private val dataStore: AppDataStore
) {
    private val _customer = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customer.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    suspend fun initialize() {
        val token = dataStore.getCustomerAccessToken() ?: return
        when (val result = fetchCustomer(token)) {
            is AppResult.Success -> {
                _customer.value = result.data
                _isLoggedIn.value = true
            }
            is AppResult.Error -> {
                // Token expired
                dataStore.clearCustomerAccessToken()
                _isLoggedIn.value = false
            }
        }
    }

    suspend fun login(email: String, password: String): AppResult<Customer> =
        AppResult.runCatching {
            val variables = buildJsonObject {
                putJsonObject("input") {
                    put("email", email)
                    put("password", password)
                }
            }
            val response = client.execute(ShopifyQueries.CUSTOMER_ACCESS_TOKEN_CREATE, variables)
            checkErrors(response)
            val resultData = response.data!!.jsonObject["customerAccessTokenCreate"]!!.jsonObject

            val customerUserErrors = ResponseParser.parseCustomerUserErrors(resultData)
            if (customerUserErrors.isNotEmpty()) {
                throw Exception(customerUserErrors.joinToString("; ") { it.message })
            }

            val (token, _) = ResponseParser.parseAccessToken(resultData)
                ?: throw Exception("Failed to create access token")

            dataStore.saveCustomerAccessToken(token)
            val customer = fetchCustomerOrThrow(token)
            _customer.value = customer
            _isLoggedIn.value = true
            customer
        }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AppResult<Customer> =
        AppResult.runCatching {
            val variables = buildJsonObject {
                putJsonObject("input") {
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("email", email)
                    put("password", password)
                }
            }
            val response = client.execute(ShopifyQueries.CUSTOMER_CREATE, variables)
            checkErrors(response)
            val resultData = response.data!!.jsonObject["customerCreate"]!!.jsonObject

            val customerUserErrors = ResponseParser.parseCustomerUserErrors(resultData)
            if (customerUserErrors.isNotEmpty()) {
                throw Exception(customerUserErrors.joinToString("; ") { it.message })
            }

            // After registration, log in automatically
            val loginResult = login(email, password)
            if (loginResult is AppResult.Error) {
                throw Exception(loginResult.message)
            }
            (loginResult as AppResult.Success).data
        }

    suspend fun recoverPassword(email: String): AppResult<Unit> =
        AppResult.runCatching {
            val variables = buildJsonObject { put("email", email) }
            val response = client.execute(ShopifyQueries.CUSTOMER_RECOVER, variables)
            checkErrors(response)
            val resultData = response.data!!.jsonObject["customerRecover"]!!.jsonObject
            val customerUserErrors = ResponseParser.parseCustomerUserErrors(resultData)
            if (customerUserErrors.isNotEmpty()) {
                throw Exception(customerUserErrors.joinToString("; ") { it.message })
            }
        }

    suspend fun logout(): AppResult<Unit> =
        AppResult.runCatching {
            val token = dataStore.getCustomerAccessToken()
            if (token != null) {
                val variables = buildJsonObject { put("customerAccessToken", token) }
                client.execute(ShopifyQueries.CUSTOMER_ACCESS_TOKEN_DELETE, variables)
            }
            dataStore.clearCustomerAccessToken()
            _customer.value = null
            _isLoggedIn.value = false
        }

    suspend fun refreshCustomer(): AppResult<Customer> {
        val token = dataStore.getCustomerAccessToken()
            ?: return AppResult.Error("Not logged in")
        return fetchCustomer(token).also { result ->
            if (result is AppResult.Success) {
                _customer.value = result.data
            }
        }
    }

    private suspend fun fetchCustomer(token: String): AppResult<Customer> =
        AppResult.runCatching {
            fetchCustomerOrThrow(token)
        }

    private suspend fun fetchCustomerOrThrow(token: String): Customer {
        val variables = buildJsonObject { put("customerAccessToken", token) }
        val response = client.execute(ShopifyQueries.GET_CUSTOMER, variables)
        checkErrors(response)
        val customerData = response.data!!.jsonObject["customer"]
            ?: throw Exception("Customer not found")
        return ResponseParser.parseCustomer(customerData)
    }

    private fun checkErrors(response: GraphQLResponse) {
        response.errors?.let { errors ->
            if (errors.isNotEmpty()) {
                throw Exception(errors.joinToString("; ") { it.message })
            }
        }
    }
}
