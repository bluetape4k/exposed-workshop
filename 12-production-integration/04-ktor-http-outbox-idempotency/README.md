# Ktor HTTP Outbox and Idempotency

[English](README.md) | [한국어](README.ko.md)

This module is the Ktor pair for the chapter 12 HTTP client
outbox/idempotency topic. It mirrors the Spring Boot 4 sample while keeping
blocking Exposed JDBC work behind a repository-owned `Dispatchers.IO` boundary.

## Architecture

![Ktor HTTP outbox idempotency Architecture diagram](../../docs/images/readme-diagrams/12-production-integration-04-ktor-http-outbox-idempotency-architecture-01.png)

## What This Shows

- Explicit Ktor routes for creating, retrying, and listing payments.
- Blocking Exposed JDBC transactions isolated inside suspend repository methods.
- A unique idempotency key that returns the existing record on duplicate submit.
- `StatusPages` error mapping for validation, not found, permanent failure, and
  unexpected failures.
- Ktor client gateway tests that use a fake gateway instead of a real external
  HTTP service.

## HTTP Contract

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/payments` | Inserts a pending outbox record, calls the gateway, and returns `201`; duplicate idempotency keys return the existing record with `200`. |
| `POST` | `/payments/{id}/retry` | Retries only records in `RETRYABLE_FAILED`. |
| `GET` | `/payments/{id}` | Returns one payment/outbox record. |
| `GET` | `/payments` | Returns all payment/outbox records. |

## Run

```bash
./gradlew :04-ktor-http-outbox-idempotency:run
```

```bash
curl -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"order-100","amountCents":2500,"idempotencyKey":"payment-key-100"}'
```

The default gateway base URL points to a non-routable example host so the sample
does not accidentally charge a real provider. Tests override the gateway with an
in-memory fake.

## Test

```bash
./gradlew :04-ktor-http-outbox-idempotency:test
```

The test suite covers route success, duplicate idempotency keys, retryable
failure followed by retry success, permanent failure, repository persistence,
and sanitized Ktor errors.
