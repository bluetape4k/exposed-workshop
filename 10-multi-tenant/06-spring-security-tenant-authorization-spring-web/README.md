# Spring Security Tenant Authorization Spring Web (06)

English | [한국어](./README.ko.md)

This Spring MVC example binds tenant routing to authenticated identity before
Exposed selects a tenant database. It builds on the database-per-tenant module:
each tenant still owns a dedicated Hikari pool and Exposed `Database`, but
`X-Tenant-ID` is no longer trusted by itself.

> Not for production: `DemoJwtDecoder`, fixed API keys, and `X-Demo-Session`
> are workshop fixtures. Production systems need signed tokens, issuer and
> audience validation, key rotation, protected secret storage, and real session
> management.

Choose this strategy when a service must prove that the requested tenant
matches authenticated claims or server-side identity before routing to tenant
data. The MVC `ThreadLocal` propagation shown here does not transfer unchanged
to coroutine, WebFlux, or virtual-thread modules.

## Architecture Diagram

![Spring Security Tenant Authorization Spring Web Architecture diagram](../../docs/images/readme-diagrams/10-multi-tenant-06-spring-security-tenant-authorization-spring-web-architecture-01.png)

## Request Flow

![Spring Security Tenant Authorization Spring Web Sequence diagram](../../docs/images/readme-diagrams/10-multi-tenant-06-spring-security-tenant-authorization-spring-web-sequence-02.png)

## Strategy

| Concern | Behavior |
|---|---|
| Credential sources | Exactly one of bearer JWT, `X-API-Key`, or `X-Demo-Session` |
| Tenant authorization | Authenticated tenant must match one `X-Tenant-ID` value |
| Conflict handling | Mixed supported credential sources return `400 CONFLICTING_CREDENTIALS` |
| Missing auth | Spring Security returns `401 Unauthorized` |
| Missing/invalid tenant selector | `400 MISSING_TENANT`; unknown selector returns `404 UNKNOWN_TENANT` |
| Routing boundary | `TenantAuthorizationFilter` sets `TenantContext`; repositories use `TenantTransaction` |
| Fallback | No default datasource; no header-only tenant routing |
| Isolation | Each tenant has a different H2 JDBC URL and Hikari pool |

## Demo Credentials

| Source | Header | Tenant |
|---|---|---|
| JWT | `Authorization: Bearer demo-acme-token` | `acme` |
| JWT | `Authorization: Bearer demo-globex-token` | `globex` |
| API key | `X-API-Key: demo-acme-key` | `acme` |
| API key | `X-API-Key: demo-globex-key` | `globex` |
| Demo session | `X-Demo-Session: acme-session` | `acme` |
| Demo session | `X-Demo-Session: globex-session` | `globex` |

## Run

```bash
./gradlew :06-spring-security-tenant-authorization-spring-web:bootRun
```

```bash
curl -H 'Authorization: Bearer demo-acme-token' \
  -H 'X-Tenant-ID: acme' \
  http://localhost:8080/inventory/ACME-ROUTER-001

curl -H 'X-API-Key: demo-globex-key' \
  -H 'X-Tenant-ID: globex' \
  http://localhost:8080/inventory/GLOBEX-DRONE-001
```

## Error Contract

| Case | Status |
|---|---|
| Missing or invalid credential | `401 Unauthorized` |
| Multiple supported credential sources | `400 CONFLICTING_CREDENTIALS` |
| JWT/API/session tenant missing, malformed, or unknown | `403 Forbidden` |
| Authenticated tenant differs from `X-Tenant-ID` | `403 Forbidden` |
| Missing, blank, duplicated, or malformed `X-Tenant-ID` | `400 MISSING_TENANT` |
| Unknown `X-Tenant-ID` | `404 UNKNOWN_TENANT` |

## Test

```bash
./gradlew :06-spring-security-tenant-authorization-spring-web:test
```

The tests cover JWT, API-key, and demo-session access; invalid credentials;
tenant mismatch; missing and malformed claims; credential conflicts; tenant
selector failures; cross-tenant isolation; `ThreadLocal` cleanup; rollback;
database bootstrap; datasource close; and source-text architecture guards.

## CI Coverage

The module uses H2-only tenant databases and is included in selected examples
CI. It does not need a separate Testcontainers or Nightly shard.
