# FoalRider API Documentation

Complete API reference for the FoalRider E-Commerce Backend.

**Base URL**: `http://localhost:8080`  
**Content-Type**: `application/json`  
**Authentication**: Bearer JWT Token

---

## Table of Contents

1. [Authentication](#authentication)
2. [Products](#products)
3. [Categories](#categories)
4. [Users](#users)
5. [Cart](#cart)
6. [Orders](#orders)
7. [Reviews](#reviews)
8. [Notifications](#notifications)
9. [Admin](#admin)
10. [Pricing](#pricing)

---

## Authentication

All protected endpoints require a JWT token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

### POST /auth/register

Register a new user account.

**Request Body**:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "phoneNumber": "1234567890"
}
```

**Response** (201 Created):

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "id": "uuid",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

**Validation Rules**:

- Email: Valid email format, unique
- Password: Min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
- Phone: Valid phone number format

---

### POST /auth/login

Authenticate user and get JWT tokens.

**Request Body**:

```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

**Error Response** (401 Unauthorized):

```json
{
  "success": false,
  "message": "Invalid email or password"
}
```

---

### POST /auth/refresh

Refresh expired access token.

**Request Body**:

```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }
}
```

---

### POST /auth/logout

Logout and invalidate tokens.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "success": true,
  "message": "Logout successful"
}
```

---

## Products

### GET /products

Get paginated list of all products.

**Query Parameters**:

| Parameter  | Type    | Default        | Description                |
| ---------- | ------- | -------------- | -------------------------- |
| page       | int     | 0              | Page number (0-indexed)    |
| size       | int     | 10             | Items per page             |
| sort       | string  | createdAt,desc | Sort field and direction   |
| categoryId | uuid    | -              | Filter by category         |
| minPrice   | decimal | -              | Minimum price filter       |
| maxPrice   | decimal | -              | Maximum price filter       |
| search     | string  | -              | Search in name/description |

**Response** (200 OK):

```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Classic Cotton T-Shirt",
        "slug": "classic-cotton-t-shirt",
        "description": "Premium cotton t-shirt",
        "basePrice": 29.99,
        "category": {
          "id": "uuid",
          "name": "T-Shirts"
        },
        "brand": {
          "id": "uuid",
          "name": "FoalRider"
        },
        "images": [
          {
            "id": "uuid",
            "url": "https://images.unsplash.com/...",
            "isPrimary": true
          }
        ],
        "variants": [
          {
            "id": "uuid",
            "size": "M",
            "color": "Blue",
            "sku": "FR-TS-001-M",
            "stockQuantity": 50,
            "price": 29.99
          }
        ],
        "isActive": true,
        "isFeatured": false,
        "averageRating": 4.5,
        "reviewCount": 12
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 100,
    "totalPages": 10
  }
}
```

---

### GET /products/{id}

Get single product by ID.

**Path Parameters**:

- `id` (uuid): Product ID

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Classic Cotton T-Shirt",
    "slug": "classic-cotton-t-shirt",
    "description": "Premium quality cotton t-shirt...",
    "basePrice": 29.99,
    "category": { ... },
    "brand": { ... },
    "images": [ ... ],
    "variants": [ ... ]
  }
}
```

**Error Response** (404 Not Found):

```json
{
  "success": false,
  "message": "Product not found"
}
```

---

### POST /products

Create new product (Admin/Vendor only).

**Headers**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "name": "New Product",
  "description": "Product description",
  "basePrice": 49.99,
  "categoryId": "category-uuid",
  "brandId": "brand-uuid",
  "images": [
    {
      "url": "https://example.com/image.jpg",
      "isPrimary": true,
      "displayOrder": 0
    }
  ],
  "variants": [
    {
      "size": "M",
      "color": "Black",
      "sku": "PROD-M-BLK",
      "stockQuantity": 100,
      "price": 49.99
    }
  ]
}
```

**Response** (201 Created):

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": { ... }
}
```

---

### PUT /products/{id}

Update existing product (Admin/Vendor only).

**Headers**: `Authorization: Bearer <token>`

**Request Body**: Same as POST

---

### DELETE /products/{id}

Delete product (Admin only).

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

---

## Categories

### GET /categories

Get all categories (hierarchical).

**Query Parameters**:

| Parameter | Type    | Default | Description               |
| --------- | ------- | ------- | ------------------------- |
| parentId  | uuid    | -       | Filter by parent category |
| isActive  | boolean | true    | Filter active/inactive    |

**Response** (200 OK):

```json
{
  "success": true,
  "message": "Categories retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "name": "Men's Fashion",
      "slug": "mens-fashion",
      "description": "Men's clothing collection",
      "imageUrl": "https://...",
      "parentId": null,
      "parentName": null,
      "displayOrder": 0,
      "isActive": true,
      "isFeatured": true,
      "productCount": 25
    },
    {
      "id": "uuid",
      "name": "T-Shirts",
      "slug": "mens-t-shirts",
      "parentId": "parent-uuid",
      "parentName": "Men's Fashion",
      "productCount": 10
    }
  ]
}
```

---

### GET /categories/{id}

Get category by ID with subcategories.

---

### POST /categories

Create new category (Admin only).

**Request Body**:

```json
{
  "name": "New Category",
  "description": "Category description",
  "imageUrl": "https://...",
  "parentId": "parent-uuid-or-null",
  "displayOrder": 0,
  "isActive": true,
  "isFeatured": false
}
```

---

## Users

### GET /users/me

Get current authenticated user profile.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "1234567890",
    "role": "CUSTOMER",
    "isActive": true,
    "createdAt": "2025-12-06T10:00:00Z"
  }
}
```

---

### PUT /users/me

Update current user profile.

**Headers**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "9876543210"
}
```

---

### PUT /users/me/password

Change password.

**Headers**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass456!"
}
```

---

## Cart

### GET /cart

Get user's shopping cart.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "items": [
      {
        "id": "uuid",
        "product": {
          "id": "uuid",
          "name": "Classic Cotton T-Shirt",
          "basePrice": 29.99
        },
        "variant": {
          "id": "uuid",
          "size": "M",
          "color": "Blue"
        },
        "quantity": 2,
        "unitPrice": 29.99,
        "totalPrice": 59.98
      }
    ],
    "subtotal": 59.98,
    "itemCount": 2
  }
}
```

---

### POST /cart/items

Add item to cart.

**Headers**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "productId": "product-uuid",
  "variantId": "variant-uuid",
  "quantity": 1
}
```

---

### PUT /cart/items/{itemId}

Update cart item quantity.

**Request Body**:

```json
{
  "quantity": 3
}
```

---

### DELETE /cart/items/{itemId}

Remove item from cart.

---

### DELETE /cart

Clear entire cart.

---

## Orders

### GET /orders

Get user's orders (paginated).

**Headers**: `Authorization: Bearer <token>`

**Query Parameters**:

| Parameter | Type   | Default | Description      |
| --------- | ------ | ------- | ---------------- |
| page      | int    | 0       | Page number      |
| size      | int    | 10      | Items per page   |
| status    | string | -       | Filter by status |

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "orderNumber": "ORD-2025-001",
        "status": "DELIVERED",
        "items": [ ... ],
        "shippingAddress": { ... },
        "subtotal": 99.99,
        "shippingCost": 5.99,
        "tax": 10.00,
        "total": 115.98,
        "createdAt": "2025-12-01T10:00:00Z"
      }
    ],
    "totalElements": 5
  }
}
```

**Order Statuses**: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`

---

### GET /orders/{id}

Get order details.

---

### POST /orders

Create new order from cart.

**Request Body**:

```json
{
  "shippingAddressId": "address-uuid",
  "billingAddressId": "address-uuid",
  "paymentMethod": "CARD",
  "notes": "Leave at door"
}
```

---

### PUT /orders/{id}/cancel

Cancel an order (if status allows).

---

## Reviews

### GET /reviews

Get product reviews.

**Query Parameters**:

| Parameter | Type | Required | Description    |
| --------- | ---- | -------- | -------------- |
| productId | uuid | Yes      | Product ID     |
| page      | int  | No       | Page number    |
| size      | int  | No       | Items per page |

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "rating": 5,
        "title": "Great product!",
        "comment": "Very comfortable and good quality.",
        "user": {
          "firstName": "John",
          "lastName": "D."
        },
        "createdAt": "2025-12-01T10:00:00Z",
        "isVerifiedPurchase": true
      }
    ],
    "averageRating": 4.5,
    "totalReviews": 12
  }
}
```

---

### POST /reviews

Create a review (must have purchased product).

**Headers**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "productId": "product-uuid",
  "rating": 5,
  "title": "Excellent quality!",
  "comment": "Very happy with this purchase."
}
```

---

## Notifications

### GET /notifications

Get user notifications.

**Headers**: `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "type": "ORDER_SHIPPED",
      "title": "Order Shipped",
      "message": "Your order ORD-2025-001 has been shipped",
      "isRead": false,
      "createdAt": "2025-12-06T10:00:00Z"
    }
  ]
}
```

---

### PUT /notifications/{id}/read

Mark notification as read.

---

### PUT /notifications/read-all

Mark all notifications as read.

---

## Admin

### GET /admin/dashboard

Get admin dashboard statistics.

**Headers**: `Authorization: Bearer <admin_token>`  
**Required Role**: ADMIN

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "totalUsers": 1500,
    "totalProducts": 250,
    "totalOrders": 5000,
    "totalRevenue": 250000.00,
    "recentOrders": [ ... ],
    "topProducts": [ ... ],
    "salesByCategory": [ ... ]
  }
}
```

---

### GET /admin/users

Get all users (Admin only).

**Query Parameters**: page, size, role, search

---

### PUT /admin/users/{id}/status

Enable/disable user account.

**Request Body**:

```json
{
  "isActive": false
}
```

---

## Pricing

### GET /pricing/currencies

Get supported currencies.

**Response** (200 OK):

```json
{
  "success": true,
  "data": [
    {
      "code": "USD",
      "name": "US Dollar",
      "symbol": "$",
      "exchangeRate": 1.0
    },
    {
      "code": "EUR",
      "name": "Euro",
      "symbol": "€",
      "exchangeRate": 0.85
    }
  ]
}
```

---

### GET /pricing/convert

Convert price between currencies.

**Query Parameters**:

| Parameter | Type    | Required | Description          |
| --------- | ------- | -------- | -------------------- |
| amount    | decimal | Yes      | Amount to convert    |
| from      | string  | Yes      | Source currency code |
| to        | string  | Yes      | Target currency code |

---

## Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/products",
  "timestamp": "2025-12-06T10:00:00Z",
  "errors": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

**Common HTTP Status Codes**:

- 200: Success
- 201: Created
- 400: Bad Request (validation error)
- 401: Unauthorized (missing/invalid token)
- 403: Forbidden (insufficient permissions)
- 404: Not Found
- 500: Internal Server Error

---

## Rate Limiting

- 100 requests per minute for authenticated users
- 20 requests per minute for unauthenticated users

---

## Webhooks (Payment)

### POST /webhooks/stripe

Stripe payment webhook endpoint.

### POST /webhooks/paypal

PayPal payment webhook endpoint.
