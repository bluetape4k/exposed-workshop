# Issue 61 HTTP outbox idempotency 설계

## 배경

Issue #61은 Exposed JDBC 기반 Spring Boot 4/Ktor service의 safe outbound HTTP integration을
보여 주는 12장 pair를 추가한다.

이전 검색:

- `gno query "HTTP client outbox idempotency external API persisted outbound requests retry duplicate permanent failure examples" -c bluetape4k-docs --no-rerank`
- `gno query "Spring Boot Ktor Exposed outbox idempotency retry transaction cancellation testcontainers cautions" -c bluetape4k-docs --no-rerank`
- `gno query "outbox idempotency external API retry duplicate key permanent failure Kotlin Exposed" -c wiki --no-rerank`

관련 주의사항:

- Blocking Exposed JDBC work는 repository 안에 둔다.
- Ktor route는 `transaction {}`을 직접 호출하면 안 된다.
- Duplicate write는 idempotency key를 통해 명시적이어야 한다.
- Retryable failure와 permanent failure는 서로 다른 state로 저장돼야 한다.

## 범위

두 모듈을 추가한다.

- `12-production-integration/03-spring-http-outbox-idempotency`
- `12-production-integration/04-ktor-http-outbox-idempotency`

두 모듈 모두 다음 endpoint를 노출한다.

- `POST /payments`: outbound payment request를 저장하고 dispatch한다.
- `POST /payments/{id}/retry`: retryable failure를 다시 시도한다.
- `GET /payments/{id}` 및 `GET /payments`: 저장된 outbox record를 조회한다.
- `/` 및 `/health` discovery endpoint.

## Domain contract

`PaymentRequest`는 `orderId`, `amountCents`, `idempotencyKey`를 포함한다.

Service는 다음을 만족해야 한다.

1. Caller input을 normalize/validate한다.
2. External client 호출 전에 pending outbox row를 insert한다.
3. Duplicate idempotency key에는 existing record를 반환한다.
4. 2xx external response를 `SUCCEEDED`로 표시한다.
5. Retryable failure를 `RETRYABLE_FAILED`로 표시한다.
6. Client/permanent failure를 `PERMANENT_FAILED`로 표시한다.
7. `RETRYABLE_FAILED` record만 retry한다.

## 구현 메모

- Spring production client는 `RestClient`를 사용한다.
- Ktor production client는 Ktor `HttpClient`를 사용한다.
- Test는 fake client를 사용하며 real external service를 호출하지 않는다.
- Collision을 피하기 위해 Exposed table name은 stack-specific으로 둔다.
  - `spring_payment_outbox`
  - `ktor_payment_outbox`
- Public README file은 Spring vs Ktor tradeoff를 설명한다.

## 수용 기준

- 두 모듈은 독립적으로 build/test된다.
- Success, retryable failure, duplicate idempotency key, permanent failure path를 다룬다.
- `.github/workflows/examples.yml`은 새 모듈을 build한다.
- Root 및 chapter README file은 새 pair를 나열한다.
