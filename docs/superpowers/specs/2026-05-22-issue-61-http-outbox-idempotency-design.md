# Issue 61 HTTP Outbox Idempotency Design

## Context

Issue #61 adds a chapter 12 pair that demonstrates safe outbound HTTP integration
for Spring Boot 4 and Ktor services backed by Exposed JDBC.

Prior retrieval:

- `qmd query "HTTP client outbox idempotency external API persisted outbound requests retry duplicate permanent failure examples" -c bluetape4k-docs --no-rerank`
- `qmd query "Spring Boot Ktor Exposed outbox idempotency retry transaction cancellation testcontainers cautions" -c bluetape4k-docs --no-rerank`
- `qmd query "outbox idempotency external API retry duplicate key permanent failure Kotlin Exposed" -c wiki --no-rerank`

Relevant cautions:

- Keep blocking Exposed JDBC work inside repositories.
- Ktor routes must not call `transaction {}` directly.
- Duplicate writes must be explicit through an idempotency key.
- Retryable and permanent failures must persist different states.

## Scope

Add two modules:

- `12-production-integration/03-spring-http-outbox-idempotency`
- `12-production-integration/04-ktor-http-outbox-idempotency`

Both modules expose:

- `POST /payments` to persist an outbound payment request and dispatch it.
- `POST /payments/{id}/retry` to retry retryable failures.
- `GET /payments/{id}` and `GET /payments` to inspect stored outbox records.
- `/` and `/health` discovery endpoints.

## Domain Contract

`PaymentRequest` contains `orderId`, `amountCents`, and `idempotencyKey`.

The service must:

1. Normalize and validate caller input.
2. Insert a pending outbox row before calling the external client.
3. Return the existing record for duplicate idempotency keys.
4. Mark 2xx external responses as `SUCCEEDED`.
5. Mark retryable failures as `RETRYABLE_FAILED`.
6. Mark client/permanent failures as `PERMANENT_FAILED`.
7. Retry only `RETRYABLE_FAILED` records.

## Implementation Notes

- Spring production client uses `RestClient`.
- Ktor production client uses Ktor `HttpClient`.
- Tests use fake clients and do not call real external services.
- Exposed table names are stack-specific to avoid collision:
  - `spring_payment_outbox`
  - `ktor_payment_outbox`
- Public README files explain the Spring vs Ktor tradeoff.

## Acceptance Criteria

- Both modules build and test independently.
- Success, retryable failure, duplicate idempotency key, and permanent failure
  paths are covered.
- `.github/workflows/examples.yml` builds the new modules.
- Root and chapter README files list the new pair.
