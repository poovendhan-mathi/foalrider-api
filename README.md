# 🐎 FoalRider API

A production-grade Spring Boot backend for a clothing e-commerce platform.

## 🛠 Technology Stack

- **Framework:** Spring Boot 3.2.x
- **Language:** Java 21 LTS
- **Database:** PostgreSQL (Supabase)
- **Cache:** Redis
- **ORM:** Spring Data JPA / Hibernate
- **Migrations:** Flyway
- **Security:** Spring Security + JWT
- **Payments:** Stripe
- **API Docs:** SpringDoc OpenAPI (Swagger)

## 📁 Project Structure

```
foalrider-api/
├── src/main/java/com/foalrider/
│   ├── FoalRiderApplication.java
│   ├── config/              # Configuration classes
│   ├── security/            # Security & JWT
│   ├── modules/             # Feature modules
│   │   ├── auth/
│   │   ├── user/
│   │   ├── product/
│   │   ├── category/
│   │   ├── brand/
│   │   ├── cart/
│   │   ├── order/
│   │   ├── payment/
│   │   └── ...
│   ├── shared/              # Shared components
│   └── infrastructure/      # Infrastructure concerns
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/        # Flyway migrations
└── pom.xml
```

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+ (optional for development)

### Setup

1. **Clone the repository:**

   ```bash
   cd BackendSpring/foalrider-api
   ```

2. **Configure environment:**

   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Create database:**

   ```sql
   CREATE DATABASE foalrider_dev;
   ```

4. **Run the application:**

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

5. **Access Swagger UI:**
   ```
   http://localhost:8080/api/v1/swagger-ui.html
   ```

## 📚 API Documentation

Once the application is running, access the API documentation at:

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/api-docs`

## 🔐 Authentication

The API uses JWT (JSON Web Tokens) for authentication:

1. Register or login to get tokens
2. Include the access token in the `Authorization` header:
   ```
   Authorization: Bearer <access_token>
   ```
3. Use refresh token to get new access tokens when expired

## 🧪 Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 📝 Environment Variables

| Variable            | Description                       | Default                                      |
| ------------------- | --------------------------------- | -------------------------------------------- |
| `DATABASE_URL`      | PostgreSQL connection URL         | `jdbc:postgresql://localhost:5432/foalrider` |
| `DATABASE_USERNAME` | Database username                 | `postgres`                                   |
| `DATABASE_PASSWORD` | Database password                 | -                                            |
| `JWT_SECRET`        | JWT signing secret (min 512 bits) | -                                            |
| `STRIPE_API_KEY`    | Stripe secret key                 | -                                            |
| `REDIS_HOST`        | Redis host                        | `localhost`                                  |
| `REDIS_PORT`        | Redis port                        | `6379`                                       |

## 📄 License

MIT License

## 👥 Team

FoalRider Development Team
