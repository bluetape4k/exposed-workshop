# Issue 61 HTTP outbox idempotency 예제

## 배경

Issue #61은 두 번째 paired 12장 production-integration topic인 Spring Boot 4/Ktor HTTP
client outbox/idempotency 예제를 추가했다.

## 결정

Gateway dispatch 전에 outbound payment record를 저장하고, unique idempotency key를
duplicate boundary로 사용하며, test가 실제 external HTTP service를 요구하지 않도록 gateway를
교체 가능하게 유지한다.

## 결과

Spring 모듈은 MVC, `RestClient`, controller advice, Exposed JDBC repository를 사용한다.
Ktor 모듈은 route, `StatusPages`, Ktor HTTP client, `Dispatchers.IO` 뒤로 격리한 blocking
Exposed call로 같은 contract를 반영한다.

## 검증

변경 후 두 module build와 `Examples.yml` workflow path를 실행한다.

## 향후 지침

12장의 paired example에서는 runnable module과 같은 PR에서 chapter README, root README file,
`.github/workflows/examples.yml`을 갱신한다.
