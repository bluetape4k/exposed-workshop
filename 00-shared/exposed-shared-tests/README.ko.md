# exposed-shared-tests — 공유 Exposed 테스트 Fixture

[English](./README.md) | 한국어

`exposed-shared-tests`는 워크샵 예제가 함께 사용하는 fixture 모듈입니다.
각 챕터 테스트가 같은 방식으로 데이터베이스 Dialect를 선택하고, Exposed 트랜잭션을 열고, 테이블과 스키마를 생성/삭제하며, repository 검증용 샘플 데이터를 준비할 수 있게 합니다.

## 목표

- 개별 챕터 테스트에서 데이터베이스 선택과 Testcontainers 설정을 반복하지 않게 합니다.
- blocking 트랜잭션과 suspending 트랜잭션 패턴을 같은 모양으로 제공합니다.
- 테이블, 스키마, DAO 엔티티, repository fixture를 여러 챕터에서 재사용합니다.
- downstream 모듈이 의존하기 전에 fixture 헬퍼를 실제 Exposed 테이블로 먼저 검증합니다.

## Fixture 클래스 지도

![exposed-shared-tests class map](../../docs/images/readme-diagrams/00-shared-exposed-shared-tests-class-01.png)

## 실행 방법

```bash
# 기본 매트릭스: H2 + PostgreSQL + MySQL V8
./gradlew :exposed-shared-tests:test

# H2만 사용해 빠르게 피드백 확인
./gradlew :exposed-shared-tests:test -PuseFastDB=true

# Dialect를 직접 지정
./gradlew :exposed-shared-tests:test -PuseDB=H2,POSTGRESQL
```

### 데이터베이스 선택

| Gradle 프로퍼티 | 설명 |
|-----------------|------|
| `-PuseDB=<이름,...>` | 쉼표로 나열한 `TestDB` 값만 실행합니다. |
| `-PuseFastDB=true` | `H2`만 실행합니다. `useDB`가 있으면 무시됩니다. |
| 없음 | `H2`, `POSTGRESQL`, `MYSQL_V8`을 실행합니다. |

사용 가능한 `TestDB` 값은 다음과 같습니다.

| 값 | 설명 |
|----|------|
| `H2`, `H2_V1` | H2 인메모리 대상 |
| `H2_MYSQL`, `H2_MARIADB`, `H2_PSQL` | H2 호환 모드 |
| `MARIADB`, `MYSQL_V5`, `MYSQL_V8` | MySQL 계열 대상 |
| `POSTGRESQL`, `POSTGRESQLNG` | PostgreSQL 대상 |
| `COCKROACH` | CockroachDB 대상 |
| `ORACLE`, `SQLSERVER` | 명시적으로 선택하고 실행 환경이 준비된 경우 사용하는 엔터프라이즈 DB 대상 |

우선순위는 `-PuseDB`, `-PuseFastDB`, 기본 매트릭스 순서입니다.

## 핵심 사용 패턴

### `withDb`와 `withDbSuspending`

blocking Exposed 트랜잭션은 `withDb(testDB) { ... }`, coroutine 테스트는 `withDbSuspending(testDB) { ... }`를 사용합니다.
두 헬퍼 모두 Dialect별 semaphore를 사용하고, 선택한 데이터베이스를 지연 연결하며, 트랜잭션 범위 안에서 `currentTestDB`를 노출합니다.

```kotlin
withDb(testDB) {
    // blocking transaction
}

withDbSuspending(testDB) {
    // coroutine transaction through newSuspendedTransaction
}
```

### `withTables`와 `withTablesSuspending`

`withTables(testDB, vararg tables)`는 블록 실행 전에 지정한 테이블을 drop/create하고, 테스트를 실행한 뒤 commit과 cleanup을 수행합니다.
suspending variant도 coroutine 트랜잭션 안에서 같은 생명주기를 따릅니다.

```kotlin
withTables(testDB, ActorTable) {
    ActorTable.insert { it[firstName] = "Ryu" }
    ActorTable.selectAll().count()
}
```

관련 테스트: [`src/test/kotlin/exposed/shared/tests/WithTablesTest.kt`](src/test/kotlin/exposed/shared/tests/WithTablesTest.kt)

### `withSchemas`와 `withSchemasSuspending`

스키마 헬퍼는 현재 Dialect가 schema 생성을 지원한다고 보고할 때만 스키마를 생성하고 삭제합니다.
그래서 H2, PostgreSQL, MySQL 계열, 명시적으로 선택한 컨테이너 기반 대상 사이에서 챕터 테스트를 더 쉽게 이식할 수 있습니다.

관련 테스트: [`src/test/kotlin/exposed/shared/tests/WithSchemasTest.kt`](src/test/kotlin/exposed/shared/tests/WithSchemasTest.kt)

### 샘플 Repository Fixture

`MovieSchema`는 movie/actor 테이블, many-to-many join table, DAO 엔티티, `withMovieAndActors` 같은 fixture 헬퍼를 정의합니다.
`ActorRepository`는 조회, 저장, count, 조건부 query 예제를 담은 작은 `JdbcRepository<Long, ActorRecord>` 구현입니다.

관련 테스트: [`src/test/kotlin/exposed/shared/repository/ActorRepositoryTest.kt`](src/test/kotlin/exposed/shared/repository/ActorRepositoryTest.kt)

## 챕터 작성자를 위한 메모

- Dialect별 파라미터화 테스트는 `AbstractExposedTest`를 상속하고 `@MethodSource(ENABLE_DIALECTS_METHOD)`를 사용하세요.
- 임의의 `Database.connect()`나 `SchemaUtils` 호출보다 공유 fixture 헬퍼를 우선 사용하세요.
- 로컬에서 빠르게 반복할 때는 `-PuseFastDB=true`를 사용하고, Dialect별 동작을 신뢰하기 전에는 기본 매트릭스를 실행하세요.

## 다음 챕터

- [01-spring-boot](../../01-spring-boot/README.ko.md): Spring Boot MVC/WebFlux 예제에서 Exposed 사용 패턴을 다룹니다.
