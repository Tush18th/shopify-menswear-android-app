# Menswear - Android Shopping App

A native Android shopping application for a Men's Wear brand, fully integrated with Shopify as the backend. Built with Kotlin, Jetpack Compose, and Material 3.

## Architecture

```
app/
├── data/
│   ├── api/            # Shopify Storefront API client, GraphQL queries, response parser
│   ├── datastore/      # DataStore persistence (cart ID, tokens, preferences)
│   ├── model/          # Domain models (Product, Cart, Customer, etc.)
│   └── repository/     # ProductRepository, CartRepository, CustomerRepository
├── di/                 # Hilt dependency injection modules
├── service/            # Firebase Cloud Messaging service
├── ui/
│   ├── components/     # Reusable Compose components
│   ├── navigation/     # NavGraph, routes, deep link handler
│   ├── screens/        # Feature screens
│   │   ├── home/       # Home screen with hero, categories, product rails
│   │   ├── collection/ # Product listing (PLP) with filters & sorting
│   │   ├── product/    # Product detail (PDP) with gallery, variants, cart
│   │   ├── cart/       # Cart with line items, discounts, checkout
│   │   ├── auth/       # Login, Register, Forgot Password
│   │   ├── account/    # Account profile, orders, addresses
│   │   └── search/     # Product search with results grid
│   └── theme/          # Material 3 theme, typography, colors
└── util/               # AppResult wrapper
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| DI | Hilt |
| Network | Retrofit + OkHttp |
| Serialization | Kotlinx Serialization |
| Images | Coil |
| Persistence | DataStore Preferences |
| Navigation | Jetpack Navigation Compose |
| Push | Firebase Cloud Messaging |
| Checkout | Chrome Custom Tabs |

## Setup

### 1. Configure Shopify Credentials

Add your Shopify store credentials to `local.properties` (in the project root):

```properties
SHOP_DOMAIN=yourstore.myshopify.com
STOREFRONT_ACCESS_TOKEN=your-storefront-access-token
```

These values are read at build time and injected via `BuildConfig`.

**How to get a Storefront Access Token:**
1. Go to your Shopify admin → Settings → Apps and sales channels → Develop apps
2. Create a new app (or select existing)
3. Configure Storefront API scopes:
   - `unauthenticated_read_product_listings`
   - `unauthenticated_read_product_inventory`
   - `unauthenticated_read_collection_listings`
   - `unauthenticated_write_checkouts`
   - `unauthenticated_read_checkouts`
   - `unauthenticated_read_customers`
   - `unauthenticated_write_customers`
4. Install the app and copy the Storefront access token

### 2. Firebase (Optional for MVP)

To enable push notifications:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add your Android app (package: `com.menswear.app`)
3. Download `google-services.json` and place it in `app/`
4. Uncomment the `google-services` plugin in `app/build.gradle.kts`

Without Firebase configured, the app will still run — FCM initialization is wrapped in a try-catch.

### 3. Build & Run

```bash
# Open in Android Studio (Hedgehog or newer recommended)
# Or build from command line:
./gradlew assembleDebug

# Install on connected device/emulator:
./gradlew installDebug
```

**Requirements:**
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Min SDK 26 (Android 8.0)

## Features

### Home Screen
- Hero banner promoting latest collection
- Category grid (T-Shirts, Shirts, Denim, Trousers, Suits, Jackets, Footwear, Accessories)
- New Arrivals product rail
- Best Sellers product rail
- Search bar

### Collection / Product Listing (PLP)
- 2-column product grid
- Infinite scroll with cursor-based pagination
- Sort by: Best Selling, Newest, Price Low→High, Price High→Low
- Filter by: Size, Color, Price Range, Product Type, Vendor/Brand
- Filter drawer (bottom sheet) with active filter chips

### Product Detail (PDP)
- Horizontal paging image gallery with page indicators
- Color swatches (reads `custom.swatch_hex` metafield, falls back to color name mapping)
- Size selection chips
- Price display with compare-at / sale prices
- Quantity selector
- Add to Cart
- Collapsible sections: Description, Shipping & Returns, Size Guide

### Cart
- View line items with images, options, pricing
- Update quantity / remove items
- Apply discount codes
- Subtotal display
- Checkout → Opens Shopify secure checkout in Chrome Custom Tab

### Authentication
- Sign Up (creates Shopify customer)
- Sign In (customer access token)
- Forgot Password (sends Shopify reset email)
- Session persistence via DataStore

### Account
- Profile display with initials avatar
- Default address
- Recent orders list with status badges
- Order detail → opens Shopify order status page
- Sign Out

### Push Notifications
- Firebase Cloud Messaging integration
- Deep link routing from notification payload:
  - `product_handle` → Product screen
  - `collection_handle` → Collection screen
  - `url` → Generic deep link
- Token logging for backend integration

### Deep Links
- `https://yourstore.myshopify.com/products/{handle}` → Product screen
- `https://yourstore.myshopify.com/collections/{handle}` → Collection screen
- `menswear://product/{handle}` → Product screen
- `menswear://collection/{handle}` → Collection screen

## Testing Cart & Checkout

1. Browse products and add items to cart
2. Open the cart screen
3. Modify quantities, remove items, or apply a discount code
4. Tap "Checkout" — this opens Shopify's secure checkout in a browser Custom Tab
5. Complete the checkout using Shopify's test/sandbox credentials if in development mode

**Shopify Test Mode:**
- Enable "Test mode" in Shopify Payments settings
- Use Shopify's test credit card: `4242 4242 4242 4242`, any future expiry, any CVV

## Shopify API Details

- **API**: Storefront GraphQL API
- **Version**: 2024-01
- **Endpoint**: `https://{SHOP_DOMAIN}/api/2024-01/graphql.json`
- **Auth Header**: `X-Shopify-Storefront-Access-Token`

### Expected Collections (handles)
- `t-shirts`, `shirts`, `denim`, `trousers`, `suits`, `jackets`, `footwear`, `accessories`
- `new-arrivals` (optional, for home screen rail)
- `best-sellers` (optional, for home screen rail)

### Variant Metafield for Color Swatches
Products with color variants should have:
- Namespace: `custom`
- Key: `swatch_hex`
- Value: hex color string (e.g., `#0B0B0C`)

## Project Structure (Key Files)

| File | Purpose |
|------|---------|
| `ShopifyQueries.kt` | All GraphQL query/mutation strings |
| `StorefrontClient.kt` | GraphQL request executor |
| `ResponseParser.kt` | JSON → domain model parser |
| `ProductRepository.kt` | Products, collections, search |
| `CartRepository.kt` | Cart CRUD with Storefront Cart API |
| `CustomerRepository.kt` | Auth + customer data |
| `AppDataStore.kt` | Persistent preferences |
| `NetworkModule.kt` | Hilt module for Retrofit/OkHttp |
| `AppNavGraph.kt` | Navigation graph with deep links |

## License

Proprietary — All rights reserved.
