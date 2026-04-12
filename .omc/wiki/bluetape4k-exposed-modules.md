---
title: "bluetape4k-exposed Modules"
tags: [bluetape4k, exposed, library, jdbc, r2dbc, cache, json, encryption]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# bluetape4k-exposed Modules

bluetape4k-exposed 라이브러리 모듈 카탈로그.

## Module Overview Table

| Module | Description | Key Features |
|--------|-------------|--------------|
| `bluetape4k-exposed` | Umbrella (core + dao + jdbc) | 하위 호환, 단일 의존성 |
| `bluetape4k-exposed-core` | 핵심 컬럼 타입, 확장 함수 (JDBC 불필요) | 압축/암호화/직렬화 컬럼, ID 생성, 네트워크/전화번호 타입 |
| `bluetape4k-exposed-jdbc` | JDBC Repository, 트랜잭션, 쿼리 확장 | JdbcRepository, SoftDelete, VirtualThread, SuspendedQuery |
| `bluetape4k-exposed-r2dbc` | R2DBC Repository, Flow/suspend API | R2dbcRepository, Flow 조회, ImplicitQuery, 비동기 메타데이터 |
| `bluetape4k-exposed-jdbc-tests` | JDBC 테스트 인프라 | AbstractExposedTest, withTables, TestDB, Testcontainers |
| `bluetape4k-exposed-r2dbc-tests` | R2DBC 테스트 인프라 | AbstractExposedR2dbcTest, withTables, suspend 네이티브 |
| `bluetape4k-exposed-cache` | 캐시 인터페이스 허브 | JdbcCacheRepository, R2dbcCacheRepository, 백엔드 독립 |
| `bluetape4k-exposed-jdbc-caffeine` | JDBC + Caffeine 로컬 캐시 | Read-through, Write-behind, sync + suspend |
| `bluetape4k-exposed-r2dbc-caffeine` | R2DBC + Caffeine 로컬 캐시 | AsyncCache, suspend 전용, Write-behind |
| `bluetape4k-exposed-jdbc-lettuce` | JDBC + Lettuce Redis 캐시 | Read/Write-through, Write-behind, MapLoader/MapWriter |
| `bluetape4k-exposed-r2dbc-lettuce` | R2DBC + Lettuce Redis 캐시 | 코루틴 네이티브, NearCache 2-tier, suspend 전용 |
| `bluetape4k-exposed-jdbc-redisson` | JDBC + Redisson Redis 캐시 | MapLoader/MapWriter, Near Cache, Write-Behind |
| `bluetape4k-exposed-r2dbc-redisson` | R2DBC + Redisson Redis 캐시 | AsyncMapLoader/Writer, 코루틴 네이티브, Near Cache |
| `bluetape4k-exposed-jackson` | Jackson 2 JSON/JSONB 컬럼 | `jackson()`, `jacksonb()`, JSON 함수/조건식 |
| `bluetape4k-exposed-jackson3` | Jackson 3 JSON/JSONB 컬럼 | `jackson()`, `jacksonb()`, Jackson 3.x 지원 |
| `bluetape4k-exposed-fastjson2` | Fastjson2 JSON/JSONB 컬럼 | `fastjson()`, `fastjsonb()`, 고성능 JSON |
| `bluetape4k-exposed-jasypt` | Jasypt 결정적 암호화 | `jasyptVarChar()`, `jasyptBinary()`, 검색/인덱스 가능 |
| `bluetape4k-exposed-postgresql` | PostgreSQL 전용 확장 | PostGIS, pgvector, tstzrange |
| `bluetape4k-exposed-trino` | Trino JDBC 통합 | TrinoDialect, suspendTransaction, queryFlow, TrinoTable |

## Core Modules

### bluetape4k-exposed (Umbrella)

`bluetape4k-exposed-core`, `bluetape4k-exposed-dao`, `bluetape4k-exposed-jdbc` 세 모듈을 하나로 묶는 하위 호환 Umbrella 모듈. 기존 코드는 변경 없이 동작하며, 신규 프로젝트에서는 하위 모듈 직접 참조를 권장한다.

```text
bluetape4k-exposed  (umbrella)
├── bluetape4k-exposed-core   <- 핵심 컬럼 타입, 확장 함수 (JDBC 불필요)
├── bluetape4k-exposed-dao    <- DAO 엔티티, ID 테이블 전략
└── bluetape4k-exposed-jdbc   <- JDBC Repository, 트랜잭션, 쿼리 확장
```

```kotlin
dependencies {
    // 기존 코드 하위 호환
    implementation("io.github.bluetape4k:bluetape4k-exposed:${version}")
    // 또는 필요한 하위 모듈만
    implementation("io.github.bluetape4k:bluetape4k-exposed-core:${version}")
}
```

### bluetape4k-exposed-core

JDBC 의존 없이 사용 가능한 기반 모듈. R2DBC, 직렬화, 암호화 등 상위 모듈에서 공유된다.

**Key classes/functions:**
- 압축 컬럼: `compressedBinary()`, `compressedBlob()` (LZ4/Snappy/Zstd)
- 암호화 컬럼: `encryptedVarChar()`, `encryptedBinary()` (AES)
- 직렬화 컬럼: `binarySerializedBinary()` (Kryo/Fory)
- 네트워크 컬럼: `inetAddress()`, `cidr()`, `isContainedBy()`
- 전화번호 컬럼: `phoneNumber()`, `phoneNumberString()` (E.164)
- ID 생성: `timebasedGenerated()`, `snowflakeGenerated()`, `ksuidGenerated()`, `ulidGenerated()`
- 배치: `BatchInsertOnConflictDoNothing`
- 공통 인터페이스: `HasIdentifier<ID>`, `ExposedPage<T>`

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-core:${version}")
}
```

### bluetape4k-exposed-jdbc

JDBC 계층 Repository 패턴, 트랜잭션 확장, 쿼리 유틸리티.

**Key classes/functions:**
- `JdbcRepository<ID, T, E>` / `LongJdbcRepository` -- CRUD, 페이징, 배치 삽입/Upsert
- `SoftDeletedJdbcRepository<ID, T, E>` -- Soft Delete 지원
- `SuspendedQuery` -- suspend 함수로 JDBC 쿼리 실행
- `virtualThreadTransaction { }` -- JDK 21+ Virtual Thread 트랜잭션
- `ImplicitSelectAll`, `TableExtensions`, `SchemaUtilsExtensions`

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc:${version}")
}
```

### bluetape4k-exposed-r2dbc

R2DBC 비동기/반응형 데이터베이스 작업. Kotlin Coroutines와 완벽하게 호환.

**Key classes/functions:**
- `R2dbcRepository<ID, T, E>` / `LongR2dbcRepository` -- Flow 기반 조회, suspend CRUD
- `SoftDeletedR2dbcRepository<ID, T, E>` -- Soft Delete
- `virtualThreadTransaction { }` -- Java 21 Virtual Thread R2DBC 트랜잭션
- `ImplicitQuery` / `FieldSet.selectImplicitAll()` -- `SELECT *` SQL 생성
- `Table.suspendColumnMetadata()`, `suspendIndexes()` -- 비동기 메타데이터 API
- `Readable.getString`, `Readable.getLong` 등 타입 안전 확장 함수

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc:${version}")
    implementation("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")
}
```

## Test Infrastructure

### bluetape4k-exposed-jdbc-tests

Exposed JDBC 기반 모듈의 공통 테스트 인프라. H2, MySQL, MariaDB, PostgreSQL 통합 테스트를 지원한다.

**Key classes/functions:**
- `AbstractExposedTest` -- DB 테스트 기본 구조
- `withTables(testDB, vararg tables)` -- 테이블 자동 생성/삭제
- `withDb(testDB)` -- 테이블 없이 DB 연결
- `withTablesSuspending()` -- 코루틴 환경 테스트
- `TestDB` enum: `H2`, `H2_MYSQL`, `H2_MARIADB`, `H2_PSQL`, `MARIADB`, `MYSQL_V5`, `MYSQL_V8`, `POSTGRESQL`
- `ENABLE_DIALECTS_METHOD` -- `@MethodSource` 파라미터

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-exposed-tests:${version}")
}
```

### bluetape4k-exposed-r2dbc-tests

Exposed R2DBC 기반 모듈의 공통 테스트 인프라. 모든 테스트가 suspend 함수 기반.

**Key classes/functions:**
- `AbstractExposedR2dbcTest` -- R2DBC 테스트 기본 구조
- `withTables(testDB, vararg tables)` -- suspend 테이블 생성/삭제
- `withDb(testDB)` -- suspend DB 연결
- `TestDB` enum: H2 계열, `MARIADB`, `MYSQL_V8`, `POSTGRESQL`

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-tests:${version}")
}
```

## Cache Modules

### Architecture

`bluetape4k-exposed-cache`가 캐시 백엔드에 독립적인 공통 인터페이스를 정의하고, 각 캐시 모듈이 구현한다.

| Module | Cache Backend | Cache Mode | DB Access | Suspend |
|--------|--------------|------------|-----------|---------|
| `exposed-jdbc-caffeine` | Caffeine (local) | LOCAL | JDBC | sync + suspend |
| `exposed-r2dbc-caffeine` | Caffeine (local) | LOCAL | R2DBC | suspend only |
| `exposed-jdbc-lettuce` | Redis (Lettuce) | REMOTE / NEAR_CACHE | JDBC | sync + suspend |
| `exposed-r2dbc-lettuce` | Redis (Lettuce) | REMOTE | R2DBC | suspend only |
| `exposed-jdbc-redisson` | Redis (Redisson) | REMOTE / NEAR_CACHE | JDBC | sync + suspend |
| `exposed-r2dbc-redisson` | Redis (Redisson) | REMOTE | R2DBC | suspend only |

### bluetape4k-exposed-jdbc-caffeine / r2dbc-caffeine

Caffeine 로컬(인프로세스) 캐시를 사용하는 Exposed 저장소. Redis 의존 없이 `exposed-cache` 인터페이스만 사용한다.

**Key classes:**
- JDBC: `AbstractJdbcCaffeineRepository`, `AbstractSuspendedJdbcCaffeineRepository`
- R2DBC: `AbstractR2dbcCaffeineRepository`
- Read-through, Write-through, Write-behind 패턴 지원
- `LocalCacheConfig` 설정

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-caffeine:${version}")
    // or
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-caffeine:${version}")
}
```

### bluetape4k-exposed-jdbc-lettuce / r2dbc-lettuce

Lettuce Redis 캐시를 결합한 Read-through / Write-through / Write-behind 캐시 레포지토리.

**Key classes:**
- JDBC: `AbstractJdbcLettuceRepository`, `AbstractSuspendedJdbcLettuceRepository`
- R2DBC: `AbstractR2dbcLettuceRepository`
- `LettuceCacheConfig.READ_WRITE_THROUGH`
- `MapLoader` / `MapWriter` -- Lettuce `LettuceLoadedMap` 연동
- NearCache (R2DBC): Caffeine front + Redis back 2-tier

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-lettuce:${version}")
    // or
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-lettuce:${version}")
}
```

### bluetape4k-exposed-jdbc-redisson / r2dbc-redisson

Redisson Redis 캐시를 결합한 Read-Through / Write-Through 캐시 레포지토리.

**Key classes:**
- JDBC: `AbstractJdbcRedissonRepository`, `AbstractSuspendedJdbcRedissonRepository`
- R2DBC: `AbstractR2dbcRedissonRepository`
- `RedisCacheConfig` 설정
- `MapLoader` / `MapWriter` / `AsyncMapLoader` / `AsyncMapWriter` -- Redisson 연동
- Near Cache, Write-Behind 지원
- Entity must implement `java.io.Serializable`

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc-redisson:${version}")
    implementation("org.redisson:redisson:3.37.0")
    // or
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-redisson:${version}")
}
```

## JSON Serialization

### bluetape4k-exposed-jackson

Jackson 2.x 기반 JSON/JSONB 컬럼 직렬화/역직렬화.

**Key functions:** `jackson<T>()`, `jacksonb<T>()`, JSON 함수/조건식

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jackson:${version}")
}
```

### bluetape4k-exposed-jackson3

Jackson 3.x 기반 JSON/JSONB 컬럼. Jackson 3의 새 기능과 개선된 성능을 활용한다.

**Key functions:** `jackson<T>()`, `jacksonb<T>()` (jackson3 패키지)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jackson3:${version}")
}
```

### bluetape4k-exposed-fastjson2

Alibaba Fastjson2 기반 JSON/JSONB 컬럼. 고성능 JSON 처리가 필요한 환경에 적합하다.

**Key functions:** `fastjson<T>()`, `fastjsonb<T>()`

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-fastjson2:${version}")
}
```

## Encryption

### bluetape4k-exposed-jasypt

Jasypt 라이브러리를 통한 결정적 암호화(Deterministic Encryption) 컬럼. 동일 평문은 항상 동일 암호문으로 변환되어 검색과 인덱스 활용이 가능하다.

**Key functions:** `jasyptVarChar()`, `jasyptBinary()`

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jasypt:${version}")
    implementation("io.github.bluetape4k:bluetape4k-crypto:${version}")
}
```

> Note: `bluetape4k-exposed-core`에도 `encryptedVarChar()`, `encryptedBinary()` (AES)가 있다. Jasypt는 결정적 암호화가 필요할 때, core의 암호화는 일반적인 AES 암호화가 필요할 때 사용한다.

## Database-specific

### bluetape4k-exposed-postgresql

PostgreSQL 전용 Exposed 확장. PostGIS 공간 데이터, pgvector 벡터 검색, TSTZRANGE 시간 범위 컬럼 타입을 제공한다.

**Key functions/types:**
- PostGIS: `geoGeometry()`, `geoPoint()`, `geoPolygon()` -- `PGgeometry` 타입
- pgvector: `pgvector(name, dim)` -- `FloatArray` 벡터 컬럼, 유사도 검색
- tstzrange: `tstzrange()` -- `ClosedRange<Instant>` 시간 범위 컬럼

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-postgresql:${version}")
}
```

### bluetape4k-exposed-trino

JetBrains Exposed ORM과 Trino JDBC 통합. PostgreSQL Dialect 기반으로 Trino에서 Exposed DSL을 사용하고, 코루틴 기반 suspend 트랜잭션과 Flow 쿼리를 제공한다.

**Key classes/functions:**
- `TrinoDialect` -- PostgreSQLDialect 상속, Trino 호환
- `TrinoDatabase.connect()` -- 호스트/포트/카탈로그/스키마 또는 JDBC URL 연결
- `TrinoConnectionWrapper` -- autoCommit=true 고정 래퍼
- `TrinoTable` -- unsupported PRIMARY KEY / NULL 구문 제거
- `suspendTransaction { }` -- Dispatchers.IO에서 블로킹 JDBC를 suspend로 래핑
- `queryFlow { }` -- 결과를 materialize 후 `Flow<T>`로 emit

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-trino:${version}")
}
```
