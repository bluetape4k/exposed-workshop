# 09 Spring Integration

Spring Boot 환경에서 Exposed를 안정적으로 운영하기 위한 통합 패턴을 다루며, 트랜잭션, Repository, Cache까지 포괄하는 예제를 제공합니다.

## 챕터 목표

- Spring 트랜잭션과 Exposed 트랜잭션 경계를 정렬해 일관된 데이터 흐름을 설계한다.
- Repository 레이어 표준 패턴(동기/코루틴)을 정립한다.
- 캐시 통합 시 일관성과 성능의 균형을 맞추는 전략을 확인한다.

## 선수 지식

- Spring Boot 기본
- `05-exposed-dml`, `08-coroutines` 내용

## 포함 모듈

| 모듈                                 | 설명                     |
|------------------------------------|------------------------|
| `01-springboot-autoconfigure`      | 자동 설정 기반 통합            |
| `02-transactiontemplate`           | TransactionTemplate 연동 |
| `03-spring-transaction`            | 선언적 트랜잭션 연동            |
| `04-exposed-repository`            | Repository 패턴 (동기)     |
| `05-exposed-repository-coroutines` | Repository 패턴 (코루틴)    |
| `06-spring-cache`                  | Spring Cache 연동        |
| `07-spring-suspended-cache`        | 코루틴 기반 캐시 연동           |

## 권장 학습 순서

1. `01-springboot-autoconfigure`
2. `03-spring-transaction`
3. `04-exposed-repository`
4. `05-exposed-repository-coroutines`
5. `06-spring-cache`
6. `07-spring-suspended-cache`

## 실행 방법

```bash
./gradlew :exposed-09-spring:test
```

## 테스트 포인트

- 트랜잭션 전파/롤백 규칙이 의도대로 작동하는지 검증한다.
- 캐시 적중/미스/무효화 시나리오를 점검한다.
- 코루틴 경로와 동기 경로 결과 일관성을 확인한다.

## 성능·안정성 체크포인트

- 커넥션 풀 고갈 상황에서 보호 장치가 작동하는지 점검한다.
- 캐시 무효화 누락으로 stale 데이터가 노출되지 않도록 한다.

## 다음 챕터

- [10-multi-tenant](../10-multi-tenant/README.md): 테넌트 분리 아키텍처를 실전 형태로 확장합니다.
