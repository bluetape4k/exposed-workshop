---
title: "04 Exposed DDL"
tags: [exposed, ddl, connection, schema, index, sequence]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 04 Exposed DDL

DB 연결 관리와 스키마 정의(DDL).

## 모듈

### 01-connection
- `Database.connect()` (URL/DataSource)
- HikariCP 풀 고갈 복구 (21개 비동기 트랜잭션 동시 실행)
- 다중 DB 중첩 트랜잭션 격리
- 연결 예외 재시도 (`maxAttempts`)
- 타임아웃 우선순위 (트랜잭션 블록 > DatabaseConfig)

### 02-ddl
- 테이블/컬럼/제약조건 정의 (Table, IntIdTable, UUIDTable, CompositeIdTable)
- 복합 PK/FK, 조건부/함수형 인덱스
- 시퀀스, 커스텀 Enum (`enumerationByName` vs `customEnumeration`)
- `SchemaUtils.create()`, `createMissingTablesAndColumns()`, `drop()`
- DB별 DDL 차이 (PostgreSQL partial index, MySQL INVISIBLE 컬럼)

## 실행

```bash
./gradlew :01-connection:test
./gradlew :02-ddl:test
```
