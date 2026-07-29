# Movie Ticket Agent Backend

Spring Boot backend for the deterministic movie-ticket business core. The Python/LangChain agent is a separate service and calls these APIs with the user's Bearer token.

## Requirements

- Java 17+
- MySQL 8
- `JAVA_HOME` pointing to the selected JDK

## Start locally

Create an empty database and provide credentials through environment variables:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/movie_ticket_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&createDatabaseIfNotExist=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your-password'
$env:JWT_SECRET='replace-with-a-base64-encoded-32-byte-secret'
$env:DEMO_SEED_ENABLED='true'
$env:DEMO_USER_PASSWORD='choose-a-local-demo-password'
.\mvnw.cmd -pl movie-ticket-app -am spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

Health: `http://localhost:8080/actuator/health`

Demo data is inserted only when `DEMO_SEED_ENABLED=true`. Existing rows with stable demo IDs are not duplicated. The generated accounts are `user@demo.local` and `admin@demo.local`; both use `DEMO_USER_PASSWORD`.

## Implemented APIs

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/token/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/movies`
- `GET /api/v1/movies/{id}`
- `GET /api/v1/cinemas`
- `GET /api/v1/cinemas/{id}/showtimes`
- `GET /api/v1/showtimes/{id}/seats`

Purchase drafts, locking, orders, payments, and tickets currently expose domain entities, mappers, DTO contracts, and service ports only. They intentionally have no controller until their transactional workflows are implemented.

## Tests

```powershell
.\mvnw.cmd -pl movie-ticket-app -am test
```

The test profile uses H2 in MySQL compatibility mode and validates the complete Flyway schema, authentication lifecycle, authorization, query APIs, time-zone conversion, and repeatable seven-day demo seeding.

## Agent boundary

The LangChain agent is a separate Python service. It owns conversation state and passes the user's Bearer token through when calling this Java service. Purchase drafts, seat inventory, orders, payments, and tickets remain owned by Java and MySQL.
