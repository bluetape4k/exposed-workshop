# Exposed Workshop Wiki Index

## Overview
- [Exposed Workshop Overview](exposed-workshop-overview.md) — 전체 구조, 기술 스택, 학습 순서, 빌드 명령

## Chapters

### 00 Shared
- [00 Shared Test Infrastructure](00-shared-test-infra.md) — AbstractExposedTest, TestDB, withTables 등 공통 테스트 유틸

### 01 Spring Boot
- [01 Spring Boot with Exposed](01-spring-boot-exposed.md) — Spring MVC + WebFlux REST API, Virtual Threads vs Coroutines

### 02 Alternatives to JPA
- [02 Alternatives to JPA](02-alternatives-to-jpa.md) — Hibernate Reactive, R2DBC, Vert.x SQL Client 비교

### 03 Exposed Basic
- [03 Exposed Basic](03-exposed-basic.md) — DSL vs DAO 패턴 비교, City/User 도메인 CRUD

### 04 Exposed DDL
- [04 Exposed DDL](04-exposed-ddl.md) — 연결 관리, 스키마 정의, 인덱스, 시퀀스, 커스텀 Enum

### 05 Exposed DML
- [05 Exposed DML](05-exposed-dml.md) — DML 기본, 컬럼 타입, SQL 함수, 트랜잭션, Entity API

### 06 Advanced
- [06 Advanced](06-advanced.md) — 암호화, JSON, 날짜/시간, 커스텀 컬럼/엔티티 (12개 서브모듈)

### 07 JPA Migration
- [07 JPA Migration](07-jpa-migration.md) — JPA → Exposed 전환 전략, 관계/상속 매핑

### 08 Coroutines
- [08 Coroutines & Virtual Threads](08-coroutines.md) — 코루틴 vs Virtual Thread 동시성 모델 비교

### 09 Spring Integration
- [09 Spring Integration](09-spring-integration.md) — 트랜잭션, Repository, Cache (7개 서브모듈)

### 10 Multi-Tenant
- [10 Multi-Tenant](10-multi-tenant.md) — Schema 기반 멀티테넌시, MVC/VT/WebFlux 3환경 비교

### 11 High Performance
- [11 High Performance](11-high-performance.md) — 캐시 전략, DataSource 라우팅, 벤치마크

## References
- [JetBrains Exposed Official Manual](jetbrains-exposed-manual.md) — 공식 문서 요약: DSL/DAO API, 트랜잭션, 데이터 타입, Spring Boot 통합, 모듈 구성

## External Libraries
- [bluetape4k-exposed Modules](bluetape4k-exposed-modules.md) — 모듈 카탈로그 (core, jdbc, r2dbc, cache, json, encryption, postgresql, trino)

## Other
- [Memory Hybrid Enforcement](memory-hybrid-enforcement.md) — 기존 메모
