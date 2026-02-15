package com.menswear.app.data.repository

import com.menswear.app.data.api.ResponseParser
import com.menswear.app.data.api.ShopifyQueries
import com.menswear.app.data.api.StorefrontClient
import com.menswear.app.data.model.*
import com.menswear.app.util.AppResult
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val client: StorefrontClient
) {

    suspend fun getCollections(first: Int = 20): AppResult<List<Collection>> =
        AppResult.runCatching {
            val variables = buildJsonObject { put("first", first) }
            val response = client.execute(ShopifyQueries.GET_COLLECTIONS, variables)
            checkErrors(response)
            val collectionsData = response.data!!.jsonObject["collections"]!!
            ResponseParser.parseEdges(collectionsData)
        }

    private fun ResponseParser.parseEdges(element: JsonElement): List<Collection> {
        val edges = element.jsonObject["edges"]?.jsonArray ?: return emptyList()
        return edges.mapNotNull { edge ->
            try {
                ResponseParser.parseCollection(edge.jsonObject["node"]!!)
            } catch (_: Exception) { null }
        }
    }

    suspend fun getCollectionByHandle(
        handle: String,
        first: Int = 20,
        after: String? = null,
        sortKey: SortKey = SortKey.BEST_SELLING,
        filters: ProductFilters = ProductFilters()
    ): AppResult<Triple<Collection, PaginatedResult<Product>, AvailableFilters>> =
        AppResult.runCatching {
            val variables = buildJsonObject {
                put("handle", handle)
                put("first", first)
                after?.let { put("after", it) }
                put("sortKey", sortKey.storefrontValue)
                put("reverse", sortKey.reverse)
                if (filters.isActive) {
                    putJsonArray("filters") {
                        filters.toGraphQLFilters().forEach { filterMap ->
                            add(mapToJsonElement(filterMap))
                        }
                    }
                }
            }
            val response = client.execute(ShopifyQueries.GET_COLLECTION_BY_HANDLE, variables)
            checkErrors(response)
            val collectionData = response.data!!.jsonObject["collection"]
                ?: throw Exception("Collection not found")
            val (collection, paginatedProducts) = ResponseParser.parseCollectionWithProducts(collectionData)
            val availableFilters = ResponseParser.parseAvailableFilters(collectionData)
            Triple(collection, paginatedProducts, availableFilters)
        }

    suspend fun getProductByHandle(handle: String): AppResult<Product> =
        AppResult.runCatching {
            val variables = buildJsonObject { put("handle", handle) }
            val response = client.execute(ShopifyQueries.GET_PRODUCT_BY_HANDLE, variables)
            checkErrors(response)
            val productData = response.data!!.jsonObject["product"]
                ?: throw Exception("Product not found")
            ResponseParser.parseProduct(productData)
        }

    suspend fun searchProducts(
        query: String,
        first: Int = 20,
        after: String? = null
    ): AppResult<PaginatedResult<Product>> =
        AppResult.runCatching {
            val variables = buildJsonObject {
                put("query", query)
                put("first", first)
                after?.let { put("after", it) }
            }
            val response = client.execute(ShopifyQueries.SEARCH_PRODUCTS, variables)
            checkErrors(response)
            val searchData = response.data!!.jsonObject["search"]!!
            ResponseParser.parseSearchResults(searchData)
        }

    private fun checkErrors(response: GraphQLResponse) {
        response.errors?.let { errors ->
            if (errors.isNotEmpty()) {
                throw Exception(errors.joinToString("; ") { it.message })
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToJsonElement(map: Map<String, Any>): JsonElement {
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value.toDouble())
                    is Boolean -> put(key, value)
                    is Map<*, *> -> put(key, mapToJsonElement(value as Map<String, Any>))
                    else -> put(key, value.toString())
                }
            }
        }
    }
}
