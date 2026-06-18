# 00-shared — 공유 테스트 인프라

[English](./README.md) | 한국어

`00-shared`는 Exposed 워크샵의 각 챕터가 함께 사용하는 테스트 인프라를 담고 있습니다.
데이터베이스 선택, 트랜잭션 설정, 스키마/테이블 정리, 샘플 데이터 구성을 챕터마다 반복하지 않고 하나의 공유 모듈에서 관리합니다.

## 이 챕터가 제공하는 것

![00-shared test fixture architecture](../docs/images/readme-diagrams/00-shared-architecture-01.png)

- `TestDB`는 파라미터화 테스트에 사용할 데이터베이스 Dialect를 고릅니다.
- `AbstractExposedTest`는 공통 JUnit 진입점, UTC 타임존 설정, Faker, Dialect 헬퍼를 제공합니다.
- `withDb`와 `withDbSuspending`은 blocking 테스트와 coroutine 테스트에서 Exposed 트랜잭션을 엽니다.
- `withTables`와 `withSchemas`는 테스트 블록 전후로 테이블과 스키마를 생성하고 정리합니다.
- 샘플 테이블, DAO 엔티티, record, repository는 이후 예제에서 재사용할 수 있는 현실적인 fixture를 제공합니다.

## 포함 모듈

| 모듈 | 역할 |
|------|------|
| `exposed-shared-tests` | JUnit, Exposed, Testcontainers, 샘플 repository fixture를 모은 공유 테스트 모듈 |

## 소스 구조

![Shared test source layout](../docs/images/readme-diagrams/00-shared-directory-structure-02.png)

공개 fixture API는 `src/main/kotlin/exposed/shared/tests` 아래에 있습니다.
샘플 도메인은 `repository/model`, `repository/repository`에 모여 있고, `src/test`는 실제 Exposed 테이블과 설정된 Dialect를 사용해 헬퍼 동작을 검증합니다.

## 핵심 타입

### `AbstractExposedTest`

`AbstractExposedTest`는 워크샵 테스트의 공통 베이스 클래스입니다. `ENABLE_DIALECTS_METHOD`를 제공하고, Dialect 선택은 `TestDB.enabledDialects()`에 위임하며, 기본 타임존을 UTC로 맞추고 `prepareSchemaForTest()` 같은 공통 헬퍼를 제공합니다.

```kotlin
@TestMethodOrder(MethodOrderer.MethodName::class)
class MyExposedTest : AbstractExposedTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `basic CRUD test`(testDB: TestDB) {
        withTables(testDB, Users) {
            // test body
        }
    }
}
```

### `TestDB`

`TestDB`는 공유 테스트가 지원하는 데이터베이스 대상을 열거합니다.

| 값 | 설명 |
|----|------|
| `H2`, `H2_V1`, `H2_MYSQL`, `H2_MARIADB`, `H2_PSQL` | 빠른 피드백과 호환성 확인을 위한 H2 인메모리 모드 |
| `MARIADB`, `MYSQL_V5`, `MYSQL_V8` | Testcontainers 또는 로컬 fallback으로 실행하는 MySQL 계열 대상 |
| `POSTGRESQL`, `POSTGRESQLNG` | PostgreSQL 대상 |
| `COCKROACH` | CockroachDB 대상 |
| `ORACLE`, `SQLSERVER` | 명시적으로 선택하고 실행 환경이 준비된 경우 사용하는 엔터프라이즈 DB 대상 |

기본 활성 Dialect는 `H2`, `POSTGRESQL`, `MYSQL_V8`입니다.

## 데이터베이스 선택

| Gradle 프로퍼티 | 효과 |
|-----------------|------|
| `-PuseDB=<이름,...>` | 쉼표로 나열한 `TestDB` 값만 실행합니다. 가장 높은 우선순위를 가집니다. |
| `-PuseFastDB=true` | 빠른 로컬 피드백을 위해 `H2`만 실행합니다. |
| 없음 | 기본 매트릭스인 `H2`, `POSTGRESQL`, `MYSQL_V8`을 실행합니다. |

```bash
# 공유 모듈만 테스트
./gradlew :exposed-shared-tests:test

# H2만 실행
./gradlew :exposed-shared-tests:test -PuseFastDB=true

# Dialect를 직접 지정
./gradlew :exposed-shared-tests:test -PuseDB=H2,POSTGRESQL
```

`USE_TESTCONTAINERS`는 현재 `TestDB.kt`에 정의된 소스 상수이며 기본값은 `true`입니다.
활성화되어 있으면 컨테이너 기반 데이터베이스는 공유 테스트 인프라가 기동합니다.

## 다음 단계

자세한 fixture API 지도와 예제는 [`exposed-shared-tests`](./exposed-shared-tests/README.ko.md)를 참고하세요.
