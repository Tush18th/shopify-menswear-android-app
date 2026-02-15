package com.menswear.app.data.api

import com.menswear.app.data.model.GraphQLRequest
import com.menswear.app.data.model.GraphQLResponse
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convenience wrapper around [StorefrontApi] that sends GraphQL queries
 * and returns the raw [GraphQLResponse].
 */
@Singleton
class StorefrontClient @Inject constructor(
    private val api: StorefrontApi
) {
    suspend fun execute(
        query: String,
        variables: Map<String, JsonElement> = emptyMap()
    ): GraphQLResponse {
        return api.execute(GraphQLRequest(query = query, variables = variables))
    }
}
