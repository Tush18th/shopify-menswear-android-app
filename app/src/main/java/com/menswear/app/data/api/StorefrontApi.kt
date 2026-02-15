package com.menswear.app.data.api

import com.menswear.app.data.model.GraphQLRequest
import com.menswear.app.data.model.GraphQLResponse
import retrofit2.http.Body
import retrofit2.http.POST

/** Single Retrofit endpoint for all Shopify Storefront GraphQL requests. */
interface StorefrontApi {

    @POST("graphql.json")
    suspend fun execute(@Body request: GraphQLRequest): GraphQLResponse
}
