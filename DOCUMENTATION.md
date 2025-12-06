# FoalRider E-Commerce API - Complete Documentation

**Version**: 1.0.0  
**Last Updated**: December 6, 2025  
**Base URL**: `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [Database Schema](#4-database-schema)
5. [Module Documentation](#5-module-documentation)
   - [User Module](#51-user-module)
   - [Product Module](#52-product-module)
   - [Pricing Module](#53-pricing-module)
   - [Cart Module](#54-cart-module)
   - [Order Module](#55-order-module)
   - [Review Module](#56-review-module)
   - [Notification Module](#57-notification-module)
   - [Payment Module](#58-payment-module)
   - [Admin Module](#59-admin-module)
6. [API Endpoints Reference](#6-api-endpoints-reference)
7. [Request/Response Examples](#7-requestresponse-examples)
8. [Error Handling](#8-error-handling)
9. [Frontend Integration Guide](#9-frontend-integration-guide)

---

## 1. Project Overview

FoalRider is a modern, full-featured e-commerce platform API built with Spring Boot 3.2. It supports:

- **Multi-currency pricing** with automatic conversion
- **Regional pricing** (different prices per country/region)
- **Role-based access control** (RBAC)
- **JWT authentication** with refresh tokens
- **Stripe payment integration**
- **Product variants** (sizes, colors)
- **Shopping cart with tax & shipping calculation**
- **Order management with status tracking**
- **Product reviews & ratings**
- **Admin dashboard & analytics**

### Tech Stack

| Component | Technology            |
| --------- | --------------------- |
| Framework | Spring Boot 3.2       |
| Language  | Java 21               |
| Database  | PostgreSQL (Supabase) |
| Cache     | Redis (optional)      |
| Auth      | JWT (HS512)           |
| Payment   | Stripe                |
| Docs      | OpenAPI/Swagger       |

---

## 2. Architecture

### Module Structure

```
com.foalrider/
├── config/              # Configuration classes
│   ├── SecurityConfig   # Spring Security configuration
│   ├── JwtConfig        # JWT settings
│   ├── RedisConfig      # Redis cache config
│   ├── StripeConfig     # Stripe API config
│   └── DataInitializer  # Default data seeding
│
├── security/            # Security components
│   ├── CustomUserDetails
│   ├── JwtTokenProvider
│   └── JwtAuthenticationFilter
│
├── shared/              # Shared utilities
│   ├── dto/             # Common DTOs (ApiResponse, PagedResponse)
│   ├── entity/          # BaseEntity
│   ├── exception/       # Custom exceptions
│   └── util/            # Utilities (SlugUtils, etc.)
│
└── modules/
    ├── auth/            # Authentication
    ├── user/            # User management
    ├── product/         # Products, Categories, Brands
    ├── pricing/         # Currency, Regions, Tax, Shipping
    ├── cart/            # Shopping cart
    ├── order/           # Orders
    ├── payment/         # Stripe payments
    ├── review/          # Product reviews
    ├── notification/    # User notifications
    └── admin/           # Admin dashboard
```

### API Response Format

All API responses follow a consistent format:

```json
{
  "success": true,
  "message": "Optional success message",
  "data": {
    /* Response payload */
  },
  "timestamp": "2025-12-06T08:00:00.000Z"
}
```

Error responses:

```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error message",
  "path": "/api/v1/endpoint",
  "timestamp": "2025-12-06T08:00:00.000Z"
}
```

---

## 3. Authentication & Authorization

### 3.1 Roles & Permissions

| Role               | Description      | Permissions                                             |
| ------------------ | ---------------- | ------------------------------------------------------- |
| `ROLE_CUSTOMER`    | Regular customer | View products, manage cart, place orders, write reviews |
| `ROLE_VENDOR`      | Seller/Vendor    | Manage own products, view orders                        |
| `ROLE_ADMIN`       | Administrator    | Full access to admin dashboard, manage all resources    |
| `ROLE_SUPER_ADMIN` | Super Admin      | All permissions (`*`)                                   |

### 3.2 JWT Authentication

**Token Structure:**

- **Algorithm**: HS512
- **Access Token Expiry**: 1 hour (dev) / 15 minutes (prod)
- **Refresh Token Expiry**: 7 days

**JWT Payload:**

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "ADMIN",
  "firstName": "John",
  "iat": 1765010000,
  "exp": 1765013600,
  "iss": "foalrider-api"
}
```

### 3.3 Authentication Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   POST /login   │────▶│  Validate Creds │────▶│  Generate JWT   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                        ┌─────────────────┐            │
                        │   Return Tokens │◀───────────┘
                        │ + User Info     │
                        └─────────────────┘
```

### 3.4 Using Tokens

Include the access token in the `Authorization` header:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### 3.5 Refresh Token Flow

When access token expires, use refresh token to get a new one:

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "00460757-55cb-4ba6-93c2-2b50585262d2"
}
```

---

## 4. Database Schema

### 4.1 Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    roles     │       │    users     │       │  refresh_    │
│──────────────│       │──────────────│       │  tokens      │
│ id (PK)      │◀──────│ role_id (FK) │       │──────────────│
│ name         │       │ id (PK)      │◀──────│ user_id (FK) │
│ description  │       │ email        │       │ token        │
│ permissions  │       │ password_hash│       │ expires_at   │
│ is_system    │       │ first_name   │       └──────────────┘
└──────────────┘       │ last_name    │
                       │ phone        │
                       │ region_code  │
                       │ locale       │
                       └──────────────┘
                              │
                              ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    carts     │       │  cart_items  │       │   orders     │
│──────────────│       │──────────────│       │──────────────│
│ id (PK)      │◀──────│ cart_id (FK) │       │ id (PK)      │
│ user_id (FK) │       │ product_id   │       │ user_id (FK) │
│ total_items  │       │ variant_id   │       │ order_number │
│ subtotal     │       │ quantity     │       │ status       │
└──────────────┘       │ unit_price   │       │ subtotal     │
                       │ total_price  │       │ tax_amount   │
                       └──────────────┘       │ total_amount │
                                              └──────────────┘

┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  categories  │       │   products   │       │   brands     │
│──────────────│       │──────────────│       │──────────────│
│ id (PK)      │◀──────│ category_id  │       │ id (PK)      │
│ name         │       │ brand_id (FK)│──────▶│ name         │
│ slug         │       │ id (PK)      │       │ slug         │
│ parent_id    │       │ name         │       │ logo_url     │
│ image_url    │       │ slug         │       │ is_featured  │
└──────────────┘       │ sku          │       └──────────────┘
                       │ base_price   │
                       │ sale_price   │
                       │ is_active    │
                       └──────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           ▼                  ▼                  ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ product_     │       │ product_     │       │ regional_    │
│ variants     │       │ images       │       │ prices       │
│──────────────│       │──────────────│       │──────────────│
│ product_id   │       │ product_id   │       │ product_id   │
│ sku          │       │ url          │       │ region_id    │
│ attributes   │       │ alt_text     │       │ currency_id  │
│ stock_qty    │       │ is_primary   │       │ base_price   │
│ price_adj    │       └──────────────┘       │ sale_price   │
└──────────────┘                              └──────────────┘

┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  currencies  │       │   regions    │       │  tax_rates   │
│──────────────│       │──────────────│       │──────────────│
│ id (PK)      │       │ id (PK)      │       │ id (PK)      │
│ code (USD)   │◀──────│ default_curr │       │ region_id FK │
│ name         │       │ code (US)    │       │ name (GST)   │
│ symbol ($)   │       │ name         │       │ rate (0.08)  │
│ exchange_rate│       │ locale_code  │       │ is_inclusive │
│ is_default   │       │ is_active    │       └──────────────┘
└──────────────┘       └──────────────┘
                              │
                              ▼
                       ┌──────────────┐
                       │ shipping_    │
                       │ rates        │
                       │──────────────│
                       │ region_id FK │
                       │ method       │
                       │ base_cost    │
                       │ free_thresh  │
                       └──────────────┘
```

### 4.2 Table Definitions

#### `users` Table

| Column             | Type         | Constraints      | Description               |
| ------------------ | ------------ | ---------------- | ------------------------- |
| id                 | UUID         | PK               | Unique identifier         |
| email              | VARCHAR(255) | UNIQUE, NOT NULL | User's email              |
| password_hash      | VARCHAR(255) | NOT NULL         | BCrypt hashed password    |
| first_name         | VARCHAR(50)  | NOT NULL         | First name                |
| last_name          | VARCHAR(50)  | NOT NULL         | Last name                 |
| phone              | VARCHAR(20)  |                  | Phone number              |
| avatar_url         | VARCHAR(500) |                  | Profile picture URL       |
| role_id            | UUID         | FK → roles       | User's role               |
| region_code        | VARCHAR(2)   | DEFAULT 'US'     | ISO 3166-1 alpha-2        |
| preferred_currency | VARCHAR(3)   |                  | ISO 4217 currency code    |
| locale             | VARCHAR(10)  | DEFAULT 'en-US'  | User locale               |
| is_email_verified  | BOOLEAN      | DEFAULT false    | Email verification status |
| is_active          | BOOLEAN      | DEFAULT true     | Account status            |
| last_login_at      | TIMESTAMP    |                  | Last login timestamp      |
| created_at         | TIMESTAMP    | NOT NULL         | Creation timestamp        |
| updated_at         | TIMESTAMP    |                  | Last update timestamp     |

#### `products` Table

| Column            | Type          | Constraints      | Description           |
| ----------------- | ------------- | ---------------- | --------------------- |
| id                | UUID          | PK               | Unique identifier     |
| name              | VARCHAR(255)  | NOT NULL         | Product name          |
| slug              | VARCHAR(280)  | UNIQUE, NOT NULL | URL-friendly slug     |
| sku               | VARCHAR(50)   | UNIQUE, NOT NULL | Stock Keeping Unit    |
| short_description | VARCHAR(500)  |                  | Brief description     |
| description       | TEXT          |                  | Full description      |
| base_price        | DECIMAL(10,2) | NOT NULL         | Base price in USD     |
| sale_price        | DECIMAL(10,2) |                  | Discounted price      |
| cost_price        | DECIMAL(10,2) |                  | Cost for margin calc  |
| category_id       | UUID          | FK → categories  | Product category      |
| brand_id          | UUID          | FK → brands      | Product brand         |
| tags              | JSONB         |                  | Array of tags         |
| is_active         | BOOLEAN       | DEFAULT true     | Visibility status     |
| is_featured       | BOOLEAN       | DEFAULT false    | Featured product      |
| is_new            | BOOLEAN       | DEFAULT false    | New arrival flag      |
| weight            | DECIMAL(8,2)  |                  | Weight for shipping   |
| weight_unit       | VARCHAR(10)   | DEFAULT 'kg'     | kg, lb, oz            |
| avg_rating        | DECIMAL(3,2)  | DEFAULT 0        | Average review rating |
| review_count      | INTEGER       | DEFAULT 0        | Number of reviews     |
| view_count        | INTEGER       | DEFAULT 0        | Page views            |
| sold_count        | INTEGER       | DEFAULT 0        | Units sold            |

#### `product_variants` Table

| Column              | Type          | Constraints      | Description                             |
| ------------------- | ------------- | ---------------- | --------------------------------------- |
| id                  | UUID          | PK               | Unique identifier                       |
| product_id          | UUID          | FK → products    | Parent product                          |
| sku                 | VARCHAR(50)   | UNIQUE, NOT NULL | Variant SKU                             |
| name                | VARCHAR(100)  |                  | Variant display name (e.g., "S", "Red") |
| attributes          | JSONB         |                  | `{"size": "M", "color": "Blue"}`        |
| price_adjustment    | DECIMAL(10,2) | DEFAULT 0        | Price +/- from base                     |
| stock_quantity      | INTEGER       | NOT NULL         | Available stock                         |
| low_stock_threshold | INTEGER       | DEFAULT 5        | Low stock alert level                   |
| is_active           | BOOLEAN       | DEFAULT true     | Variant availability                    |
| image_url           | VARCHAR(500)  |                  | Variant-specific image                  |
| weight              | DECIMAL(8,2)  |                  | Override product weight                 |

#### `currencies` Table

| Column               | Type          | Constraints      | Description                    |
| -------------------- | ------------- | ---------------- | ------------------------------ |
| id                   | UUID          | PK               | Unique identifier              |
| code                 | VARCHAR(3)    | UNIQUE, NOT NULL | ISO 4217 code (USD, EUR, GBP)  |
| name                 | VARCHAR(100)  | NOT NULL         | Full name (US Dollar)          |
| symbol               | VARCHAR(5)    | NOT NULL         | Currency symbol ($, €, £)      |
| symbol_position      | VARCHAR(10)   | DEFAULT 'BEFORE' | BEFORE or AFTER amount         |
| decimal_places       | INTEGER       | DEFAULT 2        | Decimal precision              |
| decimal_separator    | VARCHAR(1)    | DEFAULT '.'      | . or ,                         |
| thousands_separator  | VARCHAR(1)    | DEFAULT ','      | , or .                         |
| exchange_rate_to_usd | DECIMAL(18,8) | DEFAULT 1        | Conversion rate to USD         |
| stripe_multiplier    | INTEGER       | DEFAULT 100      | For Stripe amounts (100=cents) |
| is_active            | BOOLEAN       | DEFAULT true     | Currency availability          |
| is_default           | BOOLEAN       | DEFAULT false    | Default currency               |

#### `regions` Table

| Column              | Type         | Constraints          | Description                     |
| ------------------- | ------------ | -------------------- | ------------------------------- |
| id                  | UUID         | PK                   | Unique identifier               |
| code                | VARCHAR(2)   | UNIQUE, NOT NULL     | ISO 3166-1 alpha-2 (US, SG, IN) |
| name                | VARCHAR(100) | NOT NULL             | Full name (United States)       |
| timezone            | VARCHAR(50)  |                      | Timezone (America/New_York)     |
| default_currency_id | UUID         | FK → currencies      | Default currency for region     |
| locale_code         | VARCHAR(10)  |                      | en-US, en-SG, hi-IN             |
| date_format         | VARCHAR(20)  | DEFAULT 'yyyy-MM-dd' | Date display format             |
| is_active           | BOOLEAN      | DEFAULT true         | Region availability             |
| is_default          | BOOLEAN      | DEFAULT false        | Default region                  |

#### `regional_prices` Table

| Column      | Type          | Constraints     | Description                   |
| ----------- | ------------- | --------------- | ----------------------------- |
| id          | UUID          | PK              | Unique identifier             |
| product_id  | UUID          | FK → products   | Product reference             |
| region_id   | UUID          | FK → regions    | Region reference              |
| currency_id | UUID          | FK → currencies | Price currency                |
| base_price  | DECIMAL(12,2) | NOT NULL        | Base price in region currency |
| sale_price  | DECIMAL(12,2) |                 | Sale price in region currency |
| cost_price  | DECIMAL(12,2) |                 | Cost for margin calculation   |
| is_active   | BOOLEAN       | DEFAULT true    | Price active status           |

**Unique Constraint**: `(product_id, region_id)`

#### `tax_rates` Table

| Column       | Type         | Constraints   | Description                    |
| ------------ | ------------ | ------------- | ------------------------------ |
| id           | UUID         | PK            | Unique identifier              |
| region_id    | UUID         | FK → regions  | Region reference               |
| name         | VARCHAR(100) | NOT NULL      | Tax name (GST, VAT, Sales Tax) |
| description  | VARCHAR(255) |               | Tax description                |
| rate         | DECIMAL(5,4) | NOT NULL      | Rate as decimal (0.08 = 8%)    |
| display_rate | VARCHAR(10)  |               | Display string ("8%")          |
| is_inclusive | BOOLEAN      | DEFAULT false | VAT-style inclusive pricing    |
| is_active    | BOOLEAN      | DEFAULT true  | Tax active status              |
| priority     | INTEGER      | DEFAULT 0     | Order for multiple taxes       |

#### `shipping_rates` Table

| Column                  | Type          | Constraints   | Description                  |
| ----------------------- | ------------- | ------------- | ---------------------------- |
| id                      | UUID          | PK            | Unique identifier            |
| region_id               | UUID          | FK → regions  | Region reference             |
| shipping_method         | VARCHAR(50)   | NOT NULL      | STANDARD, EXPRESS, OVERNIGHT |
| name                    | VARCHAR(100)  | NOT NULL      | Display name                 |
| description             | TEXT          |               | Method description           |
| base_cost               | DECIMAL(10,2) | NOT NULL      | Base shipping cost           |
| cost_per_kg             | DECIMAL(10,2) | DEFAULT 0     | Additional cost per kg       |
| free_shipping_threshold | DECIMAL(10,2) |               | Min order for free shipping  |
| min_delivery_days       | INTEGER       |               | Minimum delivery days        |
| max_delivery_days       | INTEGER       |               | Maximum delivery days        |
| is_active               | BOOLEAN       | DEFAULT true  | Method availability          |
| is_default              | BOOLEAN       | DEFAULT false | Default shipping method      |

**Unique Constraint**: `(region_id, shipping_method)`

---

## 5. Module Documentation

### 5.1 User Module

#### Entities

- **User**: Application users with profile, preferences, and role
- **Role**: RBAC roles with permissions
- **RefreshToken**: JWT refresh tokens

#### Key Features

- User registration and profile management
- Regional preferences (region_code, locale, preferred_currency)
- Password reset functionality
- Email verification

#### User Preferences Flow

```
User Registration/Update
        │
        ▼
┌─────────────────┐
│ Set region_code │ (e.g., "US", "SG", "IN")
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ Lookup Region           │
│ → Get default_currency  │
│ → Get tax_rates         │
│ → Get shipping_rates    │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ Apply user's            │
│ preferred_currency      │
│ (if different from      │
│  region default)        │
└─────────────────────────┘
```

---

### 5.2 Product Module

#### Entities

- **Product**: Main product information
- **ProductVariant**: Size/color variations with stock
- **ProductImage**: Product gallery images
- **Category**: Hierarchical product categories
- **Brand**: Product brands

#### Product Price Calculation

```
┌─────────────────────────────────────────────────────────────┐
│                    Price Determination                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Check Regional Price (regional_prices table)            │
│     └─ If exists → Use regional base_price/sale_price       │
│                                                              │
│  2. Fallback to Product Base Price                          │
│     └─ Convert to user's currency using exchange_rate       │
│                                                              │
│  3. Apply Variant Price Adjustment                          │
│     └─ final_price = base_price + variant.price_adjustment  │
│                                                              │
│  4. Check Sale Price                                         │
│     └─ effective_price = sale_price ?? base_price           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Category Hierarchy

```json
{
  "Men's Fashion": {
    "Shirts": [],
    "Pants": [],
    "Jackets": []
  },
  "Women's Fashion": {
    "Dresses": [],
    "Tops": [],
    "Bottoms": []
  },
  "Kids' Fashion": {
    "Boys": [],
    "Girls": []
  }
}
```

---

### 5.3 Pricing Module (⭐ Important)

The pricing module handles **multi-currency** and **regional pricing** - crucial for international e-commerce.

#### 5.3.1 How Currency Works

**Supported Currencies:**

| Code | Name             | Symbol | Exchange Rate (to USD) |
| ---- | ---------------- | ------ | ---------------------- |
| USD  | US Dollar        | $      | 1.00                   |
| EUR  | Euro             | €      | 0.92                   |
| GBP  | British Pound    | £      | 0.79                   |
| SGD  | Singapore Dollar | S$     | 1.35                   |
| INR  | Indian Rupee     | ₹      | 83.00                  |

**Currency Formatting:**

```javascript
// Example currency format configuration
{
  "code": "USD",
  "symbol": "$",
  "symbolPosition": "BEFORE",    // $100.00
  "decimalPlaces": 2,
  "decimalSeparator": ".",
  "thousandsSeparator": ","
}

// European style
{
  "code": "EUR",
  "symbol": "€",
  "symbolPosition": "BEFORE",
  "decimalPlaces": 2,
  "decimalSeparator": ",",      // 100,00 €
  "thousandsSeparator": "."
}

// Indian style
{
  "code": "INR",
  "symbol": "₹",
  "symbolPosition": "BEFORE",
  "decimalPlaces": 2,
  "decimalSeparator": ".",
  "thousandsSeparator": ","     // ₹1,00,000.00 (lakhs format - custom)
}
```

#### 5.3.2 How Regional Pricing Works

**Priority Order:**

1. **Regional Price** (if exists in `regional_prices` table)
2. **Product Base Price** (converted using exchange rate)

```
User in Singapore (region_code: "SG")
         │
         ▼
┌─────────────────────────────┐
│ Check regional_prices       │
│ WHERE product_id = X        │
│   AND region_id = 'SG'      │
└──────────┬──────────────────┘
           │
     ┌─────┴─────┐
     │           │
   Found      Not Found
     │           │
     ▼           ▼
┌─────────┐  ┌─────────────────────────┐
│ Use SGD │  │ Use product.base_price  │
│ price   │  │ × SGD exchange_rate     │
└─────────┘  └─────────────────────────┘
```

**Example Scenario:**

```
Product: "Premium T-Shirt"
- base_price (USD): $49.99

Regional Prices:
- US: $49.99 USD (same as base)
- SG: S$68.00 SGD (custom pricing)
- IN: ₹3,999 INR (custom pricing)
- UK: Not set → £39.49 GBP (converted: $49.99 × 0.79)
```

#### 5.3.3 Tax Calculation

**Tax Types:**

| Region | Tax Name  | Rate | Type                 |
| ------ | --------- | ---- | -------------------- |
| US     | Sales Tax | 8%   | Exclusive (added)    |
| SG     | GST       | 9%   | Exclusive (added)    |
| UK     | VAT       | 20%  | Inclusive (included) |
| IN     | GST       | 18%  | Exclusive (added)    |

**Calculation Logic:**

```javascript
// Exclusive Tax (US, SG, IN)
subtotal = 100.00
tax = subtotal × rate  // 100 × 0.08 = 8.00
total = subtotal + tax // 108.00

// Inclusive Tax (UK VAT)
total = 100.00 (VAT included)
tax = total - (total / (1 + rate))  // 100 - (100/1.20) = 16.67
subtotal = total - tax              // 83.33
```

#### 5.3.4 Shipping Calculation

```javascript
// Shipping cost calculation
function calculateShipping(orderTotal, weightKg, region) {
  const rate = getShippingRate(region, 'STANDARD');

  // Free shipping check
  if (rate.freeShippingThreshold && orderTotal >= rate.freeShippingThreshold) {
    return 0;
  }

  // Base cost + weight-based cost
  return rate.baseCost + (rate.costPerKg × weightKg);
}
```

**Shipping Methods:**

| Method    | Description       | Typical Delivery  |
| --------- | ----------------- | ----------------- |
| STANDARD  | Regular shipping  | 5-7 business days |
| EXPRESS   | Fast shipping     | 2-3 business days |
| OVERNIGHT | Next day delivery | 1 business day    |

#### 5.3.5 Complete Pricing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    CART PRICING CALCULATION                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  INPUT:                                                          │
│  - Cart items (product_id, variant_id, quantity)                │
│  - User's region_code                                            │
│  - User's preferred_currency (optional)                          │
│                                                                  │
│  STEP 1: Get Region & Currency                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ region = regions.findByCode(user.region_code)           │    │
│  │ currency = user.preferred_currency ?? region.default    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  STEP 2: Calculate Line Items                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ FOR each cart_item:                                      │    │
│  │   regional_price = getRegionalPrice(product, region)    │    │
│  │   IF regional_price exists:                              │    │
│  │     unit_price = regional_price.getFinalPrice()         │    │
│  │   ELSE:                                                  │    │
│  │     unit_price = product.base_price × exchange_rate     │    │
│  │                                                          │    │
│  │   IF variant:                                            │    │
│  │     unit_price += variant.price_adjustment              │    │
│  │                                                          │    │
│  │   line_total = unit_price × quantity                    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  STEP 3: Calculate Subtotal                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ subtotal = SUM(all line_totals)                         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  STEP 4: Calculate Tax                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ tax_rates = getTaxRates(region)                         │    │
│  │ total_tax = 0                                            │    │
│  │ FOR each tax_rate:                                       │    │
│  │   IF tax_rate.is_inclusive:                              │    │
│  │     tax = subtotal - (subtotal / (1 + rate))            │    │
│  │   ELSE:                                                  │    │
│  │     tax = subtotal × rate                               │    │
│  │   total_tax += tax                                       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  STEP 5: Calculate Shipping                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ shipping_rate = getDefaultShippingRate(region)          │    │
│  │ total_weight = SUM(item.weight × quantity)              │    │
│  │ shipping = shipping_rate.calculate(subtotal, weight)    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  STEP 6: Calculate Grand Total                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ grand_total = subtotal + total_tax + shipping           │    │
│  │               - discount (if any)                        │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  OUTPUT: PricingCalculationDTO                                   │
│  {                                                               │
│    regionCode, currencyCode, currencySymbol,                    │
│    items: [...],                                                 │
│    subtotal, formattedSubtotal,                                 │
│    taxes: [...], totalTax, formattedTotalTax,                   │
│    shippingCost, formattedShippingCost,                         │
│    grandTotal, formattedGrandTotal                              │
│  }                                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

### 5.4 Cart Module

#### Entities

- **Cart**: User's shopping cart (one per user)
- **CartItem**: Items in the cart

#### Cart Item Structure

```json
{
  "id": "cart-item-uuid",
  "productId": "product-uuid",
  "productName": "Premium T-Shirt",
  "productImage": "https://...",
  "variantId": "variant-uuid",
  "variantName": "M",
  "variantAttributes": { "size": "M", "color": "Blue" },
  "quantity": 2,
  "unitPrice": 49.99,
  "totalPrice": 99.98,
  "formattedUnitPrice": "$49.99",
  "formattedTotalPrice": "$99.98",
  "inStock": true,
  "availableStock": 50
}
```

#### Cart Response with Pricing

```json
{
  "id": "cart-uuid",
  "userId": "user-uuid",
  "items": [...],
  "totalItems": 3,

  // Regional info
  "regionCode": "US",
  "regionName": "United States",
  "currencyCode": "USD",
  "currencySymbol": "$",

  // Pricing
  "subtotal": 149.97,
  "formattedSubtotal": "$149.97",
  "tax": 12.00,
  "formattedTax": "$12.00",
  "shipping": 0,
  "formattedShipping": "FREE",
  "qualifiesForFreeShipping": true,
  "total": 161.97,
  "formattedTotal": "$161.97"
}
```

---

### 5.5 Order Module

#### Entities

- **Order**: Customer order
- **OrderItem**: Line items in order
- **OrderStatusHistory**: Status change history

#### Order Status Flow

```
┌─────────┐   ┌───────────┐   ┌─────────┐   ┌───────────┐   ┌───────────┐
│ PENDING │──▶│PROCESSING │──▶│ SHIPPED │──▶│ DELIVERED │   │ CANCELLED │
└─────────┘   └───────────┘   └─────────┘   └───────────┘   └───────────┘
     │              │              │                              ▲
     │              │              │                              │
     └──────────────┴──────────────┴──────────────────────────────┘
                         (Can be cancelled before delivery)
```

#### Payment Status

| Status             | Description           |
| ------------------ | --------------------- |
| PENDING            | Payment not initiated |
| PROCESSING         | Payment in progress   |
| PAID               | Payment successful    |
| FAILED             | Payment failed        |
| REFUNDED           | Full refund issued    |
| PARTIALLY_REFUNDED | Partial refund issued |

---

### 5.6 Review Module

#### Entities

- **Review**: Product review with rating
- **ReviewImage**: Images attached to review
- **ReviewVote**: Helpful/not helpful votes

#### Review Features

- 1-5 star rating
- Title and content
- Pros and cons lists
- Fit feedback (RUNS_SMALL, TRUE_TO_SIZE, RUNS_LARGE)
- Verified purchase badge
- Helpful votes
- Admin response capability
- Moderation status (PENDING, APPROVED, REJECTED)

---

### 5.7 Notification Module

#### Entities

- **Notification**: User notifications
- **NotificationPreference**: User notification settings

#### Notification Types

| Type             | Description              |
| ---------------- | ------------------------ |
| ORDER_PLACED     | Order confirmation       |
| ORDER_SHIPPED    | Shipping notification    |
| ORDER_DELIVERED  | Delivery confirmation    |
| ORDER_CANCELLED  | Cancellation notice      |
| PAYMENT_RECEIVED | Payment confirmation     |
| PAYMENT_FAILED   | Payment failure alert    |
| REVIEW_APPROVED  | Review published         |
| PROMOTION        | Promotional notification |
| SYSTEM           | System announcements     |

---

### 5.8 Payment Module

#### Stripe Integration

**Payment Flow:**

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│ Create Order   │────▶│ Create Payment │────▶│ Return Client  │
│                │     │ Intent (Stripe)│     │ Secret         │
└────────────────┘     └────────────────┘     └────────────────┘
                                                      │
                                                      ▼
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│ Update Order   │◀────│ Webhook Event  │◀────│ Frontend       │
│ Status         │     │ (payment_      │     │ Confirms       │
│                │     │  succeeded)    │     │ Payment        │
└────────────────┘     └────────────────┘     └────────────────┘
```

**Stripe Amount Conversion:**

```javascript
// Most currencies use cents (multiply by 100)
stripeAmount = amount × 100

// JPY uses no decimals (multiply by 1)
stripeAmount = amount × 1

// Currency entity has stripe_multiplier field
stripeAmount = amount × currency.stripeMultiplier
```

---

### 5.9 Admin Module

#### Dashboard Metrics

```json
{
  "totalOrders": 150,
  "pendingOrders": 10,
  "processingOrders": 25,
  "shippedOrders": 45,
  "deliveredOrders": 65,
  "cancelledOrders": 5,

  "totalRevenue": 25000.0,
  "todayRevenue": 500.0,
  "weekRevenue": 3500.0,
  "monthRevenue": 12000.0,

  "totalUsers": 500,
  "activeUsers": 450,
  "newUsersToday": 5,
  "newUsersWeek": 25,
  "newUsersMonth": 100,

  "totalProducts": 200,
  "activeProducts": 180,
  "outOfStockProducts": 10,
  "lowStockProducts": 15,

  "totalReviews": 300,
  "pendingReviews": 20,
  "averageRating": 4.2,

  "totalCategories": 15,
  "totalBrands": 10
}
```

---

## 6. API Endpoints Reference

### 6.1 Authentication (`/auth`)

| Method | Endpoint                | Auth | Description                       |
| ------ | ----------------------- | ---- | --------------------------------- |
| POST   | `/auth/register`        | No   | Register new user                 |
| POST   | `/auth/login`           | No   | Login and get tokens              |
| POST   | `/auth/refresh`         | No   | Refresh access token              |
| POST   | `/auth/logout`          | Yes  | Logout (invalidate refresh token) |
| POST   | `/auth/forgot-password` | No   | Request password reset            |
| POST   | `/auth/reset-password`  | No   | Reset password with token         |

### 6.2 Users (`/users`)

| Method | Endpoint                | Auth | Role | Description                           |
| ------ | ----------------------- | ---- | ---- | ------------------------------------- |
| GET    | `/users/me`             | Yes  | Any  | Get current user profile              |
| PUT    | `/users/me`             | Yes  | Any  | Update current user profile           |
| PUT    | `/users/me/password`    | Yes  | Any  | Change password                       |
| PUT    | `/users/me/preferences` | Yes  | Any  | Update preferences (region, currency) |

### 6.3 Products (`/products`)

| Method | Endpoint                | Auth        | Description               |
| ------ | ----------------------- | ----------- | ------------------------- |
| GET    | `/products`             | No          | List products (paginated) |
| GET    | `/products/{id}`        | No          | Get product by ID         |
| GET    | `/products/slug/{slug}` | No          | Get product by slug       |
| GET    | `/products/search`      | No          | Search products           |
| GET    | `/products/featured`    | No          | Get featured products     |
| GET    | `/products/new`         | No          | Get new arrivals          |
| POST   | `/products`             | Yes (Admin) | Create product            |
| PUT    | `/products/{id}`        | Yes (Admin) | Update product            |
| DELETE | `/products/{id}`        | Yes (Admin) | Delete product            |

### 6.4 Categories (`/categories`)

| Method | Endpoint                  | Auth        | Description          |
| ------ | ------------------------- | ----------- | -------------------- |
| GET    | `/categories`             | No          | List all categories  |
| GET    | `/categories/{id}`        | No          | Get category by ID   |
| GET    | `/categories/slug/{slug}` | No          | Get category by slug |
| GET    | `/categories/tree`        | No          | Get category tree    |
| POST   | `/categories`             | Yes (Admin) | Create category      |
| PUT    | `/categories/{id}`        | Yes (Admin) | Update category      |
| DELETE | `/categories/{id}`        | Yes (Admin) | Delete category      |

### 6.5 Brands (`/brands`)

| Method | Endpoint              | Auth        | Description            |
| ------ | --------------------- | ----------- | ---------------------- |
| GET    | `/brands`             | No          | List all active brands |
| GET    | `/brands/{id}`        | No          | Get brand by ID        |
| GET    | `/brands/slug/{slug}` | No          | Get brand by slug      |
| GET    | `/brands/featured`    | No          | Get featured brands    |
| POST   | `/brands`             | Yes (Admin) | Create brand           |
| PUT    | `/brands/{id}`        | Yes (Admin) | Update brand           |
| DELETE | `/brands/{id}`        | Yes (Admin) | Delete brand           |

### 6.6 Pricing (`/pricing`)

| Method | Endpoint                               | Auth | Description              |
| ------ | -------------------------------------- | ---- | ------------------------ |
| GET    | `/pricing/currencies`                  | No   | List active currencies   |
| GET    | `/pricing/regions`                     | No   | List active regions      |
| GET    | `/pricing/regions/{code}`              | No   | Get region details       |
| GET    | `/pricing/tax-rates/{regionCode}`      | No   | Get tax rates for region |
| GET    | `/pricing/shipping-rates/{regionCode}` | No   | Get shipping rates       |
| POST   | `/pricing/calculate`                   | No   | Calculate cart pricing   |

### 6.7 Cart (`/cart`)

| Method | Endpoint               | Auth | Description               |
| ------ | ---------------------- | ---- | ------------------------- |
| GET    | `/cart`                | Yes  | Get current user's cart   |
| POST   | `/cart/items`          | Yes  | Add item to cart          |
| PUT    | `/cart/items/{itemId}` | Yes  | Update cart item quantity |
| DELETE | `/cart/items/{itemId}` | Yes  | Remove item from cart     |
| DELETE | `/cart`                | Yes  | Clear entire cart         |
| GET    | `/cart/count`          | Yes  | Get cart item count       |

### 6.8 Orders (`/orders`)

| Method | Endpoint                       | Auth        | Description               |
| ------ | ------------------------------ | ----------- | ------------------------- |
| POST   | `/orders`                      | Yes         | Create order from cart    |
| GET    | `/orders/my`                   | Yes         | Get current user's orders |
| GET    | `/orders/{id}`                 | Yes         | Get order details         |
| GET    | `/orders/number/{orderNumber}` | Yes         | Get order by number       |
| PUT    | `/orders/{id}/cancel`          | Yes         | Cancel order              |
| GET    | `/orders`                      | Yes (Admin) | List all orders           |
| PUT    | `/orders/{id}/status`          | Yes (Admin) | Update order status       |

### 6.9 Reviews (`/products/{productId}/reviews`)

| Method | Endpoint                              | Auth | Description              |
| ------ | ------------------------------------- | ---- | ------------------------ |
| GET    | `/products/{productId}/reviews`       | No   | Get product reviews      |
| GET    | `/products/{productId}/reviews/stats` | No   | Get review statistics    |
| POST   | `/products/{productId}/reviews`       | Yes  | Create review            |
| PUT    | `/reviews/{id}`                       | Yes  | Update own review        |
| DELETE | `/reviews/{id}`                       | Yes  | Delete own review        |
| POST   | `/reviews/{id}/vote`                  | Yes  | Vote helpful/not helpful |

### 6.10 Notifications (`/notifications`)

| Method | Endpoint                     | Auth | Description              |
| ------ | ---------------------------- | ---- | ------------------------ |
| GET    | `/notifications`             | Yes  | Get user notifications   |
| GET    | `/notifications/unread`      | Yes  | Get unread notifications |
| GET    | `/notifications/count`       | Yes  | Get unread count         |
| PUT    | `/notifications/{id}/read`   | Yes  | Mark as read             |
| PUT    | `/notifications/read-all`    | Yes  | Mark all as read         |
| DELETE | `/notifications/{id}`        | Yes  | Delete notification      |
| GET    | `/notifications/preferences` | Yes  | Get preferences          |
| PUT    | `/notifications/preferences` | Yes  | Update preferences       |

### 6.11 Payments (`/payments`)

| Method | Endpoint                              | Auth | Description                  |
| ------ | ------------------------------------- | ---- | ---------------------------- |
| POST   | `/payments/create-intent`             | Yes  | Create Stripe payment intent |
| POST   | `/payments/confirm/{paymentIntentId}` | Yes  | Confirm payment              |
| POST   | `/webhooks/stripe`                    | No   | Stripe webhook endpoint      |

### 6.12 Admin (`/admin`)

| Method | Endpoint                         | Auth        | Description              |
| ------ | -------------------------------- | ----------- | ------------------------ |
| GET    | `/admin/dashboard/overview`      | Yes (Admin) | Get dashboard metrics    |
| GET    | `/admin/dashboard/sales-report`  | Yes (Admin) | Get sales report         |
| GET    | `/admin/dashboard/top-products`  | Yes (Admin) | Get top selling products |
| GET    | `/admin/dashboard/recent-orders` | Yes (Admin) | Get recent orders        |

---

## 7. Request/Response Examples

### 7.1 User Registration

**Request:**

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "CUSTOMER"
    }
  },
  "timestamp": "2025-12-06T08:00:00.000Z"
}
```

### 7.2 Login

**Request:**

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

### 7.3 Add to Cart

**Request:**

```http
POST /api/v1/cart/items
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "productId": "123e4567-e89b-12d3-a456-426614174000",
  "variantId": "223e4567-e89b-12d3-a456-426614174001",
  "quantity": 2
}
```

**Response:**

```json
{
  "success": true,
  "message": "Item added to cart",
  "data": {
    "id": "cart-uuid",
    "userId": "user-uuid",
    "items": [
      {
        "id": "item-uuid",
        "productId": "123e4567-e89b-12d3-a456-426614174000",
        "productName": "Premium Cotton T-Shirt",
        "productImage": "https://images.unsplash.com/...",
        "variantId": "223e4567-e89b-12d3-a456-426614174001",
        "variantName": "M",
        "quantity": 2,
        "unitPrice": 49.99,
        "totalPrice": 99.98
      }
    ],
    "totalItems": 2,
    "regionCode": "US",
    "currencyCode": "USD",
    "currencySymbol": "$",
    "subtotal": 99.98,
    "formattedSubtotal": "$99.98",
    "tax": 8.0,
    "formattedTax": "$8.00",
    "shipping": 5.99,
    "formattedShipping": "$5.99",
    "total": 113.97,
    "formattedTotal": "$113.97"
  },
  "timestamp": "2025-12-06T08:00:00.000Z"
}
```

### 7.4 Create Order

**Request:**

```http
POST /api/v1/orders
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "shippingAddress": {
    "name": "John Doe",
    "phone": "+1234567890",
    "email": "john@example.com",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "United States"
  },
  "customerNotes": "Please leave at door",
  "couponCode": "SAVE10"
}
```

---

## 8. Error Handling

### Error Response Format

```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "path": "/api/v1/endpoint",
  "timestamp": "2025-12-06T08:00:00.000Z",
  "errors": [
    {
      "field": "email",
      "message": "Please provide a valid email address"
    }
  ]
}
```

### HTTP Status Codes

| Code | Meaning               | When Used                |
| ---- | --------------------- | ------------------------ |
| 200  | OK                    | Successful request       |
| 201  | Created               | Resource created         |
| 400  | Bad Request           | Validation errors        |
| 401  | Unauthorized          | Missing/invalid token    |
| 403  | Forbidden             | Insufficient permissions |
| 404  | Not Found             | Resource not found       |
| 409  | Conflict              | Duplicate resource       |
| 422  | Unprocessable Entity  | Business logic error     |
| 500  | Internal Server Error | Server error             |

### Common Error Messages

| Error                 | Cause                           | Solution                                    |
| --------------------- | ------------------------------- | ------------------------------------------- |
| "JWT token expired"   | Access token expired            | Use refresh token to get new access token   |
| "User not found"      | Invalid user ID                 | Check user exists                           |
| "Product not found"   | Invalid product ID              | Check product exists and is active          |
| "Insufficient stock"  | Not enough inventory            | Reduce quantity or select different variant |
| "Cart is empty"       | No items in cart                | Add items before checkout                   |
| "Invalid coupon code" | Coupon doesn't exist or expired | Check coupon validity                       |

---

## 9. Frontend Integration Guide

### 9.1 Initial Setup

1. **Store tokens securely:**

   - Access token: Memory/sessionStorage (short-lived)
   - Refresh token: httpOnly cookie or secure storage

2. **Set up axios interceptors:**

```javascript
// Add auth header to all requests
axios.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle token refresh
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const newToken = await refreshAccessToken();
      if (newToken) {
        error.config.headers.Authorization = `Bearer ${newToken}`;
        return axios(error.config);
      }
    }
    return Promise.reject(error);
  }
);
```

### 9.2 User Region & Currency

**Get user's region on login/register:**

```javascript
// After successful auth
const user = response.data.user;
const regionCode = user.regionCode || "US";
const preferredCurrency = user.preferredCurrency;

// Fetch region details
const region = await api.get(`/pricing/regions/${regionCode}`);
const currency = preferredCurrency || region.defaultCurrency.code;

// Store for use throughout app
store.setRegion(region);
store.setCurrency(currency);
```

**Allow user to change currency:**

```javascript
// Fetch available currencies
const currencies = await api.get("/pricing/currencies");

// Update user preference
await api.put("/users/me/preferences", {
  preferredCurrency: "EUR",
});
```

### 9.3 Displaying Prices

**Use formatted prices from API:**

```javascript
// Product listing
products.map((product) => (
  <ProductCard
    name={product.name}
    price={product.formattedEffectivePrice} // Already formatted: "$49.99"
    originalPrice={product.onSale ? product.formattedBasePrice : null}
    discount={product.discountPercentage}
  />
));
```

**Manual formatting (if needed):**

```javascript
function formatPrice(amount, currency) {
  const {
    symbol,
    symbolPosition,
    decimalPlaces,
    decimalSeparator,
    thousandsSeparator,
  } = currency;

  const formatted = amount
    .toFixed(decimalPlaces)
    .replace(".", decimalSeparator)
    .replace(/\B(?=(\d{3})+(?!\d))/g, thousandsSeparator);

  return symbolPosition === "BEFORE"
    ? `${symbol}${formatted}`
    : `${formatted} ${symbol}`;
}
```

### 9.4 Cart Management

```javascript
// Add to cart
async function addToCart(productId, variantId, quantity) {
  const response = await api.post("/cart/items", {
    productId,
    variantId,
    quantity,
  });

  // Response includes full cart with pricing
  updateCartState(response.data);
}

// Update quantity
async function updateQuantity(itemId, quantity) {
  await api.put(`/cart/items/${itemId}`, { quantity });
}

// Remove item
async function removeItem(itemId) {
  await api.delete(`/cart/items/${itemId}`);
}
```

### 9.5 Checkout Flow

```javascript
// 1. Create order
const order = await api.post('/orders', {
  shippingAddress: { ... },
  customerNotes: "..."
});

// 2. Create payment intent
const payment = await api.post('/payments/create-intent', {
  orderId: order.id
});

// 3. Confirm with Stripe (frontend)
const { error } = await stripe.confirmCardPayment(
  payment.clientSecret,
  {
    payment_method: {
      card: cardElement,
      billing_details: { ... }
    }
  }
);

// 4. Handle result
if (error) {
  showError(error.message);
} else {
  // Webhook will update order status
  redirectToOrderConfirmation(order.orderNumber);
}
```

### 9.6 Real-time Updates

**Polling for notifications:**

```javascript
// Poll every 30 seconds
setInterval(async () => {
  const count = await api.get("/notifications/count");
  updateNotificationBadge(count.data);
}, 30000);
```

**Order status tracking:**

```javascript
async function pollOrderStatus(orderId) {
  const order = await api.get(`/orders/${orderId}`);
  updateOrderStatus(order.status);

  if (!["DELIVERED", "CANCELLED"].includes(order.status)) {
    setTimeout(() => pollOrderStatus(orderId), 60000);
  }
}
```

---

## Appendix A: Environment Variables

```properties
# Database
DATABASE_URL=jdbc:postgresql://host:5432/foalrider
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=secret

# JWT
JWT_SECRET=your-512-bit-secret-key
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Stripe
STRIPE_API_KEY=sk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx

# Redis (optional)
REDIS_HOST=localhost
REDIS_PORT=6379

# Mail (optional)
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your-email
MAIL_PASSWORD=app-password
```

---

## Appendix B: Test Accounts

| Role     | Email                  | Password |
| -------- | ---------------------- | -------- |
| Admin    | admin@foalrider.com    | Test@123 |
| Customer | customer@foalrider.com | Test@123 |
| Vendor   | vendor@foalrider.com   | Test@123 |

---

## Appendix C: Seeded Data

### Currencies

- USD (US Dollar) - Default
- EUR (Euro)
- GBP (British Pound)
- SGD (Singapore Dollar)
- INR (Indian Rupee)

### Regions

- US (United States) - Default
- SG (Singapore)
- IN (India)
- GB (United Kingdom)

### Categories

- Men's Fashion (Shirts, Pants, Jackets)
- Women's Fashion (Dresses, Tops, Bottoms)
- Kids' Fashion (Boys, Girls)

### Brands

- FoalRider
- UrbanStyle
- ClassicWear

---

**Document End**

_For questions or issues, contact the backend team._
