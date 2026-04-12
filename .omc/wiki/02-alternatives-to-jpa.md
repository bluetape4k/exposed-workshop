---
title: "02 Alternatives to JPA"
tags: [exposed, jpa, hibernate-reactive, r2dbc, vertx, comparison]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 02 Alternatives to JPA

JPA 대안 기술 비교: Hibernate Reactive, R2DBC, Vert.x SQL Client.

## 모듈

| 모듈 | 설명 | 연결 모델 |
|------|------|----------|
| `hibernate-reactive-example` | Hibernate Reactive + Mutiny + PostgreSQL | Netty Non-blocking |
| `r2dbc-example` | Spring Data R2DBC + Coroutines | R2DBC Non-blocking |
| `vertx-sqlclient-example` | Vert.x SQL Client + 이벤트 루프 | Netty Event Loop |

## 핵심 비교

| 항목 | Exposed | Hibernate Reactive | R2DBC | Vert.x |
|------|---------|-------------------|-------|--------|
| 쿼리 스타일 | 타입 안전 DSL | Criteria API | Repository 인터페이스 | SqlTemplate |
| 트랜잭션 | `transaction { }` | `withTransaction` | `@Transactional` | `withSuspendTransaction` |
| 타입 안전성 | 컴파일 타임 | Criteria (장황) | 문자열 `@Query` | 문자열 SQL |
| 학습 곡선 | 낮음 | 중간 | 낮음 | 높음 |

## 실행

```bash
./gradlew :hibernate-reactive-example:test
./gradlew :r2dbc-example:test
./gradlew :vertx-sqlclient-example:test
```
