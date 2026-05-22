# Issue 54 Spring Security Tenant Authorization Plan

## Scope

Add `10-multi-tenant/06-spring-security-tenant-authorization-spring-web`, a
Spring MVC + Spring Security + Exposed JDBC workshop module that authorizes a
tenant before selecting the tenant database.

Approved spec draft:

- `docs/superpowers/specs/2026-05-23-issue-54-spring-security-tenant-authorization-design.md`

## Implementation Tasks

### 1. Scaffold the module

Complexity: M

- Copy the structure of `10-multi-tenant/05-database-per-tenant-spring-web`.
- Rename package from `exposed.multitenant.database` to
  `exposed.multitenant.security`.
- Rename the Spring Boot entrypoint to `TenantSecurityApplication.kt`.
- Update `springBoot.mainClass`, README titles, and application metadata.
- Confirm `settings.gradle.kts` auto-registers the module through
  `includeModules("10-multi-tenant", false, false)` and update it only if
  discovery fails.
- Keep inventory domain, service, repository, and seed data comparable with
  module `05`.

### 2. Add Spring Security dependencies

Complexity: S

- Add module dependencies:
  - `org.springframework.boot:spring-boot-starter-security`
  - `org.springframework.boot:spring-boot-starter-oauth2-resource-server`
  - `org.springframework.security:spring-security-test`
- Keep H2-only tenant database dependencies from module `05`.
- Avoid new version catalog entries unless existing repo style requires them;
  current examples use direct Spring Boot starter coordinates.
- Do not add Konsist or ArchUnit. Use focused JUnit source-text architecture
  tests with `java.nio.file.Files.walk` and `readText()` so the example stays
  dependency-light.
- Keep Exposed imports under `org.jetbrains.exposed.v1.*`.

### 3. Implement authentication and tenant authorization

Complexity: L

- Add `config/SecurityConfiguration.kt`.
  - Disable CSRF for this stateless JSON workshop API.
  - Use stateless session management.
  - Permit `/actuator/health`.
  - Require authentication for inventory endpoints.
  - Enable OAuth2 Resource Server JWT with a converter that preserves JWT
    authentication even when tenant claims are absent.
  - Instantiate custom security filters directly inside this configuration;
    do not annotate them with `@Component` and do not expose them as `Filter`
    beans.
  - Register `CredentialConflictFilter` before authentication filters to reject
    requests that include more than one credential source.
  - Register API-key and demo-session filters inside `SecurityFilterChain`
    before bearer-token authentication.
  - Register `TenantAuthorizationFilter` inside `SecurityFilterChain` after
    `AuthorizationFilter`, so `.anyRequest().authenticated()` has already run
    and MVC handlers still receive the authorized `TenantContext`.
  - Disable independent servlet filter auto-registration for all custom
    security filters if they are exposed as beans.
- Add `security/AuthenticatedTenant.kt`.
  - Any `data class` must implement `java.io.Serializable` and define
    `serialVersionUID`.
  - Public API/KDoc text must be English.
- Add `security/DemoJwtDecoder.kt`.
  - Fixed accepted token strings:
    - `demo-acme-token` -> `tenant_id=acme`
    - `demo-globex-token` -> `tenant_id=globex`
    - `demo-no-tenant-token` -> no `tenant_id`
    - `demo-unknown-tenant-token` -> `tenant_id=initech`
    - `demo-acme-upper-token` -> `tenant_id=ACME`
    - `demo-malformed-tenant-token` -> `tenant_id=acme,globex`
    - `demo-non-string-tenant-token` -> numeric `tenant_id`
  - Include English KDoc warning that this decoder is fixed-data workshop code,
    not production token validation.
- Add `security/CredentialConflictFilter.kt`.
  - Counts credential source presence only: bearer authorization, API key, demo
    session.
  - Treat a header as present only when at least one value is non-blank after
    trim.
  - Count `Authorization` only when the scheme is `Bearer` with
    case-insensitive scheme matching.
  - Scan all `Authorization` header values; if any non-blank value is bearer,
    bearer credentials are present.
  - Ignore non-bearer authorization schemes because the example does not
    support Basic, Digest, or form login.
  - Skip `/actuator/health`.
  - Returns `400 CONFLICTING_CREDENTIALS` when more than one source is present.
  - Must not log raw secret-bearing header values.
- Add `security/DemoApiKeyAuthenticationFilter.kt`.
  - `demo-acme-key` -> `acme`
  - `demo-globex-key` -> `globex`
  - Invalid provided API keys fail authentication.
  - Must participate in Spring Security exception translation and must not
    write directly to the servlet response outside the security chain.
  - Plain class, not `@Component`, not a `Filter` bean.
- Add `security/DemoSessionAuthenticationFilter.kt`.
  - `acme-session` -> `acme`
  - `globex-session` -> `globex`
  - Invalid provided session headers fail authentication.
  - Must participate in Spring Security exception translation and must not
    create a persistent server session.
  - Plain class, not `@Component`, not a `Filter` bean.
- Add `security/TenantAuthenticationResolver.kt`.
  - Reads tenant identity from JWT, API-key, or demo-session authentication.
  - Distinguishes missing, malformed, unknown, and resolved states.
  - Normalizes tenant claim values with trim/lowercase, matching
    `TenantId.fromHeaderOrNull`.
- Add `security/TenantAuthorizationFilter.kt`.
  - Skip `/actuator/health`.
  - Resolve authenticated tenant before validating `X-Tenant-ID`; missing,
    malformed, or unknown authenticated tenant returns 403.
  - Validates exactly one `X-Tenant-ID` header.
  - Compares requested and authenticated tenants.
  - Sets `TenantContext` only after authorization succeeds.
  - Clears `TenantContext` in `finally`.
  - Plain class, not `@Component`, not a `Filter` bean.
  - Must not log raw `Authorization`, `X-API-Key`, or `X-Demo-Session` values.
  - Use explicit servlet security anchors:
    - `addFilterBefore(credentialConflictFilter, BearerTokenAuthenticationFilter::class.java)`
    - `addFilterBefore(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter::class.java)`
    - `addFilterBefore(demoSessionAuthenticationFilter, BearerTokenAuthenticationFilter::class.java)`
    - `addFilterAfter(tenantAuthorizationFilter, AuthorizationFilter::class.java)`

### 4. Preserve database-per-tenant routing

Complexity: M

- Keep `TenantDatabaseRegistry`, `TenantTransaction`, `TenantContext`, and
  `TenantId` behavior aligned with module `05`.
- Remove any request-path filter that writes `TenantContext` from a raw header.
- Keep registry validation and datasource lifecycle tests.
- Keep repositories datasource-agnostic; repositories call `TenantTransaction`
  only.

### 5. Add tests

Complexity: L

- Security request tests:
  - valid JWT claim reads matching tenant seed data;
  - JWT/header mismatch returns 403;
  - JWT without tenant claim returns 403;
  - JWT with unknown tenant claim returns 403;
  - JWT with malformed tenant claim returns 403;
  - JWT with non-string tenant claim returns 403;
  - missing authentication returns 401;
  - API key reads matching tenant seed data;
  - invalid API key returns 401;
  - demo session reads matching tenant seed data;
  - demo session/header mismatch returns 403.
- Tenant selector tests:
  - missing header returns 400;
  - blank, duplicate, comma-containing, and too-long headers return 400;
  - unknown header returns 404 for authenticated callers.
  - uppercase tenant selector and uppercase tenant claim normalize to the same
    known tenant.
  - leading/trailing whitespace around tenant selector and claim normalizes to
    the same known tenant.
  - authenticated JWT without tenant claim plus missing `X-Tenant-ID` returns
    403 because authenticated tenant validation runs before selector
    validation.
- Credential conflict tests:
  - bearer `acme` plus API key `globex` returns 400
    `CONFLICTING_CREDENTIALS`;
  - API key plus demo session returns 400 `CONFLICTING_CREDENTIALS`;
  - `Authorization: Basic ...` plus API key is not a credential conflict and
    authenticates through the API key;
  - multi-credential `/actuator/health` still returns the health response;
  - conflict responses do not set `TenantContext`.
- Routing and cleanup tests:
  - valid `acme` and `globex` requests return tenant-specific rows;
  - cross-tenant data is not visible;
  - parallel authorized requests cannot leak `TenantContext`;
  - direct filter test proves same servlet-thread cleanup after downstream
    failure.
- Architecture tests:
  - implement as JUnit source-text scans using `java.nio.file.Files.walk`; no
    Konsist or ArchUnit dependency;
  - only `TenantAuthorizationFilter` calls `TenantContext.set`;
  - custom security filters are not annotated with `@Component`;
  - no production code in this module imports from module `05`;
  - repositories do not call bare `transaction(` except through
    `TenantTransaction`.

### 6. Update documentation and diagrams

Complexity: M

- Add module `README.md` and `README.ko.md`.
- Explain:
  - tenant routing vs tenant authorization;
  - JWT/API-key/demo-session identity sources;
  - demo session header is not a production session cookie;
  - CSRF is disabled because this example is stateless JSON;
  - request and error contract;
  - when to choose this strategy;
  - CI coverage and why Nightly remains unchanged.
  - top-level "Not for production" caveat for `DemoJwtDecoder`, fixed API key
    maps, and demo session header.
  - MVC `ThreadLocal` propagation does not transfer unchanged to coroutine,
    WebFlux, or virtual-thread examples.
- Add architecture PNG/SVG and sequence PNG/SVG under
  `docs/images/readme-diagrams/`.
- Visually inspect PNGs for readable contrast.
- Update `10-multi-tenant/README.md` and `README.ko.md` to list module `06`.
- Update root README files only if their module tables enumerate Chapter 10
  examples directly.

### 7. Wire selected Examples CI

Complexity: S

- Add `10-multi-tenant/06-spring-security-tenant-authorization-spring-web/**`
  to both `on.push.paths` and `on.pull_request.paths` in Examples workflow.
- Add `:06-spring-security-tenant-authorization-spring-web:build` to the
  selected examples Gradle command.
- Run `actionlint .github/workflows/examples.yml`.

### 8. Verify and review

Complexity: M

Run in order:

1. `./gradlew projects --quiet | rg '06-spring-security-tenant-authorization-spring-web'`
2. `./gradlew :06-spring-security-tenant-authorization-spring-web:compileKotlin --warning-mode all --console=plain`
3. `repo-test-summary -- ./gradlew :06-spring-security-tenant-authorization-spring-web:test --stacktrace --continue`
4. `./gradlew :06-spring-security-tenant-authorization-spring-web:build --stacktrace --continue`
5. `actionlint .github/workflows/examples.yml`
6. `git diff --check`
7. README diagram scan for required Architecture Diagram PNG links and no
   Mermaid blocks.
8. Visual PNG inspection through image viewer tooling.
9. IDE diagnostics if available; otherwise record compile/test fallback.
10. Step 6-R current-session 6-Tier review plus Claude Code CLI review with
    `P0=0`, `P1=0`.

### 9. Publish

Complexity: M

- Add `docs/lessons/2026-05-23-issue-54-spring-security-tenant-authorization.md`.
- Commit with Lore protocol trailers.
- Push branch and open a PR against `develop`, assigned to `debop`.
- Add `examples`, `documentation`, and security-related labels if available.
- Add Step 7-R PR comment and formal review entry.
- Watch CI until required checks are `SUCCESS` or `SKIPPED`.
- Do not merge unless the user requests merge after the DoD report.

## Acceptance Mapping

- Tenant from authenticated requests:
  Tasks 2 and 3 implement JWT/API-key/demo-session authentication and tenant
  authorization before `TenantContext` is set.
- Focused tests for isolation and error handling:
  Task 5 covers valid access, missing auth, invalid tenant claims, mismatch,
  and cross-tenant denial.
- README strategy guidance:
  Task 6 updates English/Korean docs and committed PNG diagrams.
- CI/nightly coverage decision:
  Task 7 wires Examples CI; the spec records why Nightly is unchanged.

## Review Notes

Step 2-R/3-R advisor review is required before implementation proceeds. The
gate passes only when the latest normalized table shows `P0=0` and `P1=0`.
