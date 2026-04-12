---
title: "00 Shared Test Infrastructure"
tags: [exposed, test, shared, testcontainers, h2]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 00 Shared — 공통 테스트 유틸리티

모든 모듈이 공통 의존하는 테스트 인프라. DB 연결, 테이블 생성/삭제, Faker 데이터 생성 제공.

## 핵심 클래스

- **AbstractExposedTest**: 모든 테스트의 베이스 클래스. `enableDialects()` + `@MethodSource`로 파라미터화 테스트.
- **TestDB enum**: H2, H2_MYSQL, H2_MARIADB, H2_PSQL, MARIADB, MYSQL_V8, POSTGRESQL 등 지원.
- **withTables / withTablesSuspending**: 테스트 전 테이블 생성, 후 삭제. DB 독립성 보장.
- **withDb / withDBSuspending**: 지정 DB에 연결하여 트랜잭션 블록 실행.
- **withSchemas / withSchemasSuspending**: 스키마 생성/삭제 헬퍼.

## 환경 변수

| 설정 | 설명 |
|------|------|
| `-PuseFastDB=true` | H2만 사용 (빠른 테스트) |
| `-PuseDB=H2,POSTGRESQL` | 특정 DB 지정 |

## 실행

```bash
./gradlew :exposed-shared-tests:test
./gradlew :exposed-shared-tests:test -PuseFastDB=true
```
