---
title: "09 Spring Integration"
tags: [exposed, spring, transaction, repository, cache, redis]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 09 Spring Integration

Spring Boot + Exposed 통합: 자동 설정, 트랜잭션, Repository, 캐시.

## 모듈 (7개)

| 모듈 | 설명 | 핵심 기술 |
|------|------|----------|
| `01-springboot-autoconfigure` | 자동 설정 기반 Exposed 통합 | `ExposedAutoConfiguration` |
| `02-transactiontemplate` | 프로그래밍 방식 트랜잭션 | `TransactionTemplate` |
| `03-spring-transaction` | 선언적 트랜잭션 | `@Transactional` + `SpringTransactionManager` |
| `04-exposed-repository` | 동기 Repository (Spring MVC) | `JdbcRepository`, DSL/DAO |
| `05-exposed-repository-coroutines` | 코루틴 Repository (WebFlux) | `newSuspendedTransaction` |
| `06-spring-cache` | Spring Cache + Redis (동기) | `@Cacheable`, `@CacheEvict` |
| `07-spring-suspended-cache` | 코루틴 Redis 캐시 | `LettuceSuspendedCache` 데코레이터 |

## 핵심 패턴

- `SpringTransactionManager` 등록으로 Spring/Exposed 트랜잭션 경계 통합
- `@Transactional`은 suspend 함수 미지원 → 코루틴은 `newSuspendedTransaction` 사용
- 캐시: 동기 `@Cacheable` vs 코루틴 `LettuceSuspendedCache` 데코레이터

## 학습 순서

01 → 02 → 03 → 04 → 05, 04 → 06, 05 → 07

## 실행

```bash
./gradlew :04-exposed-repository:test
./gradlew :07-spring-suspended-cache:test
```
