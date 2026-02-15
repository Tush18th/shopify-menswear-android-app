package com.menswear.app.data.api

import com.menswear.app.data.model.*
import kotlinx.serialization.json.*

/**
 * Parses raw [JsonElement] responses from the Storefront API into domain models.
 * Centralizes all JSON traversal logic.
 */
object ResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Products ─────────────────────────────────────────────────────────

    fun parseProduct(element: JsonElement): Product {
        val obj = element.jsonObject
        return Product(
            id = obj.str("id"),
            title = obj.str("title"),
            handle = obj.str("handle"),
            description = obj.strOrEmpty("description"),
            descriptionHtml = obj.strOrEmpty("descriptionHtml"),
            vendor = obj.strOrEmpty("vendor"),
            productType = obj.strOrEmpty("productType"),
            tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            availableForSale = obj["availableForSale"]?.jsonPrimitive?.booleanOrNull ?: true,
            images = parseEdges(obj["images"]) { parseImage(it) },
            variants = parseEdges(obj["variants"]) { parseVariant(it) },
            priceRange = obj["priceRange"]?.let { parsePriceRange(it) },
            compareAtPriceRange = obj["compareAtPriceRange"]?.let { parseCompareAtPriceRange(it) }
        )
    }

    fun parseProductList(element: JsonElement): PaginatedResult<Product> {
        val products = element.jsonObject
        val items = parseEdges(products) { parseProduct(it) }
        val pageInfo = parsePageInfo(products["pageInfo"])
        return PaginatedResult(items, pageInfo)
    }

    private fun parseVariant(element: JsonElement): Variant {
        val obj = element.jsonObject
        return Variant(
            id = obj.str("id"),
            title = obj.str("title"),
            availableForSale = obj["availableForSale"]?.jsonPrimitive?.booleanOrNull ?: true,
            price = parseMoney(obj["price"]!!),
            compareAtPrice = obj["compareAtPrice"]?.takeIf { it !is JsonNull }?.let { parseMoney(it) },
            selectedOptions = obj["selectedOptions"]?.jsonArray?.map { parseSelectedOption(it) } ?: emptyList(),
            image = obj["image"]?.takeIf { it !is JsonNull }?.let { parseImage(it) },
            swatchHex = obj["swatchHex"]?.takeIf { it !is JsonNull }?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
        )
    }

    private fun parseSelectedOption(element: JsonElement): SelectedOption {
        val obj = element.jsonObject
        return SelectedOption(
            name = obj.str("name"),
            value = obj.str("value")
        )
    }

    fun parseImage(element: JsonElement): ProductImage {
        val obj = element.jsonObject
        return ProductImage(
            id = obj["id"]?.jsonPrimitive?.contentOrNull,
            url = obj.str("url"),
            altText = obj["altText"]?.jsonPrimitive?.contentOrNull,
            width = obj["width"]?.jsonPrimitive?.intOrNull,
            height = obj["height"]?.jsonPrimitive?.intOrNull
        )
    }

    // ── Collections ──────────────────────────────────────────────────────

    fun parseCollection(element: JsonElement): Collection {
        val obj = element.jsonObject
        return Collection(
            id = obj.str("id"),
            title = obj.str("title"),
            handle = obj.str("handle"),
            description = obj.strOrEmpty("description"),
            image = obj["image"]?.takeIf { it !is JsonNull }?.let { parseImage(it) }
        )
    }

    fun parseCollectionWithProducts(element: JsonElement): Pair<Collection, PaginatedResult<Product>> {
        val obj = element.jsonObject
        val collection = parseCollection(element)
        val productsConn = obj["products"] ?: return collection to PaginatedResult(emptyList(), PageInfo())
        val paginatedProducts = parseProductList(productsConn)
        return collection to paginatedProducts
    }

    fun parseAvailableFilters(element: JsonElement): AvailableFilters {
        val productsObj = element.jsonObject["products"] ?: return AvailableFilters()
        val filtersArray = productsObj.jsonObject["filters"]?.jsonArray ?: return AvailableFilters()

        val sizes = mutableListOf<String>()
        val colors = mutableListOf<String>()
        val vendors = mutableListOf<String>()
        val productTypes = mutableListOf<String>()
        var minPrice = 0.0
        var maxPrice = 1000.0

        for (filter in filtersArray) {
            val filterObj = filter.jsonObject
            val label = filterObj.strOrEmpty("label").lowercase()
            val values = filterObj["values"]?.jsonArray ?: continue

            when {
                label.contains("size") -> {
                    values.forEach { v ->
                        val vLabel = v.jsonObject.strOrEmpty("label")
                        val count = v.jsonObject["count"]?.jsonPrimitive?.intOrNull ?: 0
                        if (count > 0 && vLabel.isNotBlank()) sizes.add(vLabel)
                    }
                }
                label.contains("color") || label.contains("colour") -> {
                    values.forEach { v ->
                        val vLabel = v.jsonObject.strOrEmpty("label")
                        val count = v.jsonObject["count"]?.jsonPrimitive?.intOrNull ?: 0
                        if (count > 0 && vLabel.isNotBlank()) colors.add(vLabel)
                    }
                }
                label.contains("vendor") || label.contains("brand") -> {
                    values.forEach { v ->
                        val vLabel = v.jsonObject.strOrEmpty("label")
                        val count = v.jsonObject["count"]?.jsonPrimitive?.intOrNull ?: 0
                        if (count > 0 && vLabel.isNotBlank()) vendors.add(vLabel)
                    }
                }
                label.contains("type") -> {
                    values.forEach { v ->
                        val vLabel = v.jsonObject.strOrEmpty("label")
                        val count = v.jsonObject["count"]?.jsonPrimitive?.intOrNull ?: 0
                        if (count > 0 && vLabel.isNotBlank()) productTypes.add(vLabel)
                    }
                }
                label.contains("price") -> {
                    values.forEach { v ->
                        val input = v.jsonObject["input"]?.jsonPrimitive?.contentOrNull
                        if (input != null) {
                            try {
                                val inputObj = Json.parseToJsonElement(input).jsonObject
                                val priceObj = inputObj["price"]?.jsonObject
                                priceObj?.get("min")?.jsonPrimitive?.doubleOrNull?.let {
                                    if (it < minPrice || minPrice == 0.0) minPrice = it
                                }
                                priceObj?.get("max")?.jsonPrimitive?.doubleOrNull?.let {
                                    if (it > maxPrice) maxPrice = it
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }

        return AvailableFilters(
            sizes = sizes,
            colors = colors,
            vendors = vendors,
            productTypes = productTypes,
            minPrice = minPrice,
            maxPrice = maxPrice
        )
    }

    // ── Cart ─────────────────────────────────────────────────────────────

    fun parseCart(element: JsonElement): Cart {
        val obj = element.jsonObject
        return Cart(
            id = obj.str("id"),
            checkoutUrl = obj.strOrEmpty("checkoutUrl"),
            totalQuantity = obj["totalQuantity"]?.jsonPrimitive?.intOrNull ?: 0,
            cost = obj["cost"]?.let { parseCartCost(it) },
            lines = parseEdges(obj["lines"]) { parseCartLine(it) },
            discountCodes = obj["discountCodes"]?.jsonArray?.map { parseDiscountCode(it) } ?: emptyList()
        )
    }

    private fun parseCartLine(element: JsonElement): CartLine {
        val obj = element.jsonObject
        val merchObj = obj["merchandise"]!!.jsonObject
        return CartLine(
            id = obj.str("id"),
            quantity = obj["quantity"]?.jsonPrimitive?.intOrNull ?: 1,
            merchandise = CartMerchandise(
                id = merchObj.str("id"),
                title = merchObj.str("title"),
                product = parseCartProduct(merchObj["product"]!!),
                selectedOptions = merchObj["selectedOptions"]?.jsonArray?.map { parseSelectedOption(it) } ?: emptyList(),
                image = merchObj["image"]?.takeIf { it !is JsonNull }?.let { parseImage(it) },
                price = parseMoney(merchObj["price"]!!)
            ),
            cost = obj["cost"]?.let { costObj ->
                CartLineCost(
                    totalAmount = parseMoney(costObj.jsonObject["totalAmount"]!!),
                    amountPerQuantity = parseMoney(costObj.jsonObject["amountPerQuantity"]!!)
                )
            }
        )
    }

    private fun parseCartProduct(element: JsonElement): CartProduct {
        val obj = element.jsonObject
        return CartProduct(
            id = obj.str("id"),
            title = obj.str("title"),
            handle = obj.str("handle")
        )
    }

    private fun parseCartCost(element: JsonElement): CartCost {
        val obj = element.jsonObject
        return CartCost(
            subtotalAmount = parseMoney(obj["subtotalAmount"]!!),
            totalAmount = parseMoney(obj["totalAmount"]!!),
            totalTaxAmount = obj["totalTaxAmount"]?.takeIf { it !is JsonNull }?.let { parseMoney(it) }
        )
    }

    private fun parseDiscountCode(element: JsonElement): CartDiscountCode {
        val obj = element.jsonObject
        return CartDiscountCode(
            code = obj.str("code"),
            applicable = obj["applicable"]?.jsonPrimitive?.booleanOrNull ?: false
        )
    }

    // ── Customer ─────────────────────────────────────────────────────────

    fun parseCustomer(element: JsonElement): Customer {
        val obj = element.jsonObject
        return Customer(
            id = obj.str("id"),
            firstName = obj.strOrEmpty("firstName"),
            lastName = obj.strOrEmpty("lastName"),
            email = obj.strOrEmpty("email"),
            phone = obj["phone"]?.jsonPrimitive?.contentOrNull,
            defaultAddress = obj["defaultAddress"]?.takeIf { it !is JsonNull }?.let { parseAddress(it) },
            addresses = parseEdges(obj["addresses"]) { parseAddress(it) },
            orders = parseEdges(obj["orders"]) { parseOrder(it) }
        )
    }

    private fun parseAddress(element: JsonElement): Address {
        val obj = element.jsonObject
        return Address(
            id = obj.strOrEmpty("id"),
            firstName = obj.strOrEmpty("firstName"),
            lastName = obj.strOrEmpty("lastName"),
            address1 = obj.strOrEmpty("address1"),
            address2 = obj["address2"]?.jsonPrimitive?.contentOrNull,
            city = obj.strOrEmpty("city"),
            province = obj.strOrEmpty("province"),
            country = obj.strOrEmpty("country"),
            zip = obj.strOrEmpty("zip"),
            phone = obj["phone"]?.jsonPrimitive?.contentOrNull
        )
    }

    private fun parseOrder(element: JsonElement): Order {
        val obj = element.jsonObject
        return Order(
            id = obj.str("id"),
            orderNumber = obj["orderNumber"]?.jsonPrimitive?.intOrNull ?: 0,
            name = obj.strOrEmpty("name"),
            processedAt = obj.strOrEmpty("processedAt"),
            financialStatus = obj.strOrEmpty("financialStatus"),
            fulfillmentStatus = obj.strOrEmpty("fulfillmentStatus"),
            totalPrice = parseMoney(obj["totalPrice"]!!),
            lineItems = parseEdges(obj["lineItems"]) { parseOrderLineItem(it) },
            statusUrl = obj.strOrEmpty("statusUrl")
        )
    }

    private fun parseOrderLineItem(element: JsonElement): OrderLineItem {
        val obj = element.jsonObject
        return OrderLineItem(
            title = obj.str("title"),
            quantity = obj["quantity"]?.jsonPrimitive?.intOrNull ?: 1,
            variant = obj["variant"]?.takeIf { it !is JsonNull }?.let { parseOrderVariant(it) }
        )
    }

    private fun parseOrderVariant(element: JsonElement): OrderVariant {
        val obj = element.jsonObject
        return OrderVariant(
            title = obj.str("title"),
            image = obj["image"]?.takeIf { it !is JsonNull }?.let { parseImage(it) },
            price = parseMoney(obj["price"]!!)
        )
    }

    fun parseAccessToken(element: JsonElement): Pair<String, String>? {
        val tokenObj = element.jsonObject["customerAccessToken"] ?: return null
        if (tokenObj is JsonNull) return null
        val accessToken = tokenObj.jsonObject.str("accessToken")
        val expiresAt = tokenObj.jsonObject.str("expiresAt")
        return accessToken to expiresAt
    }

    fun parseCustomerUserErrors(element: JsonElement): List<CustomerUserError> {
        val errors = element.jsonObject["customerUserErrors"]?.jsonArray ?: return emptyList()
        return errors.map {
            val obj = it.jsonObject
            CustomerUserError(
                field = obj["field"]?.jsonArray?.map { f -> f.jsonPrimitive.content },
                message = obj.str("message"),
                code = obj["code"]?.jsonPrimitive?.contentOrNull
            )
        }
    }

    fun parseUserErrors(element: JsonElement): List<UserError> {
        val errors = element.jsonObject["userErrors"]?.jsonArray ?: return emptyList()
        return errors.map {
            val obj = it.jsonObject
            UserError(
                field = obj["field"]?.jsonArray?.map { f -> f.jsonPrimitive.content },
                message = obj.str("message")
            )
        }
    }

    // ── Search ───────────────────────────────────────────────────────────

    fun parseSearchResults(element: JsonElement): PaginatedResult<Product> {
        val searchObj = element.jsonObject
        val items = parseEdges(searchObj) { parseProduct(it) }
        val pageInfo = parsePageInfo(searchObj["pageInfo"])
        return PaginatedResult(items, pageInfo)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun parseMoney(element: JsonElement): MoneyV2 {
        val obj = element.jsonObject
        return MoneyV2(
            amount = obj.str("amount"),
            currencyCode = obj.str("currencyCode")
        )
    }

    private fun parsePriceRange(element: JsonElement): PriceRange {
        val obj = element.jsonObject
        return PriceRange(
            minVariantPrice = parseMoney(obj["minVariantPrice"]!!),
            maxVariantPrice = parseMoney(obj["maxVariantPrice"]!!)
        )
    }

    private fun parseCompareAtPriceRange(element: JsonElement): CompareAtPriceRange {
        val obj = element.jsonObject
        return CompareAtPriceRange(
            minVariantPrice = parseMoney(obj["minVariantPrice"]!!),
            maxVariantPrice = parseMoney(obj["maxVariantPrice"]!!)
        )
    }

    private fun parsePageInfo(element: JsonElement?): PageInfo {
        if (element == null || element is JsonNull) return PageInfo()
        val obj = element.jsonObject
        return PageInfo(
            hasNextPage = obj["hasNextPage"]?.jsonPrimitive?.booleanOrNull ?: false,
            hasPreviousPage = obj["hasPreviousPage"]?.jsonPrimitive?.booleanOrNull ?: false,
            endCursor = obj["endCursor"]?.jsonPrimitive?.contentOrNull,
            startCursor = obj["startCursor"]?.jsonPrimitive?.contentOrNull
        )
    }

    private fun <T> parseEdges(element: JsonElement?, mapper: (JsonElement) -> T): List<T> {
        if (element == null || element is JsonNull) return emptyList()
        val edges = element.jsonObject["edges"]?.jsonArray ?: return emptyList()
        return edges.mapNotNull { edge ->
            try {
                mapper(edge.jsonObject["node"]!!)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.jsonPrimitive?.content ?: ""

    private fun JsonObject.strOrEmpty(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: ""
}
