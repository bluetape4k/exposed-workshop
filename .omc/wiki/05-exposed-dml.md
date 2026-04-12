---
title: "05 Exposed DML"
tags: [exposed, dml, select, insert, update, delete, transaction, entity, functions, types]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 05 Exposed DML

SELECT/INSERT/UPDATE/DELETE, 타입, 함수, 트랜잭션, Entity API.

## 모듈

### 01-dml
기본 DML + JOIN/UPSERT/CTE/MERGE/LATERAL JOIN/RETURNING.

DB별 지원: DISTINCT ON (PG/H2), RETURNING (PG/H2), MERGE (PG/H2), REPLACE (MySQL/MariaDB), LATERAL JOIN (PG/MySQL).

### 02-types
컬럼 타입: bool, char, integer, double, array (PG/H2), multiArray (PG), UUID (Java/Kotlin), unsigned, blob.

### 03-functions
문자열/수학/집계/통계/삼각/윈도우/조건/비트 함수.
윈도우: `rowNumber`, `rank`, `denseRank`, `lead`, `lag` + `over().partitionBy().orderBy()`.

### 04-transactions
격리 수준, 중첩 트랜잭션(Savepoint), 롤백, 코루틴 트랜잭션(`newSuspendedTransaction`, `suspendedTransactionAsync`).

### 05-entities
Entity/EntityClass CRUD, PK 전략(Long/Int/UUID/Kotlin UUID/Composite), 관계(referencedOn/referrersOn/via), EntityHook 감사 패턴, Self-Reference.

## 실행

```bash
./gradlew :01-dml:test
./gradlew :02-types:test
./gradlew :03-functions:test
./gradlew :04-transactions:test
./gradlew :05-entities:test
```
