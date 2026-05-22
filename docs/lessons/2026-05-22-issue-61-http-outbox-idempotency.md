# Issue 61 HTTP Outbox Idempotency Examples

## Context

Issue #61 added the second paired chapter 12 production-integration topic:
Spring Boot 4 and Ktor HTTP client outbox/idempotency examples.

## Decision

Persist the outbound payment record before gateway dispatch, use a unique
idempotency key as the duplicate boundary, and keep the gateway replaceable so
tests do not require a real external HTTP service.

## Outcome

The Spring module uses MVC, `RestClient`, controller advice, and an Exposed JDBC
repository. The Ktor module mirrors the contract with routes, `StatusPages`, a
Ktor HTTP client, and blocking Exposed calls isolated behind `Dispatchers.IO`.

## Verification

Run the two module builds and the `Examples.yml` workflow path after changes.

## Future Guidance

For chapter 12 paired examples, update the chapter README, root README files,
and `.github/workflows/examples.yml` in the same PR as the runnable modules.
