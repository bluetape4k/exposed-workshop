# Exposed Workshop (Kotlin Exposed 학습 자료)

[![CI](https://github.com/bluetape4k/exposed-workshop/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/exposed-workshop/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](./README.md) | 한국어

![Exposed workshop 작업대 일러스트](./docs/assets/exposed-workshop-workbench.png)

이 저장소는 Kotlin Exposed를 실행 가능한 Gradle 모듈로 익히는 워크숍입니다. 영어/한국어 README, 소스 기반 다이어그램, 데이터베이스 테스트를 함께 제공해 SQL 기초부터 운영형 서비스 패턴까지 순서대로 따라갈 수 있습니다.

## 프로젝트 목적

`exposed-workshop`은 Kotlin Exposed를 작은 실행 예제로 나눠 설명합니다. 공통 데이터베이스 fixture와 Spring Boot 진입점에서 시작해 SQL DSL, DAO, DDL/DML, 고급 컬럼 타입, JPA 마이그레이션, 코루틴, 가상 스레드, 멀티테넌시, 캐시/라우팅, Spring Boot와 Ktor 기반 운영 통합 예제로 확장됩니다.

## 제공 기능

- **소스 순서와 같은 학습 경로** — `settings.gradle.kts`와 챕터 디렉터리 구조를 그대로 따릅니다.
- **DSL, DAO, DDL, DML 예제** — 트랜잭션 경계와 dialect 차이를 테스트에서 확인합니다.
- **런타임 비교** — blocking JDBC, 코루틴, 가상 스레드, Spring MVC/WebFlux, Ktor를 같은 관점에서 비교합니다.
- **다중 데이터베이스 검증** — 빠른 H2 경로와 Testcontainers 기반 PostgreSQL, MySQL, MariaDB 옵션을 제공합니다.

## Kotlin Exposed 란?

Kotlin Exposed는 JetBrains가 만든 Kotlin 우선 SQL 프레임워크입니다. 테이블과 쿼리를 타입 안전한 DSL로 작성할 수도 있고, DAO/Entity 클래스로 행을 객체처럼 다룰 수도 있습니다. 이 워크숍은 두 방식을 실제 애플리케이션 코드와 테스트 가까이에 배치해, 어떤 상황에서 어떤 선택이 자연스러운지 비교할 수 있게 합니다.

### Exposed의 주요 특징

| 특징 | 이 저장소에서 확인하는 방식 |
|------|---------------------------|
| **타입 안전한 SQL DSL** | Table, join, predicate, CTE, function, batch DML 예제로 확인합니다. |
| **DAO와 Entity API** | EntityClass, 관계 매핑, lifecycle hook, cache 동작을 다룹니다. |
| **명시적인 트랜잭션** | blocking, coroutine, Spring `TransactionTemplate`, `@Transactional` 변형을 비교합니다. |
| **런타임 선택지** | Spring MVC/WebFlux, Ktor, 코루틴, 가상 스레드, benchmark 모듈을 제공합니다. |
| **데이터베이스 검증** | 빠른 H2 검증과 Testcontainers 기반 PostgreSQL, MySQL, MariaDB 검증을 함께 둡니다. |

![Kotlin Exposed feature map](docs/images/readme-diagrams/exposed-workshop-mindmap-01.png)

### Exposed API 구조

![Exposed API diagram](docs/images/readme-diagrams/exposed-workshop-architecture-01.png)

<!-- README_VISUAL_OVERVIEW:START -->
## Overview Diagram

![Exposed Workshop overview diagram](docs/images/readme-diagrams/root-readme-overview-01.png)

## Module Composition Chart

![Exposed Workshop module composition chart](docs/images/readme-charts/root-readme-module-chart-01.png)
<!-- README_VISUAL_OVERVIEW:END -->

## 기술 스택

| 기술 | 버전 / 설정 |
|------|-----------|
| Kotlin plugin | 2.4.0 |
| Kotlin language/API level | 2.3 |
| Java toolchain | 21 |
| Exposed | 1.3.0 |
| Spring Boot | 4.0.6 |
| Kotlinx Coroutines | 1.11.0 |
| Bluetape4k dependencies BOM | 1.3.1 |
| Gradle Wrapper | 9.5.0 |

## 학습 가이드

저장소의 소스 트리 순서대로 읽는 것이 가장 자연스럽습니다. 공통 fixture에서 시작해 Exposed 핵심 기능, 런타임 래퍼, 운영형 애플리케이션 패턴으로 넘어갑니다.

1. **공통 기반과 진입점**: 테스트 fixture, Spring MVC/WebFlux, reactive 대안 기술을 먼저 봅니다.
2. **Exposed 핵심**: SQL DSL, DAO, 스키마 정의, DML, 함수, 트랜잭션, Entity를 학습합니다.
3. **확장과 마이그레이션**: JSON, money, 암호화, 커스텀 컬럼/Entity, Jackson/Fastjson/Tink, JPA 마이그레이션을 다룹니다.
4. **런타임 모델**: 코루틴과 Java 21 가상 스레드에서 Exposed 사용 방식을 비교합니다.
5. **운영 패턴**: Spring 트랜잭션, 리포지토리, 캐시, 멀티테넌시, routing datasource, benchmark, Ktor, outbox, 인증/세션, realtime, observability 예제로 확장합니다.

### 학습 경로

![exposed workshop Architecture 2 diagram](docs/images/readme-diagrams/exposed-workshop-architecture-02.png)

## 상세 문서

모든 예제의 상세 설명은 [Kotlin Exposed Book](https://debop.notion.site/Kotlin-Exposed-Book-1ad2744526b080428173e9c907abdae2)에서 확인할 수 있습니다.

---

## 모듈 구조

![exposed workshop Architecture 3 diagram](docs/images/readme-diagrams/exposed-workshop-architecture-03.png)

## 모듈 목록

### 공유 라이브러리

#### [Exposed Shared Tests](00-shared/exposed-shared-tests/README.ko.md)

`exposed-workshop` 프로젝트 전반에서 사용되는 공통 테스트 유틸리티와 리소스를 제공합니다. 다양한 데이터베이스 환경에서 일관된 테스트를 수행할 수 있도록 지원합니다.

---

### Spring Boot 통합

#### [Spring MVC with Exposed](01-spring-boot/spring-mvc-exposed/README.ko.md)

Spring MVC + Virtual Threads + Exposed를 이용하여 동기식 REST API를 구축하는 방법을 학습합니다. 영화와 배우 데이터를 다루며 다대다 관계 매핑을 실습합니다.

#### [Spring WebFlux with Exposed](01-spring-boot/spring-webflux-exposed/README.ko.md)

Spring WebFlux + Kotlin Coroutines + Exposed를 이용하여 비동기 REST API를 구축하는 방법을 학습합니다. 반응형 프로그래밍 모델과 Exposed의 통합 방법을 익힙니다.

---

### JPA 대안 기술

#### [Hibernate Reactive Example](02-alternatives-to-jpa/hibernate-reactive-example/README.ko.md)

Hibernate Reactive를 이용한 반응형 Spring Boot 애플리케이션 구축 예제입니다.

#### [R2DBC Example](02-alternatives-to-jpa/r2dbc-example/README.ko.md)

Spring Data R2DBC를 이용한 반응형 데이터베이스 접근 예제입니다.

#### [Vert.x SQL Client Example](02-alternatives-to-jpa/vertx-sqlclient-example/README.ko.md)

Vert.x SQL Client를 이용한 이벤트 기반 비동기 데이터베이스 작업 예제입니다.

---

### Exposed 기본

#### [Exposed DAO Example](03-exposed-basic/exposed-dao-example/README.ko.md)

Exposed의 DAO(Data Access Object) 패턴을 학습합니다. Entity와 EntityClass를 사용하여 객체지향적으로 데이터베이스 작업을 수행하는 방법을 익힙니다.

#### [Exposed SQL DSL Example](03-exposed-basic/exposed-sql-example/README.ko.md)

Exposed의 SQL DSL(Domain Specific Language)을 학습합니다. 타입 안전한 SQL 쿼리 작성 방법과 DSL의 장점을 익힙니다.

---

### Exposed DDL (스키마 정의)

#### [Connection Management](04-exposed-ddl/01-connection/README.ko.md)

데이터베이스 연결 설정, 예외 처리, 타임아웃, 커넥션 풀링 등 연결 관리의 핵심 개념을 학습합니다.

#### [Schema Definition Language (DDL)](04-exposed-ddl/02-ddl/README.ko.md)

Exposed의 DDL 기능을 학습합니다. 테이블, 컬럼, 인덱스, 시퀀스 정의 방법을 익힙니다.

---

### Exposed DML (데이터 조작)

#### [DML 기본 연산](05-exposed-dml/01-dml/README.ko.md)

SELECT, INSERT, UPDATE, DELETE의 기본 패턴을 학습합니다. 조건식, 서브쿼리, 페이징, Batch Insert/Update, CTE(Common Table Expression) 등 실무에서 자주 사용하는 패턴을 익힙니다.

#### [컬럼 타입](05-exposed-dml/02-types/README.ko.md)

Exposed에서 제공하는 다양한 컬럼 타입을 학습합니다. 기본 타입부터 배열, BLOB, UUID, unsigned 수형까지 폭넓게 다룹니다.

#### [SQL 함수](05-exposed-dml/03-functions/README.ko.md)

Exposed 쿼리에서 다양한 SQL 함수를 사용하는 방법을 학습합니다. 집계 함수, 윈도우 함수, 수학/삼각 함수 등을 다룹니다.

#### [트랜잭션 관리](05-exposed-dml/04-transactions/README.ko.md)

Exposed의 트랜잭션 관리 기능을 학습합니다. 격리 수준, 중첩 트랜잭션, 롤백, 코루틴 통합 등을 다룹니다.

#### [Entity API](05-exposed-dml/05-entities/README.ko.md)

Exposed의 강력한 Entity API를 학습합니다. 다양한 기본키 전략, 관계 매핑, 라이프사이클 훅, 캐싱 등을 다룹니다.

---

### 고급 기능

#### [Exposed Crypt (투명한 컬럼 암호화)](06-advanced/01-exposed-crypt/README.ko.md)

`exposed-crypt` 확장을 사용하여 데이터베이스 컬럼을 투명하게 암호화/복호화하는 방법을 학습합니다.

#### [Exposed JavaTime (java.time 통합)](06-advanced/02-exposed-javatime/README.ko.md)

Java 8의 `java.time` API와 Exposed의 통합 방법을 학습합니다.

#### [Exposed Kotlinx-Datetime](06-advanced/03-exposed-kotlin-datetime/README.ko.md)

`kotlinx.datetime` 라이브러리와 Exposed의 통합 방법을 학습합니다. 멀티플랫폼 프로젝트에 적합합니다.

#### [Exposed Json (JSON/JSONB 지원)](06-advanced/04-exposed-json/README.ko.md)

`exposed-json` 모듈을 사용하여 JSON/JSONB 컬럼을 다루는 방법을 학습합니다.

#### [Exposed Money (금융 데이터 처리)](06-advanced/05-exposed-money/README.ko.md)

`exposed-money` 모듈을 사용하여 통화 값을 안전하게 처리하는 방법을 학습합니다.

#### [커스텀 컬럼 타입](06-advanced/06-custom-columns/README.ko.md)

사용자 정의 컬럼 타입을 구현하는 방법을 학습합니다. 암호화, 압축(GZIP/LZ4/Snappy/ZSTD), 직렬화(Kryo/Fury) 등의 투명한 변환을 구현합니다.

#### [커스텀 Entity (ID 생성 전략)](06-advanced/07-custom-entities/README.ko.md)

Snowflake, KSUID, Time-based UUID, Base62 encoded UUID 등 다양한 ID 생성 전략을 가진 커스텀 Entity를 구현합니다.

#### [Exposed Jackson (Jackson 기반 JSON)](06-advanced/08-exposed-jackson/README.ko.md)

Jackson 라이브러리를 사용하여 JSON/JSONB 컬럼을 처리하는 방법을 학습합니다.

#### [Exposed Fastjson2](06-advanced/09-exposed-fastjson2/README.ko.md)

Alibaba Fastjson2 라이브러리를 사용하여 JSON 컬럼을 처리하는 방법을 학습합니다.

#### [Exposed Jackson 3](06-advanced/11-exposed-jackson3/README.ko.md)

Jackson 3.x 버전을 사용하여 JSON/JSONB 컬럼을 처리하는 방법을 학습합니다.

#### [Exposed Tink (Google Tink 기반 컬럼 암호화)](06-advanced/12-exposed-tink/README.ko.md)

Google Tink 라이브러리를 사용하여 AEAD(비결정적) 및 DAEAD(결정적) 방식으로 컬럼 데이터를 암호화하는 방법을 학습합니다. DAEAD 방식은 암호화된 상태로 WHERE 절 검색이 가능합니다.

---

### JPA 마이그레이션

#### [JPA 기본 기능 변환](07-jpa/01-convert-jpa-basic/README.ko.md)

JPA의 기본 기능을 Exposed로 구현하는 방법을 학습합니다. Entity, 연관관계(One-to-One, One-to-Many, Many-to-Many), 기본키, 복합키 등을 다룹니다.

#### [JPA 고급 기능 변환](07-jpa/02-convert-jpa-advanced/README.ko.md)

JPA의 고급 기능을 Exposed로 구현하는 방법을 학습합니다. 상속 매핑(Single Table, Table Per Class, Joined Table), Self-Reference, Auditable, 낙관적 잠금 등을 다룹니다.

---

### 코루틴 & 가상 스레드

#### [Coroutines 기본](08-coroutines/01-coroutines-basic/README.ko.md)

Exposed를 Kotlin Coroutines 환경에서 사용하는 방법을 학습합니다.
`newSuspendedTransaction`, `suspendedTransactionAsync` 등을 다룹니다.

#### [Virtual Threads 기본](08-coroutines/02-virtualthreads-basic/README.ko.md)

Exposed를 Java 21 Virtual Threads 환경에서 사용하는 방법을 학습합니다. 블로킹 코드 스타일을 유지하면서 고성능 비동기 처리를 구현합니다.

---

### Spring 통합

#### [Spring Boot AutoConfiguration](09-spring/01-springboot-autoconfigure/README.ko.md)

Spring Boot의 자동 설정을 활용하여 Exposed를 설정하는 방법을 학습합니다.

#### [TransactionTemplate 활용](09-spring/02-transactiontemplate/README.ko.md)

Spring의 `TransactionTemplate`으로 프로그래밍 방식 트랜잭션을 관리하는 방법을 학습합니다.

#### [Spring Transaction 통합](09-spring/03-spring-transaction/README.ko.md)

`@Transactional` 어노테이션으로 선언적 트랜잭션을 관리하는 방법을 학습합니다.

#### [ExposedRepository (동기)](09-spring/04-exposed-repository/README.ko.md)

Spring Data Repository 패턴을 적용한 Exposed 리포지토리 구현 방법을 학습합니다.

#### [ExposedRepository (코루틴)](09-spring/05-exposed-repository-coroutines/README.ko.md)

코루틴 환경에서 Repository 패턴을 사용하여 비동기 데이터 접근을 구현합니다.

#### [Spring Boot Cache](09-spring/06-spring-cache/README.ko.md)

Spring Boot Cache를 Exposed와 함께 사용하는 방법을 학습합니다.

#### [Suspended Cache](09-spring/07-spring-suspended-cache/README.ko.md)

Lettuce를 활용한 Suspended Cache를 코루틴 환경에서 Exposed와 함께 사용하는 방법을 학습합니다.

---

### 멀티테넌시

#### [Spring Web + Multitenant](10-multi-tenant/01-multitenant-spring-web/README.ko.md)

Spring Web Application에서 Schema-based Multi-tenancy를 구현하는 방법을 학습합니다.

#### [Spring Web + VirtualThreads + Multitenant](10-multi-tenant/02-multitenant-spring-web-virtualthread/README.ko.md)

Virtual Threads 환경에서 멀티테넌시를 구현하는 방법을 학습합니다.

#### [Spring WebFlux + Multitenant](10-multi-tenant/03-multitenant-spring-webflux/README.ko.md)

WebFlux와 Coroutines를 이용하여 반응형 멀티테넌시를 구현하는 방법을 학습합니다.

#### [Schema-per-Tenant Spring Web](10-multi-tenant/04-schema-per-tenant-spring-web/README.ko.md)

하나의 shared Hikari pool을 사용하면서 Exposed 트랜잭션을 허용된 tenant schema로 전환하고 connection을 안전하게 reset하는 방법을 학습합니다.

#### [Database-per-Tenant Spring Web](10-multi-tenant/05-database-per-tenant-spring-web/README.ko.md)

허용된 tenant를 전용 Hikari pool과 Exposed database로 라우팅하고 기본 datasource fallback을 두지 않는 방법을 학습합니다.

#### [Spring Security Tenant Authorization Spring Web](10-multi-tenant/06-spring-security-tenant-authorization-spring-web/README.ko.md)

JWT, API key, demo session identity에서 요청 tenant를 인가한 뒤 tenant database로 라우팅하는 방법을 학습합니다.

---

### 고성능

#### [캐시 전략 (동기)](11-high-performance/01-cache-strategies/README.ko.md)

Redisson + Exposed로 다양한 캐시 전략(Read Through, Write Through, Write Behind)을 구현합니다.

#### [캐시 전략 (코루틴)](11-high-performance/02-cache-strategies-coroutines/README.ko.md)

코루틴 환경에서 비동기로 작동하는 캐시 전략을 구현합니다.

#### [RoutingDataSource 구성](11-high-performance/03-routing-datasource/README.ko.md)

Multi-Tenant 또는 Read Replica 구조를 위한 유연한 DataSource 라우팅 구성 방법을 학습합니다.

#### [벤치마크](11-high-performance/04-benchmark/README.ko.md)

`kotlinx-benchmark` 기반 마이크로벤치마크로 캐시/라우팅 예제의 성능을 측정합니다. smoke 프로파일과 main 프로파일을 제공하며 Markdown 리포트를 생성할 수 있습니다.

### 운영 통합

#### [Chapter 12 개요](12-production-integration/README.ko.md)

Exposed 기반 운영형 서비스 패턴을 Spring Boot 4와 Ktor로 비교합니다.

#### [Ktor 애플리케이션 아키텍처](12-production-integration/01-ktor-application-architecture/README.ko.md)

명시적 라우팅, 서비스 검증, 정제된 오류, Exposed JDBC 저장소를 갖춘 작은 Ktor 서비스를 구현합니다.

#### [Spring Boot 애플리케이션 아키텍처](12-production-integration/02-spring-application-architecture/README.ko.md)

MVC 컨트롤러, 컨트롤러 advice, 서비스 검증, Exposed JDBC 저장소를 갖춘 Spring Boot 4 쌍을 구현합니다.

#### [Spring HTTP 아웃박스와 멱등성](12-production-integration/03-spring-http-outbox-idempotency/README.ko.md)

외부 HTTP 결제 요청을 전송 전에 저장하고, 멱등성 키로 중복을 방지하며, Spring Boot 4에서 재시도 가능한 gateway 실패와 영구 실패를 분리합니다.

#### [Ktor HTTP 아웃박스와 멱등성](12-production-integration/04-ktor-http-outbox-idempotency/README.ko.md)

suspend route handler, `Dispatchers.IO` 뒤의 Exposed JDBC 저장소, 성공/재시도/중복/영구 실패를 검증하는 fake-gateway 테스트를 갖춘 Ktor 쌍을 구현합니다.

#### [Spring 인증 세션](12-production-integration/05-spring-auth-session/README.ko.md)

credential 검증, Exposed 기반 사용자/세션, structured error, session invalidation 테스트를 포함한 Spring Boot 4 cookie-session 흐름을 구현합니다.

#### [Ktor 인증 세션](12-production-integration/06-ktor-auth-session/README.ko.md)

명시적 auth route, Exposed 기반 persistence, cookie 처리, login/current-user/logout 동작을 검증하는 `testApplication` 테스트를 갖춘 Ktor 쌍을 구현합니다.

#### [Spring 리얼타임 아웃박스](12-production-integration/07-spring-outbox-realtime/README.ko.md)

database outbox로 notification event를 발행하고 server-sent events를 노출하며, Spring Boot 4에서 duplicate-safe dispatch를 검증합니다.

#### [Ktor 리얼타임 아웃박스](12-production-integration/08-ktor-outbox-realtime/README.ko.md)

suspend dispatch, SSE streaming, realtime notification을 위한 Exposed 기반 outbox state transition을 갖춘 Ktor 쌍을 구현합니다.

#### [Spring 관측성/준비 상태](12-production-integration/09-spring-observability-readiness/README.ko.md)

Spring Boot 4 서비스의 Actuator readiness, request correlation, structured error, slow-operation diagnostics를 보여줍니다.

#### [Ktor 관측성/준비 상태](12-production-integration/10-ktor-observability-readiness/README.ko.md)

명시적 `/readyz`, sanitize된 `X-Request-ID`, structured `StatusPages`, Exposed 기반 diagnostics를 갖춘 Ktor 쌍을 구현합니다.

---

### Ecosystem Integrations

#### [Chapter 13 개요](13-ecosystem-integrations/README.ko.md)

이슈 [#137](https://github.com/bluetape4k/exposed-workshop/issues/137) 아래에서 데이터베이스
플랫폼, Ktor, Spring Modulith, DDD 중심의 Exposed 1.11 ecosystem 예제를 추적합니다.

#### [BigQuery Dry-Run Query Validation](13-ecosystem-integrations/01-bigquery-dry-run/README.ko.md)

H2 SQL generation과 mocked BigQuery REST response를 사용해 Exposed가 생성한 분석용
query의 BigQuery dry-run request mapping을 검증합니다.

#### [Trino Session Options and Pushdown Verification](13-ecosystem-integrations/02-trino-session-options/README.ko.md)

검증된 Trino session 설정을 `TrinoConnectionOptions`로 매핑하고, warehouse query를
로컬에서 생성한 뒤 이후 connector-specific pushdown 점검에 사용할 `EXPLAIN` request
shape를 준비합니다.

#### [CockroachDB Serializable Retry](13-ecosystem-integrations/03-cockroachdb-retry/README.ko.md)

`CockroachDatabase`로 연결하고, inventory reservation을 `withCockroachTransaction`으로
감싼 뒤, CockroachDB Testcontainers로 전체 transaction retry 동작을 검증합니다.

#### [StarRocks Local-First OLAP](13-ecosystem-integrations/04-starrocks-olap-local/README.ko.md)

`StarRocksTable`로 StarRocks OLAP rollup DDL을 모델링하고, projection SQL과
aggregation 테스트는 H2로 local deterministic하게 유지하며, 실제 StarRocks validation
경계를 문서화합니다.

#### [Explicit Ktor Exposed Integration](13-ecosystem-integrations/05-ktor-exposed-integration/README.ko.md)

Ktor 애플리케이션에 `bluetape4k-exposed-ktor`를 설치하고, CRUD route는 helper-backed
JDBC transaction으로 실행하며, helper readiness와 sanitized database error를 local H2
JDBC/R2DBC 리소스로 검증합니다.

#### [Spring Modulith Publication Store with Exposed](13-ecosystem-integrations/06-spring-modulith-publications/README.ko.md)

Spring Modulith 모듈 이벤트 publication을 Exposed 기반 bluetape4k repository로
저장하고, deterministic H2 테스트로 completion, retry, unloadable publication 처리를
검증합니다.

---

## exposed-r2dbc-workshop 예제 parity

이슈 [#99](https://github.com/bluetape4k/exposed-workshop/issues/99)는
[`exposed-r2dbc-workshop`](https://github.com/bluetape4k/exposed-r2dbc-workshop)과의 개념 수준 parity를 추적합니다.
정확한 모듈명 일치가 목적은 아니며, JDBC/blocking과 R2DBC의 데이터베이스 API 모델이 다를 때는 서로 다른 아키텍처 선택을 유지합니다.
R2DBC 쪽 대응 추적 이슈는
[exposed-r2dbc-workshop#89](https://github.com/bluetape4k/exposed-r2dbc-workshop/issues/89)입니다(닫힘; 이 결정을 반영).

| `exposed-workshop` 주제 | R2DBC 대응 항목 | 결정 |
|------------------------|----------------|------|
| Ktor 예제 epic `#45` 및 멀티테넌트 `#46` | 닫힌 R2DBC 이슈 `#32`, `#33`; 모듈 `10-multi-tenant/07-multitenant-ktor` | 대응 항목으로 충족 |
| Ktor 캐시/라우팅 구현 이슈 `#47`, `#48`, `#49` 및 wiring `#50`; JDBC 모듈 `11-high-performance/05-07-*` | 닫힌 R2DBC 이슈 `#34`, `#35`, `#36`, `#69`; R2DBC 모듈 `11-high-performance/04-06-*` | 대응 항목으로 충족; `#50`은 문서 wiring |
| Spring Boot 테넌트 전략 epic `#51`, 구현 `#52`-`#55`, wiring `#56`; JDBC 모듈 `10-multi-tenant/04-06-*`, `08-tenant-onboarding-spring-web` | 닫힌 R2DBC 이슈 `#37`-`#42`; R2DBC 모듈 `10-multi-tenant/03-06-*` | 대응 항목으로 충족; `#56`은 문서 wiring |
| Chapter 12 production integration epic `#57` 및 분리 모듈 `12-production-integration/01-10-*` | 닫힌 R2DBC 이슈 `#43`-`#49`; 통합 모듈 `12-production-integration/01-spring-production-integration`, `02-ktor-production-integration` | 대응 항목으로 충족 |
| R2DBC connection-factory-per-tenant | 닫힌 R2DBC 이슈 `#39`; JDBC는 database-per-tenant `#53` 및 schema-per-tenant `#52` 사용 | 플랫폼 특화, 중복 이슈 없음 |
| JDBC DAO/entities, transaction template, Spring cache, benchmark | `03-exposed-basic`, `05-exposed-dml`, `09-spring`, `11-high-performance`의 blocking/JDBC 전용 모듈 | 플랫폼 특화, 중복 이슈 없음 |

2026-05-24 기준 남은 차이는 플랫폼 특화 항목이므로 새 후속 이슈는 만들지 않습니다.

---

## 시작하기

### 사전 요구사항

- JDK 21 이상 (Virtual Threads 및 Preview 기능 사용)
- Gradle Wrapper 9.5.0 포함 (`./gradlew` 사용 권장)
- Docker (Testcontainers 사용 시)

### 빠른 시작

```bash
# 빠른 로컬 검증 (H2만 사용)
./gradlew test -PuseFastDB=true

# 전체 프로젝트 빌드 및 테스트
./gradlew clean build

# 특정 모듈 테스트 실행
./gradlew :03-routing-datasource:test
./gradlew :01-dml:test
./gradlew :spring-mvc-exposed:test
```

루트 `settings.gradle.kts` 에서 Gradle project path 를 모듈의 마지막 디렉터리 이름으로 생성하므로, 경로가 헷갈리면 `./gradlew projects` 로 확인하세요.

### 테스트 대상 DB 선택

기본값은 **H2, PostgreSQL, MySQL V8** 3가지를 대상으로 테스트합니다. Gradle 프로퍼티로 테스트 범위를 조절할 수 있습니다.

```bash
# H2 만 테스트 (빠른 로컬 개발용)
./gradlew test -PuseFastDB=true

# 특정 DB만 지정해서 테스트
./gradlew test -PuseDB=H2,POSTGRESQL
./gradlew test -PuseDB=H2,POSTGRESQL,MYSQL_V8,MARIADB

# 기본값으로 테스트 (H2 + PostgreSQL + MySQL V8)
./gradlew test
```

`-PuseDB`에 사용 가능한 값 (`TestDB` enum 이름):

| 값               | 설명                               |
|-----------------|----------------------------------|
| `H2`            | H2 (인메모리, 기본 모드)                 |
| `H2_V1`         | H2 1.x 호환 모드                     |
| `H2_MYSQL`      | H2 (MySQL 호환 모드)                 |
| `H2_MARIADB`    | H2 (MariaDB 호환 모드)               |
| `H2_PSQL`       | H2 (PostgreSQL 호환 모드)            |
| `MARIADB`       | MariaDB (Testcontainers)         |
| `MYSQL_V5`      | MySQL 5.x (Testcontainers)       |
| `MYSQL_V8`      | MySQL 8.x (Testcontainers)       |
| `POSTGRESQL`    | PostgreSQL (Testcontainers)      |
| `POSTGRESQLNG`  | PostgreSQL NG 드라이버               |
| ~~`COCKROACH`~~ | ~~CockroachDB (Testcontainers)~~ |

> [!NOTE]
> 우선순위: `-PuseDB` > `-PuseFastDB` > 기본값 (H2, POSTGRESQL, MYSQL_V8)

### 개발 환경

- 저장소에 포함된 Gradle Wrapper(`./gradlew`) 사용을 권장합니다.
- IntelliJ IDEA에서 열면 멀티 모듈이 자동 인식됩니다.
- Docker가 있으면 Testcontainers 기반 PostgreSQL/MySQL/Redis 테스트를 그대로 실행할 수 있습니다.

## 기여하기

이 프로젝트는 학습 목적으로 제작되었습니다. 오타 수정, 예제 추가, 번역 개선 등 모든 기여를 환영합니다.

## 라이선스

MIT License
