# 🐎 FoalRider - Complete E-Commerce Backend Architecture Blueprint

## 📋 Executive Summary

**FoalRider** is a production-grade Spring Boot Maven backend for a clothing e-commerce platform. This document provides the complete architecture, database design, and implementation roadmap.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FOALRIDER SYSTEM OVERVIEW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              ┌─────────────────┐                            │
│                              │   Swagger UI    │                            │
│                              │  /swagger-ui    │                            │
│                              └────────┬────────┘                            │
│                                       │                                     │
│   ┌──────────┐    ┌──────────┐       ▼         ┌──────────────────┐        │
│   │  Mobile  │    │   Web    │  ┌─────────────┐│    Supabase      │        │
│   │   Apps   │───▶│  Client  │─▶│  FoalRider  ││   PostgreSQL     │        │
│   └──────────┘    └──────────┘  │   Spring    ││                  │        │
│                                 │    Boot     │└──────────────────┘        │
│   ┌──────────┐    ┌──────────┐  │   Backend   │                            │
│   │  Admin   │───▶│  Staff   │─▶│             │  ┌──────────────────┐      │
│   │  Panel   │    │  Portal  │  │  REST APIs  │─▶│      Redis       │      │
│   └──────────┘    └──────────┘  │             │  │     (Cache)      │      │
│                                 │             │  └──────────────────┘      │
│                                 │             │                            │
│                                 │             │  ┌──────────────────┐      │
│                                 │             │─▶│     Stripe       │      │
│                                 └─────────────┘  │   (Payments)     │      │
│                                                  └──────────────────┘      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠 Technology Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TECHNOLOGY STACK                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  CATEGORY             │ TECHNOLOGY                │ VERSION    │ PURPOSE   │
│  ─────────────────────┼───────────────────────────┼────────────┼──────────│
│                                                                             │
│  ◆ CORE FRAMEWORK                                                           │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Build Tool           │ Apache Maven              │ 3.9+       │ Deps mgmt │
│  Framework            │ Spring Boot               │ 3.2.x      │ Core      │
│  Language             │ Java                      │ 21 LTS     │ Backend   │
│                                                                             │
│  ◆ DATABASE & CACHING                                                       │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Primary DB           │ Supabase PostgreSQL       │ 15+        │ Data store│
│  Cache                │ Redis                     │ 7+         │ Caching   │
│  ORM                  │ Spring Data JPA/Hibernate │ 6.4+       │ DB access │
│  Migrations           │ Flyway                    │ 9+         │ DB version│
│                                                                             │
│  ◆ SECURITY                                                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Auth Framework       │ Spring Security           │ 6.2+       │ Security  │
│  Token                │ JJWT                      │ 0.12+      │ JWT       │
│  Password             │ BCrypt                    │ Built-in   │ Hashing   │
│                                                                             │
│  ◆ API & DOCUMENTATION                                                      │
│  ─────────────────────────────────────────────────────────────────────────  │
│  API Docs             │ SpringDoc OpenAPI         │ 2.3+       │ Swagger   │
│  Validation           │ Jakarta Validation        │ 3.0+       │ Input val │
│  JSON                 │ Jackson                   │ 2.16+      │ Serialize │
│                                                                             │
│  ◆ PAYMENTS                                                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Payment Gateway      │ Stripe Java SDK           │ 24+        │ Payments  │
│                                                                             │
│  ◆ UTILITIES                                                                │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Mapping              │ MapStruct                 │ 1.5+       │ DTO map   │
│  Boilerplate          │ Lombok                    │ 1.18+      │ Code gen  │
│  Logging              │ SLF4J + Logback           │ Built-in   │ Logging   │
│                                                                             │
│  ◆ MONITORING                                                               │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Metrics              │ Micrometer                │ 1.12+      │ Metrics   │
│  Health               │ Spring Actuator           │ Built-in   │ Health    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Maven POM Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         POM.XML DEPENDENCIES                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  <?xml version="1.0" encoding="UTF-8"?>                                     │
│  <project>                                                                  │
│      <groupId>com.foalrider</groupId>                                       │
│      <artifactId>foalrider-api</artifactId>                                 │
│      <version>1.0.0</version>                                               │
│      <packaging>jar</packaging>                                             │
│                                                                             │
│      <parent>                                                               │
│          spring-boot-starter-parent : 3.2.x                                 │
│      </parent>                                                              │
│                                                                             │
│      <properties>                                                           │
│          java.version           : 21                                        │
│          jjwt.version           : 0.12.3                                    │
│          mapstruct.version      : 1.5.5.Final                               │
│          springdoc.version      : 2.3.0                                     │
│          stripe.version         : 24.0.0                                    │
│      </properties>                                                          │
│                                                                             │
│      DEPENDENCIES:                                                          │
│      ─────────────                                                          │
│      ├── spring-boot-starter-web          (REST APIs)                       │
│      ├── spring-boot-starter-data-jpa     (Database)                        │
│      ├── spring-boot-starter-security     (Security)                        │
│      ├── spring-boot-starter-validation   (Validation)                      │
│      ├── spring-boot-starter-data-redis   (Caching)                         │
│      ├── spring-boot-starter-actuator     (Monitoring)                      │
│      ├── spring-boot-starter-mail         (Emails)                          │
│      ├── postgresql                       (DB Driver)                       │
│      ├── flyway-core                      (Migrations)                      │
│      ├── jjwt-api, jjwt-impl, jjwt-jackson (JWT)                           │
│      ├── springdoc-openapi-starter-webmvc-ui (Swagger)                     │
│      ├── stripe-java                      (Payments)                        │
│      ├── mapstruct                        (DTO Mapping)                     │
│      ├── lombok                           (Boilerplate)                     │
│      └── spring-boot-starter-test         (Testing)                         │
│                                                                             │
│  </project>                                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FOALRIDER PROJECT STRUCTURE                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  foalrider-api/                                                             │
│  │                                                                          │
│  ├── 📄 pom.xml                                                             │
│  ├── 📄 README.md                                                           │
│  ├── 📄 .env.example                                                        │
│  │                                                                          │
│  ├── 📁 src/main/java/com/foalrider/                                        │
│  │   │                                                                      │
│  │   ├── 📄 FoalRiderApplication.java          # Main entry point           │
│  │   │                                                                      │
│  │   ├── 📁 config/                            # ⚙️ CONFIGURATION           │
│  │   │   ├── SecurityConfig.java               # Spring Security setup      │
│  │   │   ├── JwtConfig.java                    # JWT properties             │
│  │   │   ├── RedisConfig.java                  # Redis cache config         │
│  │   │   ├── StripeConfig.java                 # Stripe API config          │
│  │   │   ├── OpenApiConfig.java                # Swagger/OpenAPI config     │
│  │   │   ├── WebConfig.java                    # CORS, interceptors         │
│  │   │   └── AuditConfig.java                  # JPA auditing               │
│  │   │                                                                      │
│  │   ├── 📁 security/                          # 🔐 SECURITY                │
│  │   │   ├── 📁 jwt/                                                        │
│  │   │   │   ├── JwtTokenProvider.java         # Token generation           │
│  │   │   │   ├── JwtAuthenticationFilter.java  # Request filter             │
│  │   │   │   └── JwtAuthEntryPoint.java        # Auth error handler         │
│  │   │   ├── CustomUserDetails.java            # User principal             │
│  │   │   ├── CustomUserDetailsService.java     # Load user                  │
│  │   │   └── SecurityUtils.java                # Security helpers           │
│  │   │                                                                      │
│  │   ├── 📁 modules/                           # 📦 FEATURE MODULES         │
│  │   │   │                                                                  │
│  │   │   ├── 📁 auth/                          # Authentication             │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   │   └── AuthController.java                                    │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   │   ├── AuthService.java                                       │
│  │   │   │   │   └── AuthServiceImpl.java                                   │
│  │   │   │   ├── 📁 dto/                                                    │
│  │   │   │   │   ├── LoginRequest.java                                      │
│  │   │   │   │   ├── RegisterRequest.java                                   │
│  │   │   │   │   ├── AuthResponse.java                                      │
│  │   │   │   │   ├── RefreshTokenRequest.java                               │
│  │   │   │   │   └── PasswordResetRequest.java                              │
│  │   │   │   └── 📁 exception/                                              │
│  │   │   │       └── AuthenticationException.java                           │
│  │   │   │                                                                  │
│  │   │   ├── 📁 user/                          # User Management            │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   │   ├── UserController.java       # User endpoints             │
│  │   │   │   │   └── AdminUserController.java  # Admin user mgmt            │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   ├── 📁 repository/                                             │
│  │   │   │   ├── 📁 entity/                                                 │
│  │   │   │   │   ├── User.java                                              │
│  │   │   │   │   ├── Role.java                                              │
│  │   │   │   │   └── RefreshToken.java                                      │
│  │   │   │   ├── 📁 dto/                                                    │
│  │   │   │   └── 📁 mapper/                                                 │
│  │   │   │                                                                  │
│  │   │   ├── 📁 product/                       # Product Catalog            │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   │   ├── ProductController.java    # Public product APIs        │
│  │   │   │   │   └── AdminProductController.java # Admin product CRUD       │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   ├── 📁 repository/                                             │
│  │   │   │   ├── 📁 entity/                                                 │
│  │   │   │   │   ├── Product.java                                           │
│  │   │   │   │   ├── ProductVariant.java                                    │
│  │   │   │   │   ├── ProductImage.java                                      │
│  │   │   │   │   ├── Size.java                                              │
│  │   │   │   │   └── Color.java                                             │
│  │   │   │   ├── 📁 dto/                                                    │
│  │   │   │   ├── 📁 mapper/                                                 │
│  │   │   │   └── 📁 specification/             # Dynamic queries            │
│  │   │   │                                                                  │
│  │   │   ├── 📁 category/                      # Categories                 │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   ├── 📁 repository/                                             │
│  │   │   │   ├── 📁 entity/                                                 │
│  │   │   │   │   └── Category.java                                          │
│  │   │   │   ├── 📁 dto/                                                    │
│  │   │   │   └── 📁 mapper/                                                 │
│  │   │   │                                                                  │
│  │   │   ├── 📁 brand/                         # Brands                     │
│  │   │   │   └── ... (same structure)                                       │
│  │   │   │                                                                  │
│  │   │   ├── 📁 cart/                          # Shopping Cart              │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   │   └── CartController.java                                    │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   ├── 📁 repository/                                             │
│  │   │   │   ├── 📁 entity/                                                 │
│  │   │   │   │   ├── Cart.java                                              │
│  │   │   │   │   └── CartItem.java                                          │
│  │   │   │   ├── 📁 dto/                                                    │
│  │   │   │   └── 📁 mapper/                                                 │
│  │   │   │                                                                  │
│  │   │   ├── 📁 order/                         # Orders                     │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   │   ├── OrderController.java      # Customer orders            │
│  │   │   │   │   └── AdminOrderController.java # Admin order mgmt           │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   ├── 📁 repository/                                             │
│  │   │   │   ├── 📁 entity/                                                 │
│  │   │   │   │   ├── Order.java                                             │
│  │   │   │   │   ├── OrderItem.java                                         │
│  │   │   │   │   └── OrderStatus.java (enum)                                │
│  │   │   │   ├── 📁 dto/                                                    │
│  │   │   │   └── 📁 mapper/                                                 │
│  │   │   │                                                                  │
│  │   │   ├── 📁 payment/                       # Payments (Stripe)          │
│  │   │   │   ├── 📁 controller/                                             │
│  │   │   │   │   ├── PaymentController.java                                 │
│  │   │   │   │   └── StripeWebhookController.java                           │
│  │   │   │   ├── 📁 service/                                                │
│  │   │   │   │   ├── PaymentService.java                                    │
│  │   │   │   │   └── StripeService.java                                     │
│  │   │   │   ├── 📁 entity/                                                 │
│  │   │   │   │   └── Payment.java                                           │
│  │   │   │   └── 📁 dto/                                                    │
│  │   │   │                                                                  │
│  │   │   ├── 📁 address/                       # Addresses                  │
│  │   │   │   └── ... (same structure)                                       │
│  │   │   │                                                                  │
│  │   │   ├── 📁 wishlist/                      # Wishlists                  │
│  │   │   │   └── ... (same structure)                                       │
│  │   │   │                                                                  │
│  │   │   ├── 📁 review/                        # Reviews & Ratings          │
│  │   │   │   └── ... (same structure)                                       │
│  │   │   │                                                                  │
│  │   │   ├── 📁 coupon/                        # Coupons & Discounts        │
│  │   │   │   └── ... (same structure)                                       │
│  │   │   │                                                                  │
│  │   │   └── 📁 notification/                  # Notifications              │
│  │   │       ├── 📁 service/                                                │
│  │   │       │   ├── EmailService.java                                      │
│  │   │       │   └── NotificationService.java                               │
│  │   │       └── 📁 template/                                               │
│  │   │                                                                      │
│  │   ├── 📁 shared/                            # 🔄 SHARED COMPONENTS       │
│  │   │   ├── 📁 entity/                                                     │
│  │   │   │   └── BaseEntity.java               # Auditing base              │
│  │   │   ├── 📁 dto/                                                        │
│  │   │   │   ├── ApiResponse.java              # Standard response          │
│  │   │   │   ├── PagedResponse.java            # Pagination response        │
│  │   │   │   └── ErrorResponse.java            # Error response             │
│  │   │   ├── 📁 exception/                                                  │
│  │   │   │   ├── GlobalExceptionHandler.java   # Central error handler      │
│  │   │   │   ├── ResourceNotFoundException.java                             │
│  │   │   │   ├── BadRequestException.java                                   │
│  │   │   │   ├── UnauthorizedException.java                                 │
│  │   │   │   └── BusinessException.java                                     │
│  │   │   ├── 📁 validation/                                                 │
│  │   │   │   └── ... (custom validators)                                    │
│  │   │   ├── 📁 util/                                                       │
│  │   │   │   ├── SlugUtils.java                                             │
│  │   │   │   └── PriceUtils.java                                            │
│  │   │   └── 📁 constants/                                                  │
│  │   │       └── AppConstants.java                                          │
│  │   │                                                                      │
│  │   └── 📁 infrastructure/                    # 🏗️ INFRASTRUCTURE         │
│  │       ├── 📁 cache/                                                      │
│  │       │   └── CacheService.java                                          │
│  │       ├── 📁 logging/                                                    │
│  │       │   └── LoggingAspect.java                                         │
│  │       └── 📁 ratelimit/                                                  │
│  │           └── RateLimitingAspect.java                                    │
│  │                                                                          │
│  ├── 📁 src/main/resources/                                                 │
│  │   ├── 📄 application.yml                    # Main config                │
│  │   ├── 📄 application-dev.yml                # Dev profile                │
│  │   ├── 📄 application-prod.yml               # Prod profile               │
│  │   ├── 📁 db/migration/                      # Flyway migrations          │
│  │   │   ├── V1__create_users_table.sql                                     │
│  │   │   ├── V2__create_products_table.sql                                  │
│  │   │   ├── V3__create_orders_table.sql                                    │
│  │   │   └── ...                                                            │
│  │   └── 📁 templates/                         # Email templates            │
│  │       └── ...                                                            │
│  │                                                                          │
│  └── 📁 src/test/java/com/foalrider/          # Tests                       │
│      ├── 📁 unit/                                                           │
│      ├── 📁 integration/                                                    │
│      └── 📁 e2e/                                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Design

### Complete Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 FOALRIDER DATABASE SCHEMA                                        │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                 │
│   ╔═══════════════════════════════════════════════════════════════════════════════════════╗    │
│   ║                              USER & AUTHENTICATION                                     ║    │
│   ╚═══════════════════════════════════════════════════════════════════════════════════════╝    │
│                                                                                                 │
│   ┌─────────────────────┐          ┌─────────────────────┐          ┌─────────────────────┐    │
│   │       roles         │          │        users        │          │     addresses       │    │
│   ├─────────────────────┤          ├─────────────────────┤          ├─────────────────────┤    │
│   │ • id (PK, UUID)     │◄────┐    │ • id (PK, UUID)     │────────┬▶│ • id (PK, UUID)     │    │
│   │ • name              │     └────│ • role_id (FK)      │        │ │ • user_id (FK)      │    │
│   │ • description       │          │ • email (UNIQUE)    │        │ │ • label             │    │
│   │ • permissions (JSON)│          │ • password_hash     │        │ │ • recipient_name    │    │
│   │ • is_system         │          │ • first_name        │        │ │ • phone             │    │
│   │ • created_at        │          │ • last_name         │        │ │ • address_line1     │    │
│   └─────────────────────┘          │ • phone             │        │ │ • address_line2     │    │
│                                    │ • avatar_url        │        │ │ • city              │    │
│   DEFAULT ROLES:                   │ • is_email_verified │        │ │ • state             │    │
│   ├── ROLE_CUSTOMER                │ • is_active         │        │ │ • postal_code       │    │
│   ├── ROLE_STAFF                   │ • last_login_at     │        │ │ • country           │    │
│   ├── ROLE_ADMIN                   │ • created_at        │        │ │ • is_default        │    │
│   └── ROLE_SUPER_ADMIN             │ • updated_at        │        │ │ • address_type      │    │
│                                    └──────────┬──────────┘        │ │ • created_at        │    │
│                                               │                   │ └─────────────────────┘    │
│                                               │                   │                            │
│   ┌─────────────────────┐                    │                   │  ┌─────────────────────┐   │
│   │   refresh_tokens    │◄───────────────────┘                   │  │     wishlists       │   │
│   ├─────────────────────┤                                        │  ├─────────────────────┤   │
│   │ • id (PK, UUID)     │                                        └─▶│ • id (PK, UUID)     │   │
│   │ • user_id (FK)      │                                           │ • user_id (FK)      │   │
│   │ • token_hash        │                                           │ • product_id (FK)   │──┐│
│   │ • device_info (JSON)│                                           │ • created_at        │  ││
│   │ • ip_address        │                                           └─────────────────────┘  ││
│   │ • expires_at        │                                                                    ││
│   │ • is_revoked        │                                                                    ││
│   │ • created_at        │                                                                    ││
│   └─────────────────────┘                                                                    ││
│                                                                                              ││
│   ╔═══════════════════════════════════════════════════════════════════════════════════════╗ ││
│   ║                              PRODUCT CATALOG                                           ║ ││
│   ╚═══════════════════════════════════════════════════════════════════════════════════════╝ ││
│                                                                                              ││
│   ┌─────────────────────┐          ┌─────────────────────┐          ┌─────────────────────┐ ││
│   │     categories      │          │       brands        │          │      products       │◄┘│
│   ├─────────────────────┤          ├─────────────────────┤          ├─────────────────────┤  │
│   │ • id (PK, UUID)     │◄────┐    │ • id (PK, UUID)     │◄────┐    │ • id (PK, UUID)     │  │
│   │ • parent_id (FK)────│─────┘    │ • name              │     │    │ • category_id (FK)  │──┤
│   │ • name              │     ┌────│ • slug (UNIQUE)     │     └────│ • brand_id (FK)     │  │
│   │ • slug (UNIQUE)     │     │    │ • logo_url          │          │ • name              │  │
│   │ • description       │     │    │ • description       │          │ • slug (UNIQUE)     │  │
│   │ • image_url         │     │    │ • is_active         │          │ • short_description │  │
│   │ • meta_title        │     │    │ • sort_order        │          │ • description       │  │
│   │ • meta_description  │     │    │ • created_at        │          │ • material          │  │
│   │ • is_active         │     │    └─────────────────────┘          │ • care_instructions │  │
│   │ • sort_order        │     │                                     │ • base_price        │  │
│   │ • created_at        │     │                                     │ • compare_at_price  │  │
│   └─────────────────────┘     │                                     │ • is_featured       │  │
│                               │                                     │ • is_new_arrival    │  │
│   CATEGORY TREE:              │                                     │ • is_on_sale        │  │
│   ├── Men's                   │                                     │ • is_active         │  │
│   │   ├── T-Shirts            │                                     │ • meta_title        │  │
│   │   ├── Shirts              │                                     │ • meta_description  │  │
│   │   ├── Jeans               │                                     │ • tags (ARRAY)      │  │
│   │   └── Jackets             │                                     │ • avg_rating        │  │
│   ├── Women's                 │                                     │ • review_count      │  │
│   │   ├── Dresses             │                                     │ • view_count        │  │
│   │   ├── Tops                │                                     │ • created_at        │  │
│   │   └── Skirts              │                                     │ • updated_at        │  │
│   └── Kids                    │                                     └──────────┬──────────┘  │
│       ├── Boys                │                                                │             │
│       └── Girls               │                              ┌─────────────────┼─────────────┤
│                               │                              │                 │             │
│                               │                              ▼                 ▼             │
│                               │    ┌─────────────────────┐  ┌─────────────────────┐          │
│                               │    │   product_images    │  │  product_variants   │          │
│                               │    ├─────────────────────┤  ├─────────────────────┤          │
│                               │    │ • id (PK, UUID)     │  │ • id (PK, UUID)     │◄─────────┤
│                               │    │ • product_id (FK)   │  │ • product_id (FK)   │          │
│                               │    │ • color_id (FK)     │──│ • size_id (FK)      │──────┐   │
│                               │    │ • image_url         │  │ • color_id (FK)     │────┐ │   │
│                               │    │ • thumbnail_url     │  │ • sku (UNIQUE)      │    │ │   │
│                               │    │ • alt_text          │  │ • barcode           │    │ │   │
│                               │    │ • is_primary        │  │ • price             │    │ │   │
│                               │    │ • sort_order        │  │ • cost_price        │    │ │   │
│                               │    │ • created_at        │  │ • stock_quantity    │    │ │   │
│                               │    └─────────────────────┘  │ • low_stock_alert   │    │ │   │
│                               │                             │ • weight            │    │ │   │
│                               │                             │ • is_active         │    │ │   │
│   ┌─────────────────────┐     │                             │ • created_at        │    │ │   │
│   │       colors        │◄────┼─────────────────────────────└─────────────────────┘    │ │   │
│   ├─────────────────────┤     │                                                        │ │   │
│   │ • id (PK, UUID)     │     │     ┌─────────────────────┐                            │ │   │
│   │ • name              │     │     │       sizes         │◄───────────────────────────┼─┘   │
│   │ • hex_code          │     │     ├─────────────────────┤                            │     │
│   │ • swatch_image_url  │     │     │ • id (PK, UUID)     │                            │     │
│   │ • sort_order        │     │     │ • name (XS,S,M...)  │                            │     │
│   └─────────────────────┘     │     │ • display_name      │                            │     │
│                               │     │ • size_category     │                            │     │
│   EXAMPLE COLORS:             │     │ • measurements(JSON)│                            │     │
│   ├── Black (#000000)         │     │ • sort_order        │                            │     │
│   ├── White (#FFFFFF)         │     └─────────────────────┘                            │     │
│   ├── Navy (#001F3F)          │                                                        │     │
│   ├── Red (#FF4136)           │     SIZE CATEGORIES:                                   │     │
│   └── Beige (#F5F5DC)         │     ├── CLOTHING (XS-XXXL)                             │     │
│                               │     ├── NUMERIC (28-42)                                │     │
│                               │     ├── SHOES (6-12)                                   │     │
│                               │     └── KIDS (2T-14)                                   │     │
│                               │                                                        │     │
│   ╔═══════════════════════════════════════════════════════════════════════════════════════╗ │
│   ║                              SHOPPING & ORDERS                                         ║ │
│   ╚═══════════════════════════════════════════════════════════════════════════════════════╝ │
│                                                                                              │
│   ┌─────────────────────┐          ┌─────────────────────┐                                  │
│   │        carts        │          │     cart_items      │                                  │
│   ├─────────────────────┤          ├─────────────────────┤                                  │
│   │ • id (PK, UUID)     │◄─────────│ • id (PK, UUID)     │                                  │
│   │ • user_id (FK)      │          │ • cart_id (FK)      │                                  │
│   │ • session_id        │          │ • variant_id (FK)   │─────────────────────────────────▶│
│   │ • coupon_id (FK)    │──┐       │ • quantity          │                                  │
│   │ • expires_at        │  │       │ • added_at          │                                  │
│   │ • created_at        │  │       │ • updated_at        │                                  │
│   │ • updated_at        │  │       └─────────────────────┘                                  │
│   └─────────────────────┘  │                                                                │
│                            │                                                                │
│                            ▼                                                                │
│   ┌─────────────────────┐          ┌─────────────────────┐          ┌─────────────────────┐│
│   │       coupons       │          │       orders        │          │    order_items      ││
│   ├─────────────────────┤          ├─────────────────────┤          ├─────────────────────┤│
│   │ • id (PK, UUID)     │◄────┐    │ • id (PK, UUID)     │◄─────────│ • id (PK, UUID)     ││
│   │ • code (UNIQUE)     │     │    │ • user_id (FK)      │          │ • order_id (FK)     ││
│   │ • description       │     └────│ • coupon_id (FK)    │     ┌────│ • variant_id (FK)   ││
│   │ • discount_type     │          │ • order_number      │     │    │ • product_name      ││
│   │ • discount_value    │          │ • status            │     │    │ • variant_name      ││
│   │ • min_purchase_amt  │          │ • payment_status    │     │    │ • sku               ││
│   │ • max_discount_amt  │          │ • subtotal          │     │    │ • quantity          ││
│   │ • usage_limit       │          │ • discount_amount   │     │    │ • unit_price        ││
│   │ • usage_per_user    │          │ • tax_amount        │     │    │ • total_price       ││
│   │ • used_count        │          │ • shipping_amount   │     │    │ • created_at        ││
│   │ • applies_to        │          │ • total_amount      │     │    └─────────────────────┘│
│   │ • applicable_ids    │          │ • currency          │     │                           │
│   │ • valid_from        │          │ • shipping_address  │     │    ┌─────────────────────┐│
│   │ • valid_until       │          │ • billing_address   │     │    │      payments       ││
│   │ • is_active         │          │ • shipping_method   │     │    ├─────────────────────┤│
│   │ • created_at        │          │ • tracking_number   │     │    │ • id (PK, UUID)     ││
│   └─────────────────────┘          │ • notes             │     └───▶│ • order_id (FK)     ││
│                                    │ • estimated_delivery│          │ • stripe_payment_id ││
│   DISCOUNT TYPES:                  │ • shipped_at        │          │ • stripe_charge_id  ││
│   ├── PERCENTAGE (20%)             │ • delivered_at      │          │ • amount            ││
│   ├── FIXED ($10)                  │ • cancelled_at      │          │ • currency          ││
│   └── FREE_SHIPPING                │ • cancellation_reason          │ • status            ││
│                                    │ • created_at        │          │ • payment_method    ││
│   ORDER STATUS:                    │ • updated_at        │          │ • card_last_four    ││
│   PENDING → CONFIRMED              └─────────────────────┘          │ • card_brand        ││
│      ↓                                                              │ • failure_reason    ││
│   PROCESSING → SHIPPED → DELIVERED                                  │ • refunded_amount   ││
│      ↓                                                              │ • metadata (JSON)   ││
│   CANCELLED → REFUNDED                                              │ • created_at        ││
│                                                                     │ • updated_at        ││
│                                                                     └─────────────────────┘│
│   ╔═══════════════════════════════════════════════════════════════════════════════════════╗│
│   ║                              REVIEWS & TRACKING                                        ║│
│   ╚═══════════════════════════════════════════════════════════════════════════════════════╝│
│                                                                                             │
│   ┌─────────────────────┐          ┌─────────────────────┐                                 │
│   │      reviews        │          │    coupon_usage     │                                 │
│   ├─────────────────────┤          ├─────────────────────┤                                 │
│   │ • id (PK, UUID)     │          │ • id (PK, UUID)     │                                 │
│   │ • user_id (FK)      │          │ • coupon_id (FK)    │                                 │
│   │ • product_id (FK)   │          │ • user_id (FK)      │                                 │
│   │ • order_id (FK)     │          │ • order_id (FK)     │                                 │
│   │ • rating (1-5)      │          │ • discount_amount   │                                 │
│   │ • title             │          │ • used_at           │                                 │
│   │ • comment           │          └─────────────────────┘                                 │
│   │ • pros (ARRAY)      │                                                                  │
│   │ • cons (ARRAY)      │          FIT FEEDBACK:                                           │
│   │ • fit_feedback      │          ├── RUNS_SMALL                                          │
│   │ • is_verified       │          ├── TRUE_TO_SIZE                                        │
│   │ • is_approved       │          └── RUNS_LARGE                                          │
│   │ • helpful_count     │                                                                  │
│   │ • created_at        │                                                                  │
│   └─────────────────────┘                                                                  │
│                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Entity Relationships Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       RELATIONSHIP MATRIX                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  RELATIONSHIP        │ TYPE          │ DESCRIPTION                         │
│  ────────────────────┼───────────────┼─────────────────────────────────────│
│                                                                             │
│  ◆ ONE-TO-MANY                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Role → Users        │ 1:N           │ One role has many users             │
│  User → Addresses    │ 1:N           │ User can have multiple addresses    │
│  User → Orders       │ 1:N           │ User can place many orders          │
│  User → Reviews      │ 1:N           │ User can write many reviews         │
│  User → RefreshTokens│ 1:N           │ User can have multiple sessions     │
│  Category → Products │ 1:N           │ Category contains many products     │
│  Category → Children │ 1:N (self)    │ Nested category structure           │
│  Brand → Products    │ 1:N           │ Brand has many products             │
│  Product → Variants  │ 1:N           │ Product has size/color combos       │
│  Product → Images    │ 1:N           │ Product has multiple images         │
│  Product → Reviews   │ 1:N           │ Product receives many reviews       │
│  Order → OrderItems  │ 1:N           │ Order contains many items           │
│  Order → Payments    │ 1:N           │ Order can have payment attempts     │
│  Cart → CartItems    │ 1:N           │ Cart contains many items            │
│  Coupon → CouponUsage│ 1:N           │ Coupon used multiple times          │
│                                                                             │
│  ◆ MANY-TO-MANY (via junction tables)                                       │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Product ↔ Size      │ N:M           │ Via product_variants                │
│  Product ↔ Color     │ N:M           │ Via product_variants                │
│  User ↔ Product      │ N:M           │ Via wishlists                       │
│  User ↔ Coupon       │ N:M           │ Via coupon_usage                    │
│                                                                             │
│  ◆ ONE-TO-ONE                                                               │
│  ─────────────────────────────────────────────────────────────────────────  │
│  User → Cart         │ 1:1           │ Each user has one active cart       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Database Indexes Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         INDEX STRATEGY                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  TABLE              │ INDEX                      │ TYPE     │ PURPOSE      │
│  ───────────────────┼────────────────────────────┼──────────┼─────────────│
│                                                                             │
│  ◆ USERS                                                                    │
│  users              │ idx_users_email            │ UNIQUE   │ Login lookup │
│  users              │ idx_users_role_id          │ BTREE    │ Role filter  │
│  users              │ idx_users_created_at       │ BTREE    │ Sorting      │
│                                                                             │
│  ◆ PRODUCTS                                                                 │
│  products           │ idx_products_slug          │ UNIQUE   │ URL lookup   │
│  products           │ idx_products_category      │ BTREE    │ Category list│
│  products           │ idx_products_brand         │ BTREE    │ Brand filter │
│  products           │ idx_products_featured      │ BTREE    │ Featured list│
│  products           │ idx_products_active        │ BTREE    │ Active filter│
│  products           │ idx_products_price         │ BTREE    │ Price sort   │
│  products           │ idx_products_search        │ GIN      │ Full-text    │
│                                                                             │
│  ◆ PRODUCT VARIANTS                                                         │
│  product_variants   │ idx_variants_sku           │ UNIQUE   │ SKU lookup   │
│  product_variants   │ idx_variants_product       │ BTREE    │ Product link │
│  product_variants   │ idx_variants_stock         │ BTREE    │ Stock check  │
│  product_variants   │ idx_variants_composite     │ UNIQUE   │ Unique combo │
│                     │ (product_id, size_id, color_id)                       │
│                                                                             │
│  ◆ ORDERS                                                                   │
│  orders             │ idx_orders_user            │ BTREE    │ User orders  │
│  orders             │ idx_orders_number          │ UNIQUE   │ Order lookup │
│  orders             │ idx_orders_status          │ BTREE    │ Status filter│
│  orders             │ idx_orders_created         │ BTREE    │ Date sort    │
│                                                                             │
│  ◆ CATEGORIES                                                               │
│  categories         │ idx_categories_slug        │ UNIQUE   │ URL lookup   │
│  categories         │ idx_categories_parent      │ BTREE    │ Tree query   │
│                                                                             │
│  ◆ COUPONS                                                                  │
│  coupons            │ idx_coupons_code           │ UNIQUE   │ Code lookup  │
│  coupons            │ idx_coupons_valid          │ BTREE    │ Active check │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Security Architecture

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       AUTHENTICATION FLOWS                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      1. REGISTRATION                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│   Client                      Server                       Database         │
│     │                           │                             │             │
│     │  POST /api/v1/auth/register                             │             │
│     │  ─────────────────────────▶                             │             │
│     │  {                        │                             │             │
│     │    email,                 │  ① Validate input           │             │
│     │    password,              │  ② Check email unique       │             │
│     │    firstName,             │  ③ Hash password (BCrypt)   │             │
│     │    lastName               │  ④ Assign ROLE_CUSTOMER     │             │
│     │  }                        │  ⑤ Generate verification    │             │
│     │                           │     token                   │             │
│     │                           │  ────────────────────────────▶            │
│     │                           │         Save User           │             │
│     │                           │  ◀────────────────────────────            │
│     │                           │                             │             │
│     │                           │  ⑥ Send verification email  │             │
│     │  ◀─────────────────────────                             │             │
│     │  201 Created              │                             │             │
│     │  {userId, message}        │                             │             │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      2. LOGIN                                        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│   Client                      Server                       Database         │
│     │                           │                             │             │
│     │  POST /api/v1/auth/login  │                             │             │
│     │  ─────────────────────────▶                             │             │
│     │  {email, password}        │                             │             │
│     │                           │  ① Find user by email       │             │
│     │                           │  ────────────────────────────▶            │
│     │                           │  ◀────────────────────────────            │
│     │                           │                             │             │
│     │                           │  ② Verify password          │             │
│     │                           │  ③ Check account active     │             │
│     │                           │                             │             │
│     │                           │  ④ Generate Access Token    │             │
│     │                           │     (JWT, 15 min expiry)    │             │
│     │                           │                             │             │
│     │                           │  ⑤ Generate Refresh Token   │             │
│     │                           │     (7 days expiry)         │             │
│     │                           │  ────────────────────────────▶            │
│     │                           │     Store refresh token     │             │
│     │                           │  ◀────────────────────────────            │
│     │                           │                             │             │
│     │                           │  ⑥ Update last_login_at     │             │
│     │  ◀─────────────────────────                             │             │
│     │  200 OK                   │                             │             │
│     │  {                        │                             │             │
│     │    accessToken,           │                             │             │
│     │    refreshToken,          │                             │             │
│     │    tokenType: "Bearer",   │                             │             │
│     │    expiresIn: 900,        │                             │             │
│     │    user: {...}            │                             │             │
│     │  }                        │                             │             │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      3. TOKEN REFRESH                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│   Client                      Server                       Database         │
│     │                           │                             │             │
│     │  POST /api/v1/auth/refresh│                             │             │
│     │  ─────────────────────────▶                             │             │
│     │  {refreshToken}           │                             │             │
│     │                           │  ① Find token in DB         │             │
│     │                           │  ────────────────────────────▶            │
│     │                           │  ◀────────────────────────────            │
│     │                           │                             │             │
│     │                           │  ② Validate not expired     │             │
│     │                           │  ③ Validate not revoked     │             │
│     │                           │                             │             │
│     │                           │  ④ Revoke old refresh token │             │
│     │                           │  ────────────────────────────▶            │
│     │                           │                             │             │
│     │                           │  ⑤ Generate new tokens      │             │
│     │                           │  ────────────────────────────▶            │
│     │                           │     Store new refresh token │             │
│     │                           │  ◀────────────────────────────            │
│     │  ◀─────────────────────────                             │             │
│     │  200 OK                   │                             │             │
│     │  {accessToken, refreshToken}                            │             │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      4. PROTECTED REQUEST                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│   Client                   JwtFilter              Controller  Service       │
│     │                         │                       │          │          │
│     │  GET /api/v1/users/me   │                       │          │          │
│     │  Authorization: Bearer <token>                  │          │          │
│     │  ─────────────────────▶ │                       │          │          │
│     │                         │                       │          │          │
│     │                         │ ① Extract token       │          │          │
│     │                         │ ② Validate signature  │          │          │
│     │                         │ ③ Check expiration    │          │          │
│     │                         │ ④ Load user details   │          │          │
│     │                         │ ⑤ Set SecurityContext │          │          │
│     │                         │                       │          │          │
│     │                         │ ─────────────────────▶│          │          │
│     │                         │                       │ ────────▶│          │
│     │                         │                       │ ◀────────│          │
│     │                         │ ◀─────────────────────│          │          │
│     │  ◀──────────────────────│                       │          │          │
│     │  200 OK {user data}     │                       │          │          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### JWT Token Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         JWT TOKEN DESIGN                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      ACCESS TOKEN                                    │    │
│  │                      (Short-lived: 15 minutes)                       │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  HEADER:                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ {                                                                    │   │
│  │   "alg": "HS512",           // HMAC-SHA512 algorithm                │   │
│  │   "typ": "JWT"                                                       │   │
│  │ }                                                                    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  PAYLOAD:                                                                   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ {                                                                    │   │
│  │   "sub": "550e8400-e29b-41d4-a716-446655440000",  // User UUID      │   │
│  │   "email": "john@example.com",                     // User email    │   │
│  │   "role": "ROLE_CUSTOMER",                         // Primary role  │   │
│  │   "firstName": "John",                             // Display name  │   │
│  │   "iat": 1699900000,                               // Issued at     │   │
│  │   "exp": 1699900900,                               // Expires (+15m)│   │
│  │   "iss": "foalrider-api"                           // Issuer        │   │
│  │ }                                                                    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  SIGNATURE:                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ HMACSHA512(                                                          │   │
│  │   base64UrlEncode(header) + "." + base64UrlEncode(payload),         │   │
│  │   secret                                                             │   │
│  │ )                                                                    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      REFRESH TOKEN                                   │    │
│  │                      (Long-lived: 7 days)                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  • Stored in database (hashed)                                              │
│  • Used only to get new access tokens                                       │
│  • Rotated on each use (old one invalidated)                               │
│  • Can be revoked (logout, security breach)                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Role-Based Access Control

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       RBAC MATRIX                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ROLE          │ PERMISSIONS                                                │
│  ──────────────┼────────────────────────────────────────────────────────────│
│                │                                                            │
│  CUSTOMER      │ • View products, categories, brands                        │
│                │ • Manage own cart and wishlist                             │
│                │ • Place orders and view own order history                  │
│                │ • Manage own addresses                                     │
│                │ • Write reviews for purchased products                     │
│                │ • Update own profile                                       │
│                │                                                            │
│  STAFF         │ • All CUSTOMER permissions                                 │
│                │ • View all orders                                          │
│                │ • Update order status                                      │
│                │ • View customer information                                │
│                │ • Manage inventory stock levels                            │
│                │                                                            │
│  ADMIN         │ • All STAFF permissions                                    │
│                │ • Full CRUD on products, categories, brands                │
│                │ • Manage coupons and discounts                             │
│                │ • View analytics and reports                               │
│                │ • Manage staff users                                       │
│                │ • Approve/reject reviews                                   │
│                │                                                            │
│  SUPER_ADMIN   │ • All ADMIN permissions                                    │
│                │ • Manage admin users                                       │
│                │ • System configuration                                     │
│                │ • View audit logs                                          │
│                │ • Database management operations                           │
│                │                                                            │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                       ENDPOINT ACCESS MATRIX                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ENDPOINT                    │ PUBLIC │ CUSTOMER │ STAFF │ ADMIN │ SUPER   │
│  ────────────────────────────┼────────┼──────────┼───────┼───────┼─────────│
│                                                                             │
│  ◆ AUTH                                                                     │
│  POST /auth/register         │   ✓    │    -     │   -   │   -   │    -    │
│  POST /auth/login            │   ✓    │    -     │   -   │   -   │    -    │
│  POST /auth/refresh          │   ✓    │    -     │   -   │   -   │    -    │
│  POST /auth/logout           │   -    │    ✓     │   ✓   │   ✓   │    ✓    │
│                                                                             │
│  ◆ PRODUCTS                                                                 │
│  GET /products               │   ✓    │    ✓     │   ✓   │   ✓   │    ✓    │
│  GET /products/{slug}        │   ✓    │    ✓     │   ✓   │   ✓   │    ✓    │
│  POST /admin/products        │   -    │    -     │   -   │   ✓   │    ✓    │
│  PUT /admin/products/{id}    │   -    │    -     │   -   │   ✓   │    ✓    │
│  DELETE /admin/products/{id} │   -    │    -     │   -   │   ✓   │    ✓    │
│                                                                             │
│  ◆ ORDERS                                                                   │
│  GET /orders                 │   -    │    ✓     │   ✓   │   ✓   │    ✓    │
│  POST /orders                │   -    │    ✓     │   -   │   -   │    -    │
│  GET /admin/orders           │   -    │    -     │   ✓   │   ✓   │    ✓    │
│  PUT /admin/orders/{id}      │   -    │    -     │   ✓   │   ✓   │    ✓    │
│                                                                             │
│  ◆ USERS                                                                    │
│  GET /users/me               │   -    │    ✓     │   ✓   │   ✓   │    ✓    │
│  PUT /users/me               │   -    │    ✓     │   ✓   │   ✓   │    ✓    │
│  GET /admin/users            │   -    │    -     │   -   │   ✓   │    ✓    │
│  PUT /admin/users/{id}/role  │   -    │    -     │   -   │   -   │    ✓    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📡 API Design

### API Versioning & Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API STRUCTURE                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  BASE URL: /api/v1                                                          │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      PUBLIC ENDPOINTS                                │    │
│  │                      (No authentication required)                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  /api/v1/auth                                                               │
│  ├── POST   /register              Register new customer                    │
│  ├── POST   /login                 Authenticate user                        │
│  ├── POST   /refresh               Refresh access token                     │
│  ├── POST   /forgot-password       Request password reset                   │
│  ├── POST   /reset-password        Reset password with token                │
│  └── GET    /verify-email          Verify email address                     │
│                                                                             │
│  /api/v1/products                                                           │
│  ├── GET    /                      List products (paginated, filterable)    │
│  ├── GET    /{slug}                Get product details                      │
│  ├── GET    /featured              Get featured products                    │
│  ├── GET    /new-arrivals          Get new arrivals                         │
│  ├── GET    /on-sale               Get sale products                        │
│  └── GET    /{slug}/reviews        Get product reviews                      │
│                                                                             │
│  /api/v1/categories                                                         │
│  ├── GET    /                      List all categories (tree)               │
│  ├── GET    /{slug}                Get category details                     │
│  └── GET    /{slug}/products       Get products in category                 │
│                                                                             │
│  /api/v1/brands                                                             │
│  ├── GET    /                      List all brands                          │
│  └── GET    /{slug}/products       Get products by brand                    │
│                                                                             │
│  /api/v1/search                                                             │
│  └── GET    /                      Search products                          │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      CUSTOMER ENDPOINTS                              │    │
│  │                      (ROLE_CUSTOMER required)                        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  /api/v1/users                                                              │
│  ├── GET    /me                    Get current user profile                 │
│  ├── PUT    /me                    Update profile                           │
│  ├── PUT    /me/password           Change password                          │
│  └── DELETE /me                    Delete account                           │
│                                                                             │
│  /api/v1/addresses                                                          │
│  ├── GET    /                      List user's addresses                    │
│  ├── POST   /                      Add new address                          │
│  ├── PUT    /{id}                  Update address                           │
│  ├── DELETE /{id}                  Delete address                           │
│  └── PUT    /{id}/default          Set as default                           │
│                                                                             │
│  /api/v1/cart                                                               │
│  ├── GET    /                      Get current cart                         │
│  ├── POST   /items                 Add item to cart                         │
│  ├── PUT    /items/{id}            Update cart item quantity                │
│  ├── DELETE /items/{id}            Remove item from cart                    │
│  ├── DELETE /                      Clear cart                               │
│  └── POST   /apply-coupon          Apply coupon code                        │
│                                                                             │
│  /api/v1/wishlist                                                           │
│  ├── GET    /                      Get wishlist                             │
│  ├── POST   /{productId}           Add to wishlist                          │
│  └── DELETE /{productId}           Remove from wishlist                     │
│                                                                             │
│  /api/v1/orders                                                             │
│  ├── GET    /                      Get order history                        │
│  ├── POST   /                      Create order (checkout)                  │
│  ├── GET    /{orderNumber}         Get order details                        │
│  └── POST   /{orderNumber}/cancel  Cancel order                             │
│                                                                             │
│  /api/v1/reviews                                                            │
│  ├── POST   /                      Submit review                            │
│  ├── PUT    /{id}                  Update own review                        │
│  └── DELETE /{id}                  Delete own review                        │
│                                                                             │
│  /api/v1/payments                                                           │
│  ├── POST   /create-intent         Create Stripe payment intent             │
│  └── POST   /confirm               Confirm payment                          │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      ADMIN ENDPOINTS                                 │    │
│  │                      (ROLE_ADMIN required)                           │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  /api/v1/admin/products                                                     │
│  ├── GET    /                      List all products (with filters)         │
│  ├── POST   /                      Create product                           │
│  ├── GET    /{id}                  Get product (admin view)                 │
│  ├── PUT    /{id}                  Update product                           │
│  ├── DELETE /{id}                  Delete product                           │
│  ├── POST   /{id}/variants         Add variant                              │
│  ├── PUT    /{id}/variants/{vid}   Update variant                           │
│  ├── DELETE /{id}/variants/{vid}   Delete variant                           │
│  ├── POST   /{id}/images           Upload images                            │
│  └── DELETE /{id}/images/{imgId}   Delete image                             │
│                                                                             │
│  /api/v1/admin/categories                                                   │
│  ├── POST   /                      Create category                          │
│  ├── PUT    /{id}                  Update category                          │
│  ├── DELETE /{id}                  Delete category                          │
│  └── PUT    /{id}/sort             Reorder categories                       │
│                                                                             │
│  /api/v1/admin/brands                                                       │
│  ├── POST   /                      Create brand                             │
│  ├── PUT    /{id}                  Update brand                             │
│  └── DELETE /{id}                  Delete brand                             │
│                                                                             │
│  /api/v1/admin/orders                                                       │
│  ├── GET    /                      List all orders                          │
│  ├── GET    /{id}                  Get order details                        │
│  ├── PUT    /{id}/status           Update order status                      │
│  └── POST   /{id}/refund           Process refund                           │
│                                                                             │
│  /api/v1/admin/users                                                        │
│  ├── GET    /                      List users                               │
│  ├── GET    /{id}                  Get user details                         │
│  ├── PUT    /{id}/status           Activate/deactivate user                 │
│  └── PUT    /{id}/role             Change user role (SUPER_ADMIN only)      │
│                                                                             │
│  /api/v1/admin/coupons                                                      │
│  ├── GET    /                      List coupons                             │
│  ├── POST   /                      Create coupon                            │
│  ├── PUT    /{id}                  Update coupon                            │
│  └── DELETE /{id}                  Delete coupon                            │
│                                                                             │
│  /api/v1/admin/reviews                                                      │
│  ├── GET    /                      List pending reviews                     │
│  ├── PUT    /{id}/approve          Approve review                           │
│  └── DELETE /{id}                  Delete review                            │
│                                                                             │
│  /api/v1/admin/dashboard                                                    │
│  ├── GET    /stats                 Get dashboard statistics                 │
│  ├── GET    /sales                 Get sales data                           │
│  └── GET    /inventory             Get inventory alerts                     │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      WEBHOOK ENDPOINTS                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  /api/v1/webhooks                                                           │
│  └── POST   /stripe                Stripe webhook handler                   │
│
```
