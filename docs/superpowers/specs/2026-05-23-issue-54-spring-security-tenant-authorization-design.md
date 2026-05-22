# Issue 54 Spring Security Tenant Authorization Design

## Context

Issue #54 closes the production trust gap left by the Chapter 10 routing
examples. The existing Spring MVC examples show tenant propagation and
isolation, but `X-Tenant-ID` is still the final trust source. This example must
authenticate the caller first, derive the caller's tenant from security
material, then authorize the requested tenant before any Exposed JDBC
transaction can select a tenant database.

This is a Type A Full Design change because it adds a security-sensitive
example module, public README material, CI wiring, and review-gated
authorization behavior.

## External Evidence

- Spring Security 6.5 servlet resource server documentation shows Kotlin
  `SecurityFilterChain` configuration with OAuth2 Resource Server JWT and a
  custom `jwtAuthenticationConverter`.
  Source: Context7 `/websites/spring_io_spring-security_reference_6_5`,
  servlet OAuth2 resource server JWT.
- Spring Security test support provides MockMvc request post processors,
  including JWT/OAuth2 helpers through `spring-security-test`.
  Source: Context7 `/websites/spring_io_spring-security_reference_6_5`,
  servlet test API docs.
- Existing repository pattern:
  `12-production-integration/05-spring-auth-session` uses servlet
  `SecurityFilterChain`, stateless JSON security, and `spring-security-test`.
- Existing tenant routing pattern:
  `10-multi-tenant/05-database-per-tenant-spring-web` owns one Hikari pool and
  Exposed `Database` per tenant, with no default datasource fallback.
- Prior R2DBC sibling design:
  `exposed-r2dbc-workshop` issue #40 requires tenant claim failures to remain
  authorization failures and requires the routing context to be written only
  after tenant authorization succeeds.
- Module `05` normalizes tenant headers with `trim().lowercase()`. This module
  keeps that policy so `X-Tenant-ID: ACME` maps to `acme`.

## Scope

- Add `10-multi-tenant/06-spring-security-tenant-authorization-spring-web`.
- Use Spring MVC, Spring Security, Exposed JDBC, and two H2 tenant databases:
  `acme` and `globex`.
- Support three self-contained authentication paths for the workshop:
  - JWT bearer token with a `tenant_id` claim.
  - API key header mapped to a tenant.
  - Demo session header mapped to a tenant.
- Keep the inventory API and database-per-tenant routing shape comparable to
  module `05`.
- Add English/Korean READMEs with committed Architecture Diagram PNG and an
  additional sequence/request-flow PNG.
- Wire the module into Chapter 10 docs, root README docs where module lists are
  maintained, and selected examples CI.

Out of scope:

- Production identity provider or authorization server setup.
- Dynamic tenant onboarding/provisioning. Issue #55 owns that.
- A shared bluetape4k security abstraction.
- Testcontainers database matrix coverage. This module remains H2-only and the
  CI decision is documented.

## Design

### Security Boundary

The example must prove this invariant:

1. Authentication establishes a tenant identity.
2. `X-Tenant-ID` selects the target tenant.
3. Tenant authorization succeeds only when the authenticated tenant equals the
   requested tenant.
4. `TenantContext` is written only after tenant authorization succeeds.
5. Repositories select an Exposed `Database` only through `TenantTransaction`.

No request path may write `TenantContext` from the raw tenant header alone. If
header-only trust can reach Exposed, the module fails its primary purpose.

### Authentication Sources

The module supports fixed demo credentials so tests and README snippets remain
self-contained:

| Source | Request shape | Tenant source |
|---|---|---|
| JWT | `Authorization: Bearer demo-acme-token` | `tenant_id=acme` |
| API key | `X-API-Key: demo-acme-key` | configured API key map |
| Demo session | `X-Demo-Session: acme-session` | configured demo session map |

The demo JWT decoder accepts only fixed token strings. It returns Spring
Security `Jwt` instances with predictable claims and does not implement signing
or issuer discovery. That keeps the example focused on tenant authorization,
not identity-provider infrastructure.

JWT authentication must still succeed when a bearer token is syntactically
valid but lacks a tenant claim. The tenant claim problem is an authorization
failure and should return `403 Forbidden`, not `401 Unauthorized`.

Invalid bearer tokens, invalid API keys, and invalid demo sessions remain
authentication failures and return `401 Unauthorized`.

### Credential Precedence

Multiple credential sources in one request are rejected before authentication.
A request may send exactly one of:

- `Authorization: Bearer ...`
- `X-API-Key`
- `X-Demo-Session`

Header presence means at least one header value exists and at least one value
is non-blank after trim. Empty API-key or demo-session headers do not count as
credential sources and do not authenticate.

The `Authorization` header is counted as a bearer credential source only when
any non-blank header value uses the `Bearer` scheme with case-insensitive
scheme matching. Non-bearer authorization schemes and authorization values
without a scheme are ignored by this example because the module does not
support Basic, Digest, or form login authentication. Multiple `Authorization`
header values are scanned together; if any value is bearer and another
supported credential source is present, the request is conflicting.

If two or more credential sources are present, the request returns
`400 Bad Request` with `CONFLICTING_CREDENTIALS`. The module deliberately avoids
"first credential wins" behavior because it can hide cross-tenant escalation
attempts such as bearer token `acme` plus API key `globex`.

Public health checks bypass credential conflict checks and tenant
authorization. `/actuator/health` returns the health response even when a proxy
or caller sends unrelated credential headers.

### Authorization Filter

Custom filters are plain classes instantiated inside `SecurityConfiguration`;
they are not `@Component` classes and are not exposed as `Filter` beans. This
prevents Spring Boot from registering them as independent servlet filters
outside `SecurityFilterChain`.

`CredentialConflictFilter` is registered inside `SecurityFilterChain` before
authentication filters. It checks only credential-source presence and writes
`400 CONFLICTING_CREDENTIALS` for mixed credential sources.

`TenantAuthorizationFilter` is registered inside `SecurityFilterChain`. It runs
after Spring Security authentication and request authentication authorization
have had a chance to populate and validate `SecurityContextHolder`, but before
MVC handler invocation. It:

- skips public actuator health requests;
- resolves the authenticated tenant from JWT/API-key/demo-session
  authentication;
- returns `403` for missing, malformed, unknown, or mismatched authenticated
  tenant identity;
- validates exactly one `X-Tenant-ID` header only after the authenticated
  tenant is valid;
- rejects blank, comma-containing, too-long, or unknown tenant selectors;
- sets `TenantContext` only for an authorized tenant and clears it in
  `finally`.

The check order is fixed:

1. `CredentialConflictFilter` skips public health, then rejects mixed supported
   credential sources.
2. Spring Security authenticates the one supported credential source.
3. Spring Security `AuthorizationFilter` enforces authentication for inventory
   requests.
4. `TenantAuthorizationFilter` resolves the authenticated tenant and returns
   `403` for missing, malformed, or unknown tenant identity.
5. Only after a valid authenticated tenant exists, the filter validates
   `X-Tenant-ID` and returns `400`/`404` for selector errors.
6. Tenant mismatch returns `403`; match sets `TenantContext` and continues.

Spring Security owns `401` and `403` for authentication and authorization
failures. Tenant selector validation returns stable JSON errors before the
request reaches the controller.

`DemoApiKeyAuthenticationFilter` and `DemoSessionAuthenticationFilter` are also
registered inside the Spring Security chain, before bearer-token authentication.
They must not be plain `@Component` servlet filters because they create
`Authentication` objects and must participate in Spring Security exception
translation.

Security logs must not record raw `Authorization`, `X-API-Key`, or
`X-Demo-Session` values. When a credential-related failure is logged, log only
the credential source and outcome, not the secret-bearing header value.

### Tenant Routing

The database-per-tenant infrastructure is intentionally similar to module `05`:

- `TenantDataSourceProperties` configures all known tenant datasources under
  `app.tenants`.
- `TenantDatabaseRegistry` builds and owns one Hikari pool and one Exposed
  `Database` per `TenantId`.
- `TenantTransaction.execute { ... }` reads `TenantContext.current()` by
  default and runs `transaction(registry.databaseFor(tenant))`.
- The registry fails startup if a known tenant is missing or an unknown tenant
  is configured.
- There is no default datasource fallback.

### Domain

The module reuses the inventory API shape from module `05`:

- `InventoryItems`
- `InventoryItemRecord`
- `CreateInventoryItemRequest`
- `InventoryRepository`
- `InventoryService`
- `InventoryController`
- `InventorySeeder`

Tenant seed data must be visibly different for `acme` and `globex` so tests and
README examples can prove isolation.

### Error Contract

| Case | Result |
|---|---|
| valid auth tenant equals `X-Tenant-ID` | `200 OK` |
| missing authentication | `401 Unauthorized` |
| invalid bearer token/API key/demo session | `401 Unauthorized` |
| multiple credential sources | `400 Bad Request`, `CONFLICTING_CREDENTIALS` |
| missing `X-Tenant-ID` | `400 Bad Request`, `MISSING_TENANT` |
| blank, duplicated, or malformed `X-Tenant-ID` | `400 Bad Request`, `MISSING_TENANT` |
| unknown `X-Tenant-ID` | `404 Not Found`, `UNKNOWN_TENANT` |
| authenticated tenant missing or unknown | `403 Forbidden` |
| authenticated tenant malformed | `403 Forbidden` |
| authenticated tenant differs from `X-Tenant-ID` | `403 Forbidden` |

The 400/404 tenant-selector behavior stays aligned with module `05`; the new
403 cases are the security boundary added by this issue.
Tenant selector and tenant claim values are normalized with trim/lowercase
before matching, so `ACME`, ` acme `, and `acme` are treated as the same tenant.
The tenant selector length cap is 64 characters, matching module `05`. Blank,
comma-containing, and too-long values remain malformed.
For request tenant selectors, blank, duplicated, comma-containing, and too-long
values intentionally reuse module `05`'s `MISSING_TENANT` code. For
authenticated tenant identity, malformed means a non-string claim, blank claim,
comma-containing claim, mid-value whitespace, or a value longer than 64
characters. Malformed authenticated identity returns `403` because the caller
is authenticated but not authorized for a valid tenant.

### README Diagrams

Both README files link the same committed PNG files under
`docs/images/readme-diagrams/`:

- `10-multi-tenant-06-spring-security-tenant-authorization-spring-web-architecture-01.png`
- `10-multi-tenant-06-spring-security-tenant-authorization-spring-web-sequence-02.png`

SVG sources are committed next to the PNGs. Diagram text is English. README
files must not contain Mermaid blocks.

## Verification

Local verification:

- `./gradlew :06-spring-security-tenant-authorization-spring-web:test --stacktrace --continue`
- `./gradlew :06-spring-security-tenant-authorization-spring-web:build --stacktrace --continue`
- `./gradlew projects --quiet | rg '06-spring-security-tenant-authorization-spring-web'`
- `actionlint .github/workflows/examples.yml`
- README diagram scan for Architecture Diagram PNG links, existing PNG files,
  and no Mermaid blocks.
- Visual inspection of generated PNGs for readable contrast.

Required test coverage:

- JWT tenant claim can access the matching tenant.
- JWT/header mismatch returns 403.
- JWT without `tenant_id` returns 403.
- Missing authentication returns 401.
- API key can access the matching tenant.
- Invalid API key returns 401.
- Demo session can access the matching tenant.
- Demo session/header mismatch returns 403.
- Missing, malformed, and unknown tenant selectors keep the module's stable
  400/404 error contract for authenticated callers.
- Parallel authorized `acme`/`globex` requests cannot leak `TenantContext`.
- A failing downstream request clears `TenantContext` on the servlet thread.
- Architecture scan proves only `TenantAuthorizationFilter` writes
  `TenantContext` on HTTP request paths.
- Multi-credential requests are rejected with `CONFLICTING_CREDENTIALS`.
- Uppercase tenant selector and tenant claim variants follow the explicit
  trim/lowercase normalization policy.

Review gates:

- Step 2-R/3-R advisor review on this spec and plan using Claude Code CLI via
  stdin with timeout >= 5 minutes.
- Step 6-R code review after implementation using the 6-Tier frame plus Claude
  Code CLI via stdin with timeout >= 5 minutes.

## Advisor Gate

| Artifact | Result | Notes |
|---|---|---|
| `.omx/artifacts/claude-issue-54-spec-plan-advisor-stdin-6min-20260523005725.md` | FAIL, P0=0/P1=3 | Accepted edits for credential conflict behavior, source-text architecture tests, and custom filter registration. |
| `.omx/artifacts/claude-issue-54-spec-plan-advisor-rerun-stdin-6min-20260523010057.md` | FAIL, P0=0/P1=3 | Accepted edits for bearer detection, authorization/selector check order, and malformed authenticated tenant semantics. |
| `.omx/artifacts/claude-issue-54-spec-plan-advisor-rerun2-stdin-6min-20260523010327.md` | PASS, P0=0/P1=0 | Applied P2 clarifications for concrete filter anchors, blank credential presence, multiple authorization headers, selector length, and whitespace normalization tests. |

## CI Decision

This module is H2-only, so it belongs in selected Examples CI and does not need
a Nightly Testcontainers shard. The selected Examples workflow must include the
module path filters and the `:06-spring-security-tenant-authorization-spring-web:build`
task.
