package com.foalrider.shared.constants;

/**
 * Application-wide constants.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // ===========================================
    // Pagination
    // ===========================================
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // ===========================================
    // Roles
    // ===========================================
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    // ===========================================
    // JWT
    // ===========================================
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // ===========================================
    // API Paths
    // ===========================================
    public static final String API_BASE_PATH = "/api/v1";
    public static final String AUTH_PATH = "/auth";
    public static final String USERS_PATH = "/users";
    public static final String PRODUCTS_PATH = "/products";
    public static final String CATEGORIES_PATH = "/categories";
    public static final String BRANDS_PATH = "/brands";
    public static final String CART_PATH = "/cart";
    public static final String ORDERS_PATH = "/orders";
    public static final String PAYMENTS_PATH = "/payments";
    public static final String REVIEWS_PATH = "/reviews";
    public static final String WISHLIST_PATH = "/wishlist";
    public static final String COUPONS_PATH = "/coupons";
    public static final String ADDRESSES_PATH = "/addresses";
    public static final String ADMIN_PATH = "/admin";

    // ===========================================
    // Validation
    // ===========================================
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 50;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;
    public static final int REVIEW_COMMENT_MAX_LENGTH = 1000;

    // ===========================================
    // Order Status
    // ===========================================
    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_PROCESSING = "PROCESSING";
    public static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_STATUS_REFUNDED = "REFUNDED";

    // ===========================================
    // Payment Status
    // ===========================================
    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_PROCESSING = "PROCESSING";
    public static final String PAYMENT_STATUS_COMPLETED = "COMPLETED";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";

    // ===========================================
    // Cache Keys
    // ===========================================
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_BRANDS = "brands";
    public static final long CACHE_TTL_SECONDS = 3600; // 1 hour

    // ===========================================
    // Regex Patterns
    // ===========================================
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    public static final String PHONE_REGEX = "^\\+?[1-9]\\d{1,14}$";
    public static final String SLUG_REGEX = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    public static final String SKU_REGEX = "^[A-Z0-9-]+$";
}
