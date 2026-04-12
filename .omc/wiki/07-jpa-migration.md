---
title: "07 JPA Migration"
tags: [exposed, jpa, migration, hibernate, inheritance, relations]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 07 JPA Migration

JPA 코드베이스를 Exposed로 전환하는 전략.

## JPA vs Exposed

| 항목 | JPA | Exposed |
|------|-----|---------|
| 매핑 | `@Entity`, `@Column` | Kotlin DSL `object Table` |
| 쿼리 | JPQL / Criteria | DSL `selectAll().where { }` |
| 지연 로딩 | `FetchType.LAZY` | 명시적 `load()` / `with()` |
| 트랜잭션 | `@Transactional` | `transaction { }` 람다 |
| 상속 | `@Inheritance(SINGLE_TABLE/JOINED/TABLE_PER_CLASS)` | 테이블 구조로 직접 표현 |
| 감사 | `@CreatedDate` | `EntityHook` / Property Delegate |
| 낙관적 잠금 | `@Version` | 버전 컬럼 수동 관리 |

## 모듈

- `01-convert-jpa-basic` — 기본 CRUD, Entity 매핑, 관계(1:1, 1:N, N:M), PK/복합키
- `02-convert-jpa-advanced` — 상속(Single Table/Joined/Table Per Class), Self-Reference, Auditable, 낙관적 잠금, 서브쿼리

## 전환 전략

JPA 코드베이스 → 범위 분석 → 동등성 테스트 → 점진적 전환(모듈 단위) → CI 회귀 검증 → JPA 의존성 제거

## 실행

```bash
./gradlew :01-convert-jpa-basic:test
./gradlew :02-convert-jpa-advanced:test
```
