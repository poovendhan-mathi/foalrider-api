# FoalRider E-Commerce Platform - User Guide

A comprehensive guide on how to use all features of the FoalRider E-Commerce Backend.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [User Management](#user-management)
3. [Product Management](#product-management)
4. [Category Management](#category-management)
5. [Shopping Cart](#shopping-cart)
6. [Order Management](#order-management)
7. [Reviews & Ratings](#reviews--ratings)
8. [Admin Features](#admin-features)

---

## Getting Started

### Base URL

All API requests should be made to:

```
http://localhost:8080
```

### Test Accounts

Use these pre-created accounts for testing:

| Role     | Email                  | Password | Capabilities                |
| -------- | ---------------------- | -------- | --------------------------- |
| Admin    | admin@foalrider.com    | Test@123 | Full access to all features |
| Customer | customer@foalrider.com | Test@123 | Shop, cart, orders, reviews |
| Vendor   | vendor@foalrider.com   | Test@123 | Manage own products         |

---

## User Management

### How to Register a New Customer

Send a POST request to create a new account:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com",
    "password": "MyPassword123!",
    "phoneNumber": "5551234567"
  }'
```

**Password Requirements:**

- Minimum 8 characters
- At least 1 uppercase letter (A-Z)
- At least 1 lowercase letter (a-z)
- At least 1 number (0-9)
- At least 1 special character (!@#$%^&\*)

### How to Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@foalrider.com",
    "password": "Test@123"
  }'
```

**Save the access token from the response** - you'll need it for authenticated requests:

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "..."
  }
}
```

### How to View Your Profile

```bash
curl http://localhost:8080/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### How to Update Your Profile

```bash
curl -X PUT http://localhost:8080/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "phoneNumber": "5559876543"
  }'
```

### How to Change Password

```bash
curl -X PUT http://localhost:8080/users/me/password \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "OldPassword123!",
    "newPassword": "NewPassword456!"
  }'
```

---

## Product Management

### How to Browse Products

**Get all products:**

```bash
curl http://localhost:8080/products
```

**Search products by name:**

```bash
curl "http://localhost:8080/products?search=cotton"
```

**Filter by category:**

```bash
curl "http://localhost:8080/products?categoryId=CATEGORY_UUID"
```

**Filter by price range:**

```bash
curl "http://localhost:8080/products?minPrice=20&maxPrice=50"
```

**Pagination:**

```bash
curl "http://localhost:8080/products?page=0&size=20"
```

**Sort products:**

```bash
# Sort by price (low to high)
curl "http://localhost:8080/products?sort=basePrice,asc"

# Sort by price (high to low)
curl "http://localhost:8080/products?sort=basePrice,desc"

# Sort by newest
curl "http://localhost:8080/products?sort=createdAt,desc"
```

### How to View a Single Product

```bash
curl http://localhost:8080/products/PRODUCT_UUID
```

### How to Create a Product (Admin/Vendor)

```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer ADMIN_OR_VENDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Premium Leather Jacket",
    "description": "High-quality leather jacket for men",
    "basePrice": 199.99,
    "categoryId": "mens-category-uuid",
    "brandId": "brand-uuid",
    "images": [
      {
        "url": "https://images.unsplash.com/photo-jacket",
        "isPrimary": true,
        "displayOrder": 0
      }
    ],
    "variants": [
      {
        "size": "S",
        "color": "Black",
        "sku": "LJ-BLK-S",
        "stockQuantity": 25,
        "price": 199.99
      },
      {
        "size": "M",
        "color": "Black",
        "sku": "LJ-BLK-M",
        "stockQuantity": 50,
        "price": 199.99
      },
      {
        "size": "L",
        "color": "Black",
        "sku": "LJ-BLK-L",
        "stockQuantity": 30,
        "price": 199.99
      }
    ],
    "isActive": true,
    "isFeatured": true
  }'
```

### How to Update a Product

```bash
curl -X PUT http://localhost:8080/products/PRODUCT_UUID \
  -H "Authorization: Bearer ADMIN_OR_VENDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Premium Leather Jacket - Updated",
    "basePrice": 179.99,
    "isFeatured": true
  }'
```

### How to Delete a Product (Admin Only)

```bash
curl -X DELETE http://localhost:8080/products/PRODUCT_UUID \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

---

## Category Management

### How to View All Categories

```bash
curl http://localhost:8080/categories
```

**Response includes:**

- Parent categories (Men's Fashion, Women's Fashion, Kids' Fashion)
- Subcategories with parent references
- Product count per category

### How to Get Category by ID

```bash
curl http://localhost:8080/categories/CATEGORY_UUID
```

### How to Create a Category (Admin Only)

**Create a parent category:**

```bash
curl -X POST http://localhost:8080/categories \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Accessories",
    "description": "Fashion accessories",
    "imageUrl": "https://images.unsplash.com/photo-accessories",
    "displayOrder": 4,
    "isActive": true,
    "isFeatured": true
  }'
```

**Create a subcategory:**

```bash
curl -X POST http://localhost:8080/categories \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Watches",
    "description": "Wristwatches for all occasions",
    "parentId": "ACCESSORIES_CATEGORY_UUID",
    "displayOrder": 0,
    "isActive": true
  }'
```

---

## Shopping Cart

### How to View Your Cart

```bash
curl http://localhost:8080/cart \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### How to Add Item to Cart

```bash
curl -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PRODUCT_UUID",
    "variantId": "VARIANT_UUID",
    "quantity": 2
  }'
```

### How to Update Cart Item Quantity

```bash
curl -X PUT http://localhost:8080/cart/items/ITEM_UUID \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

### How to Remove Item from Cart

```bash
curl -X DELETE http://localhost:8080/cart/items/ITEM_UUID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### How to Clear Entire Cart

```bash
curl -X DELETE http://localhost:8080/cart \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Order Management

### How to Place an Order

**Step 1: Make sure you have items in your cart**

**Step 2: Create the order**

```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddressId": "ADDRESS_UUID",
    "billingAddressId": "ADDRESS_UUID",
    "paymentMethod": "CARD",
    "notes": "Please leave at the door"
  }'
```

### How to View Your Orders

```bash
curl http://localhost:8080/orders \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Filter by status:**

```bash
curl "http://localhost:8080/orders?status=DELIVERED" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### How to View Order Details

```bash
curl http://localhost:8080/orders/ORDER_UUID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### How to Cancel an Order

```bash
curl -X PUT http://localhost:8080/orders/ORDER_UUID/cancel \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Note:** Orders can only be cancelled if status is `PENDING` or `CONFIRMED`.

### Order Status Flow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓
CANCELLED
```

---

## Reviews & Ratings

### How to View Product Reviews

```bash
curl "http://localhost:8080/reviews?productId=PRODUCT_UUID"
```

### How to Write a Review

**Note:** You must have purchased the product to write a review.

```bash
curl -X POST http://localhost:8080/reviews \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PRODUCT_UUID",
    "rating": 5,
    "title": "Excellent product!",
    "comment": "Very comfortable and great quality. Highly recommend!"
  }'
```

**Rating scale:** 1 (Poor) to 5 (Excellent)

---

## Admin Features

### Admin Dashboard

Get overview statistics:

```bash
curl http://localhost:8080/admin/dashboard \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Returns:**

- Total users, products, orders
- Revenue statistics
- Recent orders
- Top-selling products
- Sales by category

### Manage Users

**List all users:**

```bash
curl http://localhost:8080/admin/users \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Filter by role:**

```bash
curl "http://localhost:8080/admin/users?role=CUSTOMER" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Search users:**

```bash
curl "http://localhost:8080/admin/users?search=john" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Disable a user account:**

```bash
curl -X PUT http://localhost:8080/admin/users/USER_UUID/status \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isActive": false
  }'
```

### Manage Orders

**View all orders:**

```bash
curl http://localhost:8080/admin/orders \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Update order status:**

```bash
curl -X PUT http://localhost:8080/admin/orders/ORDER_UUID/status \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SHIPPED",
    "trackingNumber": "1Z999AA10123456784"
  }'
```

---

## Quick Reference

### Authentication Header

For all authenticated requests, include:

```
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### Content-Type Header

For all POST/PUT requests with JSON body:

```
Content-Type: application/json
```

### Common Response Format

**Success:**

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

**Error:**

```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [...]
}
```

### Testing with cURL

**Tip:** Save your token to a variable for easier testing:

```bash
# Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@foalrider.com","password":"Test@123"}' \
  | jq -r '.data.accessToken')

# Use token in requests
curl http://localhost:8080/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Seeded Test Data

The system comes pre-loaded with:

### Categories (10)

**Parent Categories:**

- Men's Fashion
- Women's Fashion
- Kids' Fashion

**Subcategories:**

- T-Shirts (Men's)
- Shirts (Men's)
- Dresses (Women's)
- Tops (Women's)
- Boys (Kids')
- Girls (Kids')
- Accessories (Kids')

### Brands (3)

- FoalRider (Main brand)
- UrbanStyle
- ClassicWear

### Products (10)

1. Classic Cotton T-Shirt - $29.99 (Men's)
2. Slim Fit Formal Shirt - $49.99 (Men's)
3. Denim Jacket - $89.99 (Men's)
4. Floral Summer Dress - $59.99 (Women's)
5. Casual Blouse Top - $34.99 (Women's)
6. High Waist Jeans - $54.99 (Women's)
7. Kids Cartoon T-Shirt - $19.99 (Boys)
8. Girls Floral Leggings Set - $39.99 (Girls)
9. Boys Denim Shorts - $24.99 (Boys)
10. Kids Sports Shoes - $44.99 (Kids)

Each product includes:

- Multiple product images
- Size variants (S, M, L, XL or Kids sizes)
- Stock quantities
- SKU codes

---

## Troubleshooting

### 401 Unauthorized

- Check that your token is valid and not expired
- Make sure you're using `Bearer ` prefix before the token

### 403 Forbidden

- Your user role doesn't have permission for this action
- Admin endpoints require ADMIN role

### 404 Not Found

- The resource (product, category, order) doesn't exist
- Check the UUID is correct

### 500 Internal Server Error

- Server-side error - check application logs
- Some features may not be fully implemented

---

## Need Help?

- Check the `API_DOCUMENTATION.md` for detailed endpoint specifications
- Review `TESTING.md` for known issues and test results
- Check application logs for error details
