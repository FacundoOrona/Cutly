
# Repository Guidelines — Cutly

## 1. Project Overview

Cutly is a REST API for hair salon management, built with Java 17,
Spring Boot, Maven, and MySQL.

Before implementing changes, inspect the existing codebase,
identify the relevant business domain, and follow established
architectural patterns and conventions.

Do not assume that a feature, dependency, configuration, or
architectural component exists without verifying it in the code.

## 2. Project Structure & Module Organization

The backend is organized by business domain under:

`src/main/java/com/dev/cutly`

Existing domains include `auth`, `usuario`, `negocio`,
`turno`, `pago`, `servicio`, `owner`, and `admin`.

- Place new code in the closest existing domain package.
- Follow the existing separation of controllers, services,
  repositories, entities, DTOs, and enums.
- Keep controllers focused on HTTP handling and request/response
  mapping.
- Keep business rules in the appropriate service layer.
- Use repositories for persistence and database access.
- Keep DTOs at API boundaries instead of exposing entities
  directly unless the existing API explicitly requires it.
- Avoid duplicating business logic across domains.
- Avoid circular dependencies and unnecessary coupling.
- Follow existing package and class naming conventions.
- Place tests under the matching package path in
  `src/test/java`.
- Store application configuration in
  `src/main/resources/application.properties`, following
  existing configuration conventions.

Do not introduce a new architectural pattern or reorganize
unrelated packages without a clear technical justification.

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

Use the wrapper instead of requiring a separately installed
Maven version.

Local execution expects a MySQL database named `cutly_db`.
Configure database credentials and JWT settings through the
project's supported environment variables or local configuration.

Relevant settings include:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`

Verify the actual property mappings before changing
configuration or assuming a variable is already supported.

Never expose credentials, tokens, or secret values in logs,
test reports, commits, or generated documentation.

## 4. Coding Style & Naming Conventions

- Use Java 17 and four-space indentation.
- Follow standard Spring Boot and Java conventions.
- Use `PascalCase` for classes and `camelCase` for methods
  and fields.
- Use `UPPER_SNAKE_CASE` for constants.
- Name types according to their responsibilities, for example:
  `OwnerTurnosController`, `TurnoRepository`, and
  `CrearServicioRequestDto`.
- Prefer constructor injection.
- Follow existing Lombok usage instead of introducing
  a different style without justification.
- Keep imports clean and match the surrounding code.
- Prefer readable, cohesive methods and descriptive names.
- Avoid unnecessary abstractions, premature optimization,
  duplicated logic, and overly complex implementations.
- Handle errors consistently with the existing API conventions.
- Validate incoming data at the appropriate application boundary.
- Preserve backward compatibility unless a breaking change
  is explicitly required.

No separate formatter or linter is currently configured.
Do not introduce one without a clear reason.

## 5. Security & Authorization

Security is a core requirement for Cutly.

- Inspect the existing Spring Security and JWT implementation
  before modifying authentication or authorization.
- Respect the existing authentication flow and role model.
- Verify that protected endpoints require authentication
  and the appropriate permissions.
- Enforce authorization on the server; never rely solely
  on frontend restrictions.
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
- Avoid leaking internal exceptions, credentials, tokens,
  or sensitive user data in API responses and logs.
- Review security implications whenever adding or modifying
  controllers, services, repositories, or DTOs.

When changing security behavior, include tests for unauthorized
requests, insufficient permissions, and successful authorized
requests where applicable.

## 6. Database & Persistence

- Respect existing entities, relationships, constraints,
  and repository conventions.
- Inspect the effects of entity changes on persistence,
  serialization, and existing API responses.
- Review transaction boundaries when modifying business
  operations that involve multiple database changes.
- Prevent inconsistent state and unintended partial updates.
- Use appropriate transaction management for operations
  that require atomicity.
- Review concurrency risks when multiple requests can modify
  the same resource.
- Avoid unnecessary database queries and inefficient
  relationship loading.
- Never perform destructive database operations or delete
  user data without explicit authorization.
- Do not introduce schema migrations or change database
  initialization behavior without assessing the project's
  current migration and schema-management strategy.
- Never assume a database change is safe for existing data;
  explain relevant compatibility and migration requirements.

## 7. Business Logic & API Contracts

- Inspect existing business rules before implementing new ones.
- Keep validation and business invariants in the appropriate
  backend layer.
- Handle invalid states and expected failure cases explicitly.
- Preserve consistent HTTP status codes and error responses.
- Avoid breaking existing endpoints, request DTOs, response DTOs,
  and API contracts without an explicit requirement.
- When a contract change is necessary, identify affected
  consumers and document the impact.
- For appointment scheduling and other concurrent operations,
  consider overlapping requests, availability validation,
  transaction boundaries, and database locking where relevant.
- Do not duplicate existing checks or introduce conflicting
  validation rules without understanding their purpose.

## 8. Testing Guidelines

Tests use JUnit 5 and Spring Boot test support.

- Name test classes with the `Tests` suffix, following the
  existing test suite.
- Name test methods after the behavior they verify.
- Add unit or integration tests for new business rules,
  security behavior, persistence changes, and API endpoints.
- Cover successful scenarios, validation failures,
  relevant edge cases, and authorization failures.
- Include regression tests for bugs when practical.
- Mock external dependencies only when appropriate for the
  test's purpose.
- Do not change or remove assertions merely to make tests pass.
- Do not weaken production code or security controls solely
  to accommodate tests.
- Keep tests deterministic and independent of execution order.
- Avoid relying on production data or real external services
  unless explicitly required.

During development, run the narrowest relevant test first.
Before completing a substantial change, run the full test suite:

```powershell
.\mvnw.cmd test
```

Run additional build or packaging commands when relevant.

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

## 9. Agent Workflow & Change Management

Before implementing a task:

1. Inspect the relevant files, existing implementations,
   tests, and configuration.
2. Identify applicable instructions and architectural constraints.
3. Determine the smallest reasonable change that fulfills
   the requested behavior.
4. For substantial or cross-domain changes, present a brief
   implementation plan before editing.

During implementation:

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

After implementation:

1. Review the diff for unintended changes.
2. Verify consistency with the existing architecture.
3. Run relevant tests and builds when possible.
4. Report the files changed and the main implementation decisions.
5. Report the exact validation commands executed and their results.
6. List unresolved issues, limitations, and any required
   configuration or database changes.

For simple, low-risk tasks, avoid unnecessary planning overhead.

## 10. Commit & Pull Request Guidelines

- Write short, imperative commit subjects that identify
  the change, such as `Add owner payment endpoint`.
- Keep commits focused and avoid mixing unrelated changes.
- Do not create commits unless explicitly requested.
- Pull requests should explain the behavior changed, include
  validation commands and results, link the related issue
  when applicable, and identify configuration or database
  migration requirements.
- Include request/response examples when API contracts change.
- Include screenshots only when they help explain a relevant
  user-facing change.

## 11. Configuration & Secrets

- Never commit credentials, production secrets, private keys,
  or real tokens.
- Treat development-only defaults as local configuration,
  not production-ready secrets.
- Use the configuration mechanisms already supported by
  the project.
- Do not introduce permanent machine-wide environment variables
  when a project-local configuration mechanism is appropriate.
- Do not print secret values when diagnosing configuration issues.
- Document new required configuration keys without including
  sensitive values.
- Verify that configuration changes remain compatible with
  existing development and deployment environments.
