# Movie Ticket Agent Backend Skeleton

Minimal Spring Boot backend skeleton for the movie-ticket project.

## Modules

```text
server -> pojo -> common
```

- `common`: shared module boundary.
- `pojo`: reserved packages for future `entity`, `dto`, and `vo` classes.
- `server`: Spring Boot entrypoint and MyBatis-Plus configuration placeholder.

## Requirements

- JDK 21
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Run

The skeleton intentionally excludes datasource and MyBatis auto-configuration, so it starts without a database:

```powershell
$env:JAVA_HOME='C:\Path\To\JDK-21'
.\mvnw.cmd -pl server -am spring-boot:run
```

The server listens on `http://localhost:8080` and currently exposes no business endpoints.

## Verify

```powershell
$env:JAVA_HOME='C:\Path\To\JDK-21'
.\mvnw.cmd clean verify
```

Database connection, entities, mappers, controllers, services, migrations, seed data, authentication, and Agent integration will be added in later iterations.
