# 00. 공유 라이브러리

워크샵 전 모듈에서 공통으로 사용하는 테스트 인프라를 제공하는 섹션입니다. 데이터베이스 연결 설정, 테스트 베이스 클래스, 테이블 생성/삭제 헬퍼 등 반복 코드를 캡슐화하여 각 챕터의 예제가 비즈니스 로직에 집중할 수 있도록 지원합니다.

## 챕터 목표

- 모든 Exposed 테스트가 공유하는 기반 클래스(`AbstractExposedTest`)의 구조를 파악한다.
- `TestDB` enum을 통해 H2·PostgreSQL·MySQL·MariaDB 등 다양한 DB를 일관된 방식으로 다루는 패턴을 이해한다.
- `withTables` / `withTablesSuspending` 헬퍼로 테스트 전후 테이블 생명주기를 관리하는 방법을 익힌다.
- 이후 챕터에서 이 공유 모듈을 의존성으로 추가하는 방식을 확인한다.

## 선수 지식

- Kotlin 기본 문법
- JUnit 5 기초 (어노테이션, 파라미터화 테스트)
- 관계형 데이터베이스 기본 개념 (연결 URL, 드라이버)

## 포함 모듈

| 모듈                     | 설명                                                                        |
|------------------------|---------------------------------------------------------------------------|
| [`exposed-shared-tests`](./exposed-shared-tests/README.md) | 공통 테스트 베이스 클래스, DB 설정, `WithTables` 헬퍼 및 ERD 문서 집합 |

## 핵심 인프라 클래스

### `AbstractExposedTest`

모든 Exposed 테스트의 기반 추상 클래스입니다.

- 기본 타임존을 **UTC**로 설정합니다.
- `Fakers.faker` 인스턴스를 제공하여 테스트 데이터 생성을 지원합니다.
- `enableDialects()` 메서드로 현재 활성화된 DB 방언 목록을 반환하며, `@MethodSource`와 함께 파라미터화 테스트에 사용됩니다.

```kotlin
@ParameterizedTest
@MethodSource(AbstractExposedTest.ENABLE_DIALECTS_METHOD)
fun `예제 테스트`(testDB: TestDB) {
    withTables(testDB, MyTable) {
        // 테스트 로직
    }
}
```

### `TestDB`

테스트 대상 데이터베이스를 열거형으로 정의합니다. Testcontainers를 기본으로 사용하며, `-PuseFastDB=true` 옵션으로 H2 전용 빠른 테스트를 실행할 수 있습니다.

| 값              | 설명                         |
|----------------|----------------------------|
| `H2`           | H2 인메모리 DB (기본 모드)       |
| `H2_MYSQL`     | H2 (MySQL 호환 모드)           |
| `H2_MARIADB`   | H2 (MariaDB 호환 모드)         |
| `H2_PSQL`      | H2 (PostgreSQL 호환 모드)      |
| `MARIADB`      | MariaDB (Testcontainers)    |
| `MYSQL_V8`     | MySQL 8.x (Testcontainers)  |
| `POSTGRESQL`   | PostgreSQL (Testcontainers) |

### `WithTables` / `WithTablesSuspending`

테스트 실행 전 테이블을 생성하고, 완료 후 삭제하는 헬퍼 함수입니다. 동기 트랜잭션과 코루틴 기반 트랜잭션 두 가지 버전을 제공합니다.

```kotlin
// 동기 방식
withTables(testDB, UsersTable, OrdersTable) {
    // 테이블이 생성된 상태에서 테스트 실행
}

// 코루틴 방식
withTablesSuspending(testDB, UsersTable) {
    // suspend 함수 내에서 사용 가능
}
```

### `withDb`

단일 `TestDB`에 연결하여 트랜잭션 블록을 실행하는 저수준 헬퍼입니다. DB별 Semaphore로 동시 접근을 제어합니다.

## 실행 방법

```bash
# 공유 테스트 모듈 단독 실행
./gradlew :exposed-shared-tests:test

# H2만 대상으로 빠르게 실행
./gradlew :exposed-shared-tests:test -PuseFastDB=true
```

## 테스트 포인트

- `WithTables` 헬퍼가 테스트 전후 테이블을 정확히 생성·삭제하는지 검증
- `TestDB.enabledDialects()`가 환경 변수 설정에 따라 올바른 DB 목록을 반환하는지 확인

## 성능·안정성 체크포인트

- 테스트 간 DB 연결 공유 시 Semaphore로 상호 배제가 보장되는지 확인
- Testcontainers 기동 오버헤드를 줄이기 위해 `-PuseFastDB=true`를 로컬 개발 시 활용

## 다음 챕터

- [03-exposed-basic](../03-exposed-basic/README.md): DSL과 DAO 패턴 기초 예제로 넘어갑니다.
