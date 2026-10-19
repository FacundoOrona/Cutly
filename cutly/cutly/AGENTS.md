
# Repository Guidelines — Cutly

## 1. Project Overview

Cutly is a REST API for hair salon management, built with Java 17,
Spring Boot, Maven, and MySQL.

Before implementing changes, inspect the relevant parts of the
codebase, identify the affected business domain, and follow the
existing architectural patterns and conventions.

Do not assume that a feature, dependency, configuration, or
architectural component exists without verifying it in the code.


## 2. Project Structure & Module Organization

The backend is organized by business domain under:

`src/main/java/com/dev/cutly`

Existing domains include:

- `auth`
- `usuario`
- `cliente`
- `empleado`
- `negocio`
- `turno`
- `pago`
- `servicio`
- `suscripcion`
- `auditoria`
- `review`
- `favorito`
- `notificacion`
- `owner`
- `admin`

This list reflects the currently identified domains and may evolve
as the project grows.

- Place new code in the closest existing domain package.
- Follow the existing separation of controllers, services,
  repositories, entities, DTOs, and enums where applicable.
- Not every business domain necessarily contains every layer.
  Inspect the target package before introducing new classes.
- Keep controllers focused on HTTP handling and request/response
  mapping.
- Keep business rules in the appropriate service layer.
- Use repositories for persistence and database access.
- Keep DTOs at API boundaries and avoid exposing entities directly
  unless the existing API explicitly requires it.
- Avoid duplicating business logic across domains.
- Avoid circular dependencies and unnecessary coupling.
- Follow existing package and class naming conventions.
- Place tests under the matching package path in `src/test/java`.
- Store application configuration in
  `src/main/resources/application.properties`, following the
  existing configuration conventions.

Do not reorganize unrelated packages or introduce a new
architectural pattern without a clear technical justification.

## 3. Build, Test & Development Commands

Use the Maven wrapper from the repository root.

On Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
.\mvnw.cmd package
```

On Unix-like systems:

```bash
./mvnw test
./mvnw spring-boot:run
./mvnw package
```

Use the Maven wrapper instead of requiring a separately installed
Maven version.

Local execution expects a MySQL database, normally named
`cutly_db`, unless the configured database URL specifies otherwise.

Before executing commands, consider whether the required database,
configuration, and other dependencies are available.

Run the narrowest relevant test during development, then run the
full test suite for substantial changes.

Do not claim that a command succeeded unless it was actually
executed and its result was verified.

## 4. Coding Style & Naming Conventions

- Use Java 17 and four-space indentation.
- Follow standard Java and Spring Boot conventions.
- Use `PascalCase` for classes and `camelCase` for methods
  and fields.
- Use `UPPER_SNAKE_CASE` for constants.
- Name types according to their responsibilities, for example:
  `OwnerTurnosController`, `TurnoRepository`, and
  `CrearServicioRequestDto`.
- Prefer constructor injection.
- Follow existing Lombok usage instead of introducing a
  different style without justification.
- Keep imports clean and match the surrounding code.
- Prefer readable, cohesive methods and descriptive names.
- Avoid unnecessary abstractions, premature optimization,
  duplicated logic, and overly complex implementations.
- Handle errors consistently with the existing API conventions.
- Validate incoming data at the appropriate application boundary.
- Preserve backward compatibility unless a breaking change is
  explicitly required.

No separate formatter or linter is currently configured.
Do not introduce one without a clear reason.

## 5. Architecture & Separation of Responsibilities

- Inspect existing implementations before introducing new classes
  or abstractions.
- Keep HTTP handling, business logic, and persistence concerns
  separated according to the existing architecture.
- Reuse existing services, repositories, DTOs, and utilities
  where appropriate.
- Avoid placing business rules directly in controllers.
- Avoid exposing persistence details unnecessarily through API
  responses.
- Keep dependencies between business domains explicit and minimal.
- Do not duplicate existing functionality without understanding
  why the current implementation is insufficient.
- Do not refactor unrelated code as part of a feature unless
  the refactoring is necessary and justified.

Prefer the smallest maintainable change that satisfies the
requested behavior and fits the existing architecture.

## 6. Security & Authorization

Security is a core requirement for Cutly.

- Inspect the existing Spring Security and JWT implementation
  before modifying authentication or authorization.
- Respect the existing authentication flow and role model.
- Verify that protected endpoints require authentication and
  the appropriate permissions.
- Enforce authorization on the server; never rely solely on
  frontend restrictions.
- Validate ownership and business boundaries when accessing
  resources belonging to a user or business.
- Do not trust user IDs, roles, business IDs, prices, or other
  sensitive values supplied by clients without verification.
- Never expose sensitive endpoints publicly without explicit
  authorization and a documented reason.
- Never hardcode credentials, production secrets, JWT signing
  keys, or other sensitive configuration.
- Do not weaken authentication, authorization, or validation
  to make tests pass.
- Avoid leaking internal exceptions, credentials, tokens, or
  sensitive user data in API responses and logs.
- Review security implications whenever adding or modifying
  controllers, services, repositories, or DTOs.
- Preserve existing role and permission checks unless a change
  is explicitly required and its consequences are understood.

When changing security behavior, include tests for unauthorized
requests, insufficient permissions, and successful authorized
requests where applicable.

## 7. Database & Persistence Strategy

The current database configuration is defined in
`src/main/resources/application.properties`.

The identified settings are:

- `spring.jpa.hibernate.ddl-auto=update`
- `spring.flyway.enabled=false`

Flyway dependencies exist in `pom.xml`, but their presence does
not mean that Flyway migrations are currently enabled.

No additional versioned Spring profiles or configuration files
were identified during the repository review.

Treat Hibernate schema update as the current configured
schema-management behavior and Flyway as disabled, unless the
configuration or runtime setup demonstrates otherwise.

Before changing the persistence strategy:

1. Inspect the current configuration and relevant dependencies.
2. Check whether runtime overrides affect the effective settings.
3. Assess the impact on existing databases and stored data.
4. Explain relevant compatibility and migration concerns.

Do not enable Flyway, create migrations, remove dependencies, or
change Hibernate DDL settings as part of an unrelated task.

Additional database configuration may exist outside versioned
repository files. Do not assume that repository configuration
alone describes every runtime setting.

### Persistence rules

- Respect existing entities, relationships, constraints, and
  repository conventions.
- Inspect the effects of entity changes on persistence,
  serialization, and existing API responses.
- Review transaction boundaries when modifying operations that
  involve multiple database changes.
- Use appropriate transaction management for operations that
  require atomicity.
- Consider concurrency risks when multiple requests can modify
  the same resource.
- Avoid unnecessary database queries and inefficient relationship
  loading.
- Never perform destructive database operations or delete user
  data without explicit authorization.
- Do not assume a schema change is safe for existing data.
- Preserve data integrity when changing entity relationships,
  constraints, or business rules.

For appointment scheduling and other concurrent operations,
consider overlapping requests, availability validation,
transaction boundaries, and database locking where relevant.


## 8. Environment Configuration & Secrets

The project documents the following application configuration
variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`

The project also uses five required variables for initial
super-administrator provisioning:

- `SUPER_ADMIN_NOMBRE`
- `SUPER_ADMIN_APELLIDO`
- `SUPER_ADMIN_DNI`
- `SUPER_ADMIN_EMAIL`
- `SUPER_ADMIN_PASSWORD`

The names of these five variables are confirmed by
`.env.example`, `application.properties`, and
`SuperAdminInitializer`.

### Configuration defaults

According to the current `application.properties`:

- `DB_URL` has a fallback URL pointing to the local MySQL
  database `cutly_db`.
- `JWT_EXPIRATION_MS` defaults to `7200000` milliseconds
  (two hours).
- `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` do not have
  fallback values defined in `application.properties`.

Do not assume that `.env.example` supplies runtime values.
It documents example configuration; actual loading depends
on the local development or deployment environment.

Before modifying configuration, inspect `application.properties`
and `.env.example` to verify exact property names, defaults,
and environment-variable mappings.

A local `.env` file exists at the repository root.

- Never read, print, copy, or disclose its secret values merely
  to understand the project's configuration.
- Prefer `.env.example` and configuration metadata to determine
  expected variable names and formats.
- Do not assume `.env` is loaded automatically by Spring Boot.
- Verify the local development environment's loading mechanism
  when troubleshooting configuration.
- Do not commit credentials, tokens, production secrets, or
  sensitive local configuration.
- Never print secret values when diagnosing configuration issues.
- Document new required configuration keys without including
  sensitive values.
- Do not introduce permanent machine-wide environment variables
  when an appropriate project-local configuration mechanism
  is already available.

Environment variables, JVM arguments, command-line arguments,
and external runtime configuration may override application
defaults. Do not assume repository files describe every runtime
override.

Before diagnosing missing configuration, inspect the relevant
property mapping and runtime configuration mechanism without
exposing secret values.


## 9. Initial Super-Administrator Provisioning

`SuperAdminInitializer` runs during application startup and
provisions the initial super-administrator account.

The following five variables are required:

- `SUPER_ADMIN_NOMBRE`
- `SUPER_ADMIN_APELLIDO`
- `SUPER_ADMIN_DNI`
- `SUPER_ADMIN_EMAIL`
- `SUPER_ADMIN_PASSWORD`

The initializer fails application startup if any required value
is missing or blank.

Its current behavior is:

1. If a user with the configured email already exists, validate
   that the user has the expected `SUPER_ADMIN` role, that the
   required existing fields are not blank, and that the DNI
   matches the configured DNI.
2. If no user with that email exists, verify that the DNI is not
   already in use, then create an active super-administrator
   account with the `SUPER_ADMIN` role and an encoded password.

The initializer performs database reads and may insert a user.
It does not directly change the database connection configuration,
but its execution depends on successful database access.

Before modifying this initializer or its configuration:

- Inspect its implementation and associated tests.
- Preserve its validation and security guarantees.
- Consider the effect on application startup and existing data.
- Never log or expose the configured password.
- Do not bypass required configuration to make the application
  start in an incorrectly configured environment.
- Do not create duplicate users or silently change existing
  administrator roles or identity data.
- Add or update tests for changes to its behavior.
- Consider the impact of failures and existing database records.

When an account with the configured email already exists,
preserve all existing checks for the expected role, required
nonblank fields, and matching DNI.

When creating a new account, preserve the DNI uniqueness check,
the `SUPER_ADMIN` role, the active account state, and password
encoding. Do not log or expose the configured password.

When troubleshooting startup failures, distinguish between
missing configuration, database connectivity problems, schema
issues, and initializer validation errors.

Never report that startup was verified unless the application
was actually started and the result was observed.

## 10. Business Logic & API Contracts

- Inspect existing business rules before implementing new ones.
- Keep validation and business invariants in the appropriate
  backend layer.
- Handle invalid states and expected failure cases explicitly.
- Preserve consistent HTTP status codes and error responses.
- Avoid breaking existing endpoints, request DTOs, response DTOs,
  and API contracts without an explicit requirement.
- When a contract change is necessary, identify affected
  consumers and document the impact.
- Verify ownership and permissions before performing operations
  on user or business resources.
- Do not duplicate existing checks or introduce conflicting
  validation rules without understanding their purpose.
- Ensure operations that modify multiple related records preserve
  data consistency.

When modifying appointment scheduling, payments, subscriptions,
or other sensitive business operations, inspect the related
services, repositories, entities, and tests before implementing
changes.


## 11. Testing Guidelines

Tests use JUnit 5 and Spring Boot test support.

The existing test suite includes classes following the `*Tests`
naming convention and a class named `CutlyApplicationTests`.

The current suite includes:

- `CutlyApplicationTests`, which uses `@SpringBootTest`.
- `ClienteTurnoServiceTests`, which contains tests for the
  client appointment service.

Follow the conventions of the relevant test package and existing
test class. Do not assume that all existing tests use a uniform
suffix.

### Test coverage expectations

- Add unit or integration tests for new business rules, security
  behavior, persistence changes, and API endpoints.
- Cover successful scenarios, validation failures, relevant edge
  cases, and authorization failures.
- Add regression tests for bugs when practical.
- Keep tests deterministic and independent of execution order.
- Avoid relying on production data or real external services
  unless explicitly required.
- Mock external dependencies only when appropriate for the
  test's purpose.
- Do not change or remove assertions merely to make tests pass.
- Do not weaken production code or security controls solely to
  accommodate tests.
- Verify that tests exercise the intended behavior instead of
  merely checking that the application context starts.

### Test execution

On Windows PowerShell, run the narrowest relevant test first,
then run the full suite for substantial changes.

Run the complete test suite:

```powershell
.\mvnw.cmd test
```

Run a specific test class:

```powershell
.\mvnw.cmd "-Dtest=ClienteTurnoServiceTests" test
```

Run a specific test method when supported by the test provider
and naming conventions:

```powershell
.\mvnw.cmd "-Dtest=ClienteTurnoServiceTests#nombreDelMetodo" test
```

Replace `nombreDelMetodo` with the actual test method name.

Before selecting a test, inspect the existing test classes and
identify which tests cover the modified behavior.

If a targeted test passes, do not assume the complete suite
passes. Run the full suite when appropriate.

If a test fails:

1. Inspect the failure and identify its cause.
2. Fix issues related to the requested change.
3. Rerun the relevant tests.
4. Report unresolved failures and their causes honestly.

Never claim that tests or builds passed unless they were
actually executed and their results were verified.

If tests cannot be executed because of missing infrastructure,
database configuration, permissions, or other limitations,
explain the limitation and distinguish verified results from
untested changes.

No numeric coverage threshold is currently configured.

## 12. Agent Workflow & Change Management

### Before implementation

1. Inspect the relevant files, existing implementations, tests,
   and configuration.
2. Identify applicable instructions and architectural constraints.
3. Determine the smallest reasonable change that fulfills the
   requested behavior.
4. For substantial or cross-domain changes, present a brief
   implementation plan before editing.

### During implementation

- Keep changes focused on the requested task.
- Avoid unrelated refactoring and unnecessary file changes.
- Reuse existing utilities and established patterns where suitable.
- Do not overwrite or revert unrelated user changes.
- Do not modify secrets, environment files, or local configuration
  unless explicitly required and authorized.
- Do not add dependencies without explaining why they are needed.
- Do not invent test results, API behavior, or project capabilities.
- If a requirement is ambiguous and materially affects security,
  data integrity, or API compatibility, ask for clarification
  instead of making a risky assumption.

### Startup and configuration safety

When a change affects application startup, authentication,
database access, or initial account provisioning, inspect the
relevant initialization path before editing.

Do not access local secret files merely to understand their
structure. Prefer `.env.example` and configuration metadata.

When diagnosing startup failures, distinguish between missing
configuration, database connectivity, schema problems, and
initialization validation errors.

Never report that startup was verified unless the application
was actually started and the result was observed.

### After implementation

1. Review the diff for unintended changes.
2. Verify consistency with the existing architecture.
3. Run relevant tests and builds when possible.
4. Report the files changed and the main implementation decisions.
5. Report the exact validation commands executed and their results.
6. List unresolved issues, limitations, and required configuration
   or database changes.

For simple, low-risk tasks, avoid unnecessary planning overhead.

## 13. Commit & Pull Request Guidelines

- Write short, imperative commit subjects that identify the
  change, such as `Add owner payment endpoint`.
- Keep commits focused and avoid mixing unrelated changes.
- Do not create commits unless explicitly requested.
- Pull requests should explain the behavior changed, include
  validation commands and results, link the related issue when
  applicable, and identify configuration or database migration
  requirements.
- Include request/response examples when API contracts change.
- Include screenshots only when they help explain a relevant
  user-facing change.

## 14. Dependencies & Technical Decisions

- Prefer existing project dependencies and utilities when suitable.
- Do not add libraries for functionality already provided by
  the current stack without a clear justification.
- Before adding or upgrading a dependency, evaluate compatibility
  with Java 17, the Spring Boot version, and existing libraries.
- Explain meaningful trade-offs when a decision affects
  architecture, security, performance, or maintainability.
- Do not introduce new frameworks, infrastructure, or architectural
  patterns as incidental changes.
- Keep changes proportionate to the requested task.
