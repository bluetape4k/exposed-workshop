# Ktor Auth Session

[English](README.md) | [한국어](README.ko.md)

This module is the Ktor half of the chapter 12 authentication/session example.
It uses Ktor Authentication, Ktor Sessions, service-level role checks, and
Exposed-backed user/session metadata.

## Architecture

![Ktor auth session Architecture diagram](../../docs/images/readme-diagrams/12-production-integration-06-ktor-auth-session-architecture-01.png)

## What This Shows

- Ktor Basic authentication backed by a service and Exposed repository.
- Explicit route protection with `authenticate`.
- Service-level `ADMIN` role enforcement for authorization failures.
- Cookie-backed session token transport with hashed session metadata, one-hour
  expiry, and a cookie-only profile endpoint.
- HTTP tests for missing credentials, invalid credentials, permission denial,
  authorized profile access, session creation/listing, and cookie replay.

## API Surface

| Endpoint | Authentication | Result |
|---|---|---|
| `GET /api/public` | Anonymous | Public status |
| `GET /api/profile` | HTTP Basic | Current user's profile and roles |
| `GET /api/admin` | HTTP Basic + `ADMIN` role | Admin-only profile |
| `POST /api/sessions` | HTTP Basic | Creates persisted hashed session metadata, returns the raw token once, and sets `auth_session` |
| `GET /api/sessions` | HTTP Basic | Lists current user's active session metadata without raw tokens |
| `GET /api/session-profile` | `auth_session` cookie | Current profile resolved from the session token |

Seeded accounts:

| Username | Password | Roles |
|---|---|---|
| `alice` | `password` | `USER` |
| `admin` | `password` | `USER`, `ADMIN` |

## Ktor vs Spring Boot 4

| Concern | Ktor module | Spring Boot 4 module |
|---|---|---|
| Authentication hook | `Authentication` plugin with Basic provider | Spring Security filter chain |
| User lookup | `AuthService.authenticate` | `UserDetailsService` adapter |
| Authorization | Explicit service role checks | Declarative matcher rules |
| Session metadata | Route writes repository metadata, sets cookie, and resolves the cookie on `/api/session-profile` | MVC controller writes repository metadata |
| Blocking Exposed work | Repository wraps transactions in `Dispatchers.IO` | Repository uses blocking MVC request threads |

The example uses BCrypt from Spring Security Crypto so seeded passwords use a
per-hash salt and adaptive verification. Session rows store SHA-256 token hashes
with a one-hour expiry; raw tokens are returned only when created and transported
in the `HttpOnly` cookie. Add `Secure`, explicit `SameSite`, and rotation policy
when deploying behind TLS.

## Run

```bash
./gradlew :06-ktor-auth-session:run
```

```bash
curl -u alice:password http://localhost:8080/api/profile
curl -u admin:password http://localhost:8080/api/admin
curl -i -u alice:password -X POST http://localhost:8080/api/sessions
curl -b 'auth_session=...' http://localhost:8080/api/session-profile
```

## Test

```bash
./gradlew :06-ktor-auth-session:test
```
