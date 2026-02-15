package com.menswear.app.data.api

/**
 * All Shopify Storefront GraphQL query and mutation strings.
 * Organized by feature domain.
 */
object ShopifyQueries {

    // ── Fragments ────────────────────────────────────────────────────────

    private const val MONEY_FRAGMENT = """
        fragment MoneyFields on MoneyV2 {
            amount
            currencyCode
        }
    """

    private const val IMAGE_FRAGMENT = """
        fragment ImageFields on Image {
            id
            url
            altText
            width
            height
        }
    """

    private const val VARIANT_FRAGMENT = """
        fragment VariantFields on ProductVariant {
            id
            title
            availableForSale
            price {
                ...MoneyFields
            }
            compareAtPrice {
                ...MoneyFields
            }
            selectedOptions {
                name
                value
            }
            image {
                ...ImageFields
            }
            swatchHex: metafield(namespace: "custom", key: "swatch_hex") {
                value
            }
        }
    """

    private const val PRODUCT_CARD_FRAGMENT = """
        fragment ProductCardFields on Product {
            id
            title
            handle
            vendor
            productType
            availableForSale
            priceRange {
                minVariantPrice { ...MoneyFields }
                maxVariantPrice { ...MoneyFields }
            }
            compareAtPriceRange {
                minVariantPrice { ...MoneyFields }
                maxVariantPrice { ...MoneyFields }
            }
            images(first: 1) {
                edges {
                    node { ...ImageFields }
                }
            }
            variants(first: 10) {
                edges {
                    node { ...VariantFields }
                }
            }
        }
    """

    // ── Collections ──────────────────────────────────────────────────────

    const val GET_COLLECTION_BY_HANDLE = """
        query GetCollectionByHandle(
            ${'$'}handle: String!,
            ${'$'}first: Int!,
            ${'$'}after: String,
            ${'$'}sortKey: ProductCollectionSortKeys,
            ${'$'}reverse: Boolean,
            ${'$'}filters: [ProductFilter!]
        ) {
            collection(handle: ${'$'}handle) {
                id
                title
                handle
                description
                image { ...ImageFields }
                products(
                    first: ${'$'}first,
                    after: ${'$'}after,
                    sortKey: ${'$'}sortKey,
                    reverse: ${'$'}reverse,
                    filters: ${'$'}filters
                ) {
                    edges {
                        cursor
                        node { ...ProductCardFields }
                    }
                    pageInfo {
                        hasNextPage
                        endCursor
                    }
                    filters {
                        id
                        label
                        type
                        values {
                            id
                            label
                            count
                            input
                        }
                    }
                }
            }
        }
        $MONEY_FRAGMENT
        $IMAGE_FRAGMENT
        $VARIANT_FRAGMENT
        $PRODUCT_CARD_FRAGMENT
    """

    const val GET_COLLECTIONS = """
        query GetCollections(${'$'}first: Int!) {
            collections(first: ${'$'}first) {
                edges {
                    node {
                        id
                        title
                        handle
                        description
                        image { ...ImageFields }
                    }
                }
            }
        }
        $IMAGE_FRAGMENT
    """

    // ── Product Detail ───────────────────────────────────────────────────

    const val GET_PRODUCT_BY_HANDLE = """
        query GetProductByHandle(${'$'}handle: String!) {
            product(handle: ${'$'}handle) {
                id
                title
                handle
                description
                descriptionHtml
                vendor
                productType
                tags
                availableForSale
                priceRange {
                    minVariantPrice { ...MoneyFields }
                    maxVariantPrice { ...MoneyFields }
                }
                compareAtPriceRange {
                    minVariantPrice { ...MoneyFields }
                    maxVariantPrice { ...MoneyFields }
                }
                images(first: 20) {
                    edges {
                        node { ...ImageFields }
                    }
                }
                variants(first: 100) {
                    edges {
                        node { ...VariantFields }
                    }
                }
            }
        }
        $MONEY_FRAGMENT
        $IMAGE_FRAGMENT
        $VARIANT_FRAGMENT
    """

    // ── Search ───────────────────────────────────────────────────────────

    const val SEARCH_PRODUCTS = """
        query SearchProducts(${'$'}query: String!, ${'$'}first: Int!, ${'$'}after: String) {
            search(query: ${'$'}query, first: ${'$'}first, after: ${'$'}after, types: [PRODUCT]) {
                edges {
                    node {
                        ... on Product {
                            ...ProductCardFields
                        }
                    }
                }
                pageInfo {
                    hasNextPage
                    endCursor
                }
                totalCount
            }
        }
        $MONEY_FRAGMENT
        $IMAGE_FRAGMENT
        $VARIANT_FRAGMENT
        $PRODUCT_CARD_FRAGMENT
    """

    // ── Cart ─────────────────────────────────────────────────────────────

    private const val CART_FRAGMENT = """
        fragment CartFields on Cart {
            id
            checkoutUrl
            totalQuantity
            cost {
                subtotalAmount { ...MoneyFields }
                totalAmount { ...MoneyFields }
                totalTaxAmount { ...MoneyFields }
            }
            discountCodes {
                code
                applicable
            }
            lines(first: 100) {
                edges {
                    node {
                        id
                        quantity
                        cost {
                            totalAmount { ...MoneyFields }
                            amountPerQuantity { ...MoneyFields }
                        }
                        merchandise {
                            ... on ProductVariant {
                                id
                                title
                                product {
                                    id
                                    title
                                    handle
                                }
                                selectedOptions {
                                    name
                                    value
                                }
                                image { ...ImageFields }
                                price { ...MoneyFields }
                            }
                        }
                    }
                }
            }
        }
        $MONEY_FRAGMENT
        $IMAGE_FRAGMENT
    """

    const val CREATE_CART = """
        mutation CreateCart(${'$'}lines: [CartLineInput!]!) {
            cartCreate(input: { lines: ${'$'}lines }) {
                cart { ...CartFields }
                userErrors { field message }
            }
        }
        $CART_FRAGMENT
    """

    const val GET_CART = """
        query GetCart(${'$'}cartId: ID!) {
            cart(id: ${'$'}cartId) { ...CartFields }
        }
        $CART_FRAGMENT
    """

    const val ADD_TO_CART = """
        mutation AddToCart(${'$'}cartId: ID!, ${'$'}lines: [CartLineInput!]!) {
            cartLinesAdd(cartId: ${'$'}cartId, lines: ${'$'}lines) {
                cart { ...CartFields }
                userErrors { field message }
            }
        }
        $CART_FRAGMENT
    """

    const val UPDATE_CART_LINE = """
        mutation UpdateCartLine(${'$'}cartId: ID!, ${'$'}lines: [CartLineUpdateInput!]!) {
            cartLinesUpdate(cartId: ${'$'}cartId, lines: ${'$'}lines) {
                cart { ...CartFields }
                userErrors { field message }
            }
        }
        $CART_FRAGMENT
    """

    const val REMOVE_CART_LINE = """
        mutation RemoveCartLine(${'$'}cartId: ID!, ${'$'}lineIds: [ID!]!) {
            cartLinesRemove(cartId: ${'$'}cartId, lineIds: ${'$'}lineIds) {
                cart { ...CartFields }
                userErrors { field message }
            }
        }
        $CART_FRAGMENT
    """

    const val APPLY_DISCOUNT = """
        mutation ApplyDiscount(${'$'}cartId: ID!, ${'$'}discountCodes: [String!]!) {
            cartDiscountCodesUpdate(cartId: ${'$'}cartId, discountCodes: ${'$'}discountCodes) {
                cart { ...CartFields }
                userErrors { field message }
            }
        }
        $CART_FRAGMENT
    """

    // ── Customer Auth ────────────────────────────────────────────────────

    const val CUSTOMER_CREATE = """
        mutation CustomerCreate(${'$'}input: CustomerCreateInput!) {
            customerCreate(input: ${'$'}input) {
                customer {
                    id
                    email
                    firstName
                    lastName
                }
                customerUserErrors {
                    field
                    message
                    code
                }
            }
        }
    """

    const val CUSTOMER_ACCESS_TOKEN_CREATE = """
        mutation CustomerAccessTokenCreate(${'$'}input: CustomerAccessTokenCreateInput!) {
            customerAccessTokenCreate(input: ${'$'}input) {
                customerAccessToken {
                    accessToken
                    expiresAt
                }
                customerUserErrors {
                    field
                    message
                    code
                }
            }
        }
    """

    const val CUSTOMER_RECOVER = """
        mutation CustomerRecover(${'$'}email: String!) {
            customerRecover(email: ${'$'}email) {
                customerUserErrors {
                    field
                    message
                    code
                }
            }
        }
    """

    const val GET_CUSTOMER = """
        query GetCustomer(${'$'}customerAccessToken: String!) {
            customer(customerAccessToken: ${'$'}customerAccessToken) {
                id
                firstName
                lastName
                email
                phone
                defaultAddress {
                    id
                    firstName
                    lastName
                    address1
                    address2
                    city
                    province
                    country
                    zip
                    phone
                }
                addresses(first: 10) {
                    edges {
                        node {
                            id
                            firstName
                            lastName
                            address1
                            address2
                            city
                            province
                            country
                            zip
                            phone
                        }
                    }
                }
                orders(first: 20, sortKey: PROCESSED_AT, reverse: true) {
                    edges {
                        node {
                            id
                            orderNumber
                            name
                            processedAt
                            financialStatus
                            fulfillmentStatus
                            statusUrl
                            totalPrice { ...MoneyFields }
                            lineItems(first: 10) {
                                edges {
                                    node {
                                        title
                                        quantity
                                        variant {
                                            title
                                            price { ...MoneyFields }
                                            image { ...ImageFields }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        $MONEY_FRAGMENT
        $IMAGE_FRAGMENT
    """

    const val CUSTOMER_ACCESS_TOKEN_DELETE = """
        mutation CustomerAccessTokenDelete(${'$'}customerAccessToken: String!) {
            customerAccessTokenDelete(customerAccessToken: ${'$'}customerAccessToken) {
                deletedAccessToken
                userErrors {
                    field
                    message
                }
            }
        }
    """
}
