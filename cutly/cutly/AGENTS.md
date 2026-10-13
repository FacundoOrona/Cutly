# Repository Guidelines

## Project Structure & Module Organization

Cutly is a Spring Boot backend organized by business domain under `src/main/java/com/dev/cutly`. Domain packages such as `auth`, `usuario`, `negocio`, `turno`, `pago`, `servicio`, `owner`, and `admin` contain their entities, repositories, services, controllers, DTOs, and enums. Keep new code in the closest existing domain package. Application configuration lives in `src/main/resources/application.properties`; tests belong under the matching package path in `src/test/java`.

## Build, Test, and Development Commands

Use the Maven wrapper from the repository root:

```text
./mvnw test                 # Compile and run all tests
./mvnw spring-boot:run      # Start the application locally
./mvnw package              # Build the deployable JAR
```

On Windows, use `mvnw.cmd` instead. Local execution expects a MySQL database named `cutly_db`; configure `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `JWT_EXPIRATION_MS` through environment variables when defaults are not appropriate.

## Coding Style & Naming Conventions

Use Java 17, four-space indentation, and standard Spring naming. Classes use `PascalCase`; methods and fields use `camelCase`; constants use `UPPER_SNAKE_CASE`. Name types by responsibility, for example `OwnerTurnosController`, `TurnoRepository`, and `CrearServicioRequestDto`. Prefer constructor injection, keep DTOs at API boundaries, and follow the existing Lombok usage rather than introducing a new style. No separate formatter or linter is configured; keep imports clean and match surrounding code.

## Testing Guidelines

Tests use JUnit 5 and Spring Boot test support. Name test classes with the `Tests` suffix and test methods after the behavior they verify, such as `contextLoads`. Run the narrowest relevant test during development, then `./mvnw test` before submitting changes. Add unit or integration coverage for new business rules, security behavior, and controller endpoints; no numeric coverage threshold is currently configured.

## Commit & Pull Request Guidelines

Write short, imperative commit subjects that identify the change, such as `Add owner payment endpoint`. Keep commits focused. Pull requests should explain the behavior changed, include validation commands and results, link the related issue when applicable, and call out configuration or database migration requirements. Include request/response examples or screenshots when an API or user-facing contract changes.

## Security & Configuration Tips

Never commit credentials or production secrets. Use environment variables for database and JWT settings, and treat the development JWT fallback as local-only. Review authorization and validation whenever adding an endpoint.
