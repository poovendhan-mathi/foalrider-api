cat > /tmp/complete_ordered_test.sh << 'EOF'
#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
REPORT_FILE="/Volumes/POOVENDHAN/FoalRider/BackendSpring/foalrider-api/testing2.md"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "============================================"
echo "FOALRIDER API - COMPREHENSIVE ORDERED TESTING"
echo "============================================"
echo ""

# Variables to store IDs
USER_TOKEN=""
ADMIN_TOKEN=""
CUSTOMER_TOKEN=""
PRODUCT_ID=""
VARIANT_ID=""
CATEGORY_ID=""
CART_ITEM_ID=""
ORDER_ID=""

# Test function
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local auth_type=$4
    local description=$5
    local extract_var=$6
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "\n${BLUE}[$TOTAL_TESTS] Testing: $description${NC}"
    
    local auth_header=""
    if [ "$auth_type" = "user" ]; then
        auth_header="-H \"Authorization: Bearer $USER_TOKEN\""
    elif [ "$auth_type" = "admin" ]; then
        auth_header="-H \"Authorization: Bearer $ADMIN_TOKEN\""
    elif [ "$auth_type" = "customer" ]; then
        auth_header="-H \"Authorization: Bearer $CUSTOMER_TOKEN\""
    fi
    
    local curl_cmd="curl -s -w \"\\n%{http_code}\" -X $method \"$BASE_URL$endpoint\""
    
    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -H \"Content-Type: application/json\" -d '$data'"
    fi
    
    if [ -n "$auth_header" ]; then
        curl_cmd="$curl_cmd $auth_header"
    fi
    
    RESPONSE=$(eval $curl_cmd)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo -e "${GREEN}✓ PASSED ($HTTP_CODE)${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # Extract variable if needed
        if [ -n "$extract_var" ]; then
            local value=$(echo "$BODY" | jq -r "$extract_var" 2>/dev/null)
            echo "Extracted: $value"
            eval "EXTRACTED_VALUE='$value'"
        fi
        
        # Show truncated response
        if [ ${#BODY} -gt 500 ]; then
            echo "Response: ${BODY:0:500}..."
        else
            echo "Response: $BODY"
        fi
    else
        echo -e "${RED}✗ FAILED ($HTTP_CODE)${NC}"
        echo "Response: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo "=========================================="
echo "PHASE 1: AUTHENTICATION & USER SETUP"
echo "=========================================="

# 1. Register new user
test_endpoint "POST" "/auth/register" \
'{
  "email": "testuser_'$(date +%s)'@example.com",
  "password": "Test@123",
  "firstName": "Test",
  "lastName": "User",
  "phone": "+1234567890"
}' \
"none" "Register New User" ".data.accessToken"
USER_TOKEN=$EXTRACTED_VALUE

# 2. Login with existing customer
test_endpoint "POST" "/auth/login" \
'{
  "email": "customer@foalrider.com",
  "password": "Test@123"
}' \
"none" "Login with Customer Account" ".data.accessToken"
CUSTOMER_TOKEN=$EXTRACTED_VALUE

# 3. Login with admin
test_endpoint "POST" "/auth/login" \
'{
  "email": "admin@foalrider.com",
  "password": "Test@123"
}' \
"none" "Login with Admin Account" ".data.accessToken"
ADMIN_TOKEN=$EXTRACTED_VALUE

# 4. Get user profile
test_endpoint "GET" "/users/profile" "" "user" "Get User Profile"

# 5. Refresh token
test_endpoint "POST" "/auth/refresh" \
'{
  "refreshToken": "dummy_token"
}' \
"none" "Refresh Access Token (expected to fail)"

echo ""
echo "=========================================="
echo "PHASE 2: BROWSE PRODUCTS & CATEGORIES"
echo "=========================================="

# 6. Get all products
test_endpoint "GET" "/products?page=0&size=10" "" "none" "Get All Products (Paginated)" ".data.content[0].id"
PRODUCT_ID=$EXTRACTED_VALUE

# 7. Search products
test_endpoint "GET" "/products/search?keyword=shirt&page=0&size=5" "" "none" "Search Products by Keyword"

# 8. Filter products by price
test_endpoint "GET" "/products?minPrice=50&maxPrice=200&page=0&size=10" "" "none" "Filter Products by Price Range"

# 9. Get featured products
test_endpoint "GET" "/products/featured?page=0&size=10" "" "none" "Get Featured Products"

# 10. Get product by ID
if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
    test_endpoint "GET" "/products/$PRODUCT_ID" "" "none" "Get Product by ID"
    
    # Extract variant ID
    RESPONSE=$(curl -s "$BASE_URL/products/$PRODUCT_ID")
    VARIANT_ID=$(echo "$RESPONSE" | jq -r '.data.variants[0].id // empty')
fi

# 11. Get all categories
test_endpoint "GET" "/categories" "" "none" "Get All Categories" ".data[0].id"
CATEGORY_ID=$EXTRACTED_VALUE

# 12. Get category by ID
if [ -n "$CATEGORY_ID" ] && [ "$CATEGORY_ID" != "null" ]; then
    test_endpoint "GET" "/categories/$CATEGORY_ID" "" "none" "Get Category by ID"
fi

# 13. Get products by category
if [ -n "$CATEGORY_ID" ] && [ "$CATEGORY_ID" != "null" ]; then
    test_endpoint "GET" "/products?categoryId=$CATEGORY_ID&page=0&size=5" "" "none" "Get Products by Category"
fi

echo ""
echo "=========================================="
echo "PHASE 3: CART MANAGEMENT"
echo "=========================================="

# 14. Add item to cart
if [ -n "$VARIANT_ID" ] && [ "$VARIANT_ID" != "null" ]; then
    test_endpoint "POST" "/cart/items" \
'{
  "variantId": "'$VARIANT_ID'",
  "quantity": 2
}' \
    "customer" "Add Item to Cart" ".data.id"
    CART_ITEM_ID=$EXTRACTED_VALUE
fi

# 15. Get cart
test_endpoint "GET" "/cart" "" "customer" "Get Shopping Cart"

# 16. Update cart item quantity
if [ -n "$CART_ITEM_ID" ] && [ "$CART_ITEM_ID" != "null" ]; then
    test_endpoint "PUT" "/cart/items/$CART_ITEM_ID" \
'{
  "quantity": 3
}' \
    "customer" "Update Cart Item Quantity"
fi

# 17. Get cart summary
test_endpoint "GET" "/cart/summary" "" "customer" "Get Cart Summary"

echo ""
echo "=========================================="
echo "PHASE 4: ORDER CREATION & MANAGEMENT"
echo "=========================================="

# 18. Create order
test_endpoint "POST" "/orders" \
'{
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  },
  "paymentMethod": "CARD"
}' \
"customer" "Create Order from Cart" ".data.id"
ORDER_ID=$EXTRACTED_VALUE

# 19. Get all orders
test_endpoint "GET" "/orders?page=0&size=10" "" "customer" "Get Customer Orders"

# 20. Get order by ID
if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
    test_endpoint "GET" "/orders/$ORDER_ID" "" "customer" "Get Order by ID"
fi

# 21. Filter orders by status
test_endpoint "GET" "/orders?status=PENDING&page=0&size=10" "" "customer" "Filter Orders by Status"

echo ""
echo "=========================================="
echo "PHASE 5: REVIEWS"
echo "=========================================="

# 22. Get product reviews
if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
    test_endpoint "GET" "/reviews/product/$PRODUCT_ID?page=0&size=5" "" "none" "Get Product Reviews"
fi

# 23. Create review (requires order)
if [ -n "$PRODUCT_ID" ] && [ "$PRODUCT_ID" != "null" ]; then
    test_endpoint "POST" "/reviews" \
'{
  "productId": "'$PRODUCT_ID'",
  "rating": 5,
  "title": "Great product!",
  "comment": "Very satisfied with the quality and delivery."
}' \
    "customer" "Create Product Review"
fi

echo ""
echo "=========================================="
echo "PHASE 6: NOTIFICATIONS"
echo "=========================================="

# 24. Get user notifications
test_endpoint "GET" "/notifications?page=0&size=10" "" "customer" "Get User Notifications"

# 25. Mark notification as read (if exists)
NOTIF_RESPONSE=$(curl -s -H "Authorization: Bearer $CUSTOMER_TOKEN" "$BASE_URL/notifications?page=0&size=1")
NOTIF_ID=$(echo "$NOTIF_RESPONSE" | jq -r '.data.content[0].id // empty')

if [ -n "$NOTIF_ID" ] && [ "$NOTIF_ID" != "null" ]; then
    test_endpoint "PUT" "/notifications/$NOTIF_ID/read" "" "customer" "Mark Notification as Read"
fi

echo ""
echo "=========================================="
echo "PHASE 7: PRICING & CURRENCY"
echo "=========================================="

# 26. Get supported currencies
test_endpoint "GET" "/pricing/currencies" "" "none" "Get Supported Currencies"

# 27. Convert currency
test_endpoint "GET" "/pricing/convert?amount=100&from=USD&to=EUR" "" "none" "Convert Currency (USD to EUR)"

# 28. Convert currency 2
test_endpoint "GET" "/pricing/convert?amount=50&from=EUR&to=GBP" "" "none" "Convert Currency (EUR to GBP)"

echo ""
echo "=========================================="
echo "PHASE 8: ADMIN OPERATIONS"
echo "=========================================="

# 29. Admin Dashboard Stats
test_endpoint "GET" "/admin/dashboard/stats" "" "admin" "Admin Dashboard Statistics"

# 30. Admin - Get All Users
test_endpoint "GET" "/admin/users?page=0&size=10" "" "admin" "Admin - Get All Users"

# 31. Admin - Get All Orders
test_endpoint "GET" "/admin/orders?page=0&size=10" "" "admin" "Admin - Get All Orders"

# 32. Admin - Update Order Status
if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
    test_endpoint "PUT" "/admin/orders/$ORDER_ID/status" \
'{
  "status": "PROCESSING"
}' \
    "admin" "Admin - Update Order Status"
fi

# 33. Admin - Get User by ID
USERS_RESPONSE=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/admin/users?page=0&size=1")
USER_ID=$(echo "$USERS_RESPONSE" | jq -r '.data.content[0].id // empty')

if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ]; then
    test_endpoint "GET" "/admin/users/$USER_ID" "" "admin" "Admin - Get User by ID"
fi

echo ""
echo "=========================================="
echo "PHASE 9: CLEANUP (OPTIONAL)"
echo "=========================================="

# 34. Remove cart item
if [ -n "$CART_ITEM_ID" ] && [ "$CART_ITEM_ID" != "null" ]; then
    test_endpoint "DELETE" "/cart/items/$CART_ITEM_ID" "" "customer" "Remove Item from Cart"
fi

# 35. Clear entire cart
test_endpoint "DELETE" "/cart/clear" "" "customer" "Clear Shopping Cart"

echo ""
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
echo -e "${BLUE}Total Tests: $TOTAL_TESTS${NC}"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(awk "BEGIN {printf \"%.1f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")
    echo -e "${BLUE}Success Rate: $SUCCESS_RATE%${NC}"
fi

echo ""
echo "Full test report saved to: $REPORT_FILE"
echo "=========================================="

EOF

chmod +x /tmp/complete_ordered_test.sh
