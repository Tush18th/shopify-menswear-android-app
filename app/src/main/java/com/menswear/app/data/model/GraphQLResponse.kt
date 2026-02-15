package com.menswear.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Generic wrapper for Shopify Storefront GraphQL responses. */
@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class GraphQLResponse(
    val data: JsonElement? = null,
    val errors: List<GraphQLError>? = null
)

@Serializable
data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>? = null
)

@Serializable
data class GraphQLErrorLocation(
    val line: Int,
    val column: Int
)

/** Pagination info from Shopify connections. */
@Serializable
data class PageInfo(
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val endCursor: String? = null,
    val startCursor: String? = null
)

/** Generic paginated result wrapper. */
data class PaginatedResult<T>(
    val items: List<T>,
    val pageInfo: PageInfo
)

/** User error from mutations. */
@Serializable
data class UserError(
    val field: List<String>? = null,
    val message: String
)

@Serializable
data class CustomerUserError(
    val field: List<String>? = null,
    val message: String,
    val code: String? = null
)
