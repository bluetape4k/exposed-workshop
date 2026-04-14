# 04 Exposed DDL: 연결 관리 (01-connection)

Exposed 데이터베이스 연결 설정과 연결 안정성 검증을 다루는 모듈입니다. 연결 예외, 타임아웃, H2 커넥션 풀 및 다중 DB 연결 시나리오를 실습합니다.

## 개요

`Database.connect()`는 Exposed의 진입점입니다. URL 문자열 또는 `DataSource`를 받아 내부
`TransactionManager`를 초기화합니다. 이 모듈에서는 정상 연결 외에 예외 발생 시 재시도, 타임아웃 우선순위, HikariCP 풀 고갈 복구, 다중 DB 중첩 트랜잭션을 검증합니다.

## 학습 목표

- `Database.connect` 구성 방식(URL/DataSource)을 이해한다.
- 연결 예외/타임아웃 처리 패턴과 `maxAttempts` 재시도 설정을 익힌다.
- HikariCP 풀 고갈 후 커넥션 재활용 동작을 확인한다.
- 다중 DB 연결 시 트랜잭션 격리 전략을 이해한다.

## 선수 지식

- JDBC DataSource 기본
- [`../README.md`](../README.md)

## 아키텍처 흐름

```mermaid
flowchart LR
    subgraph Connect["Database.connect()"]
        URL["URL + Driver"]
        DS["DataSource (HikariCP)"]
    end

    subgraph TM["TransactionManager"]
        Default["defaultDatabase"]
        Multi["DB1 / DB2"]
    end

    subgraph Retry["재시도 정책"]
        MA["maxAttempts"]
        TO["connectionTimeout"]
    end

    URL --> TM
    DS --> TM
    TM --> Default & Multi
    TM --> Retry

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class URL,DS blue
    class Default,Multi green
    class MA,TO orange
```

## 핵심 개념

### 기본 연결

```kotlin
// URL 기반 연결
val db = Database.connect(
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver"
)

// DataSource 기반 연결 (HikariCP)
val hikariConfig = HikariConfig().apply {
    jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    driverClassName = "org.h2.Driver"
    maximumPoolSize = 10
}
val db = Database.connect(HikariDataSource(hikariConfig))
```

### 연결 예외 재시도

```kotlin
// ConnectionSpy로 commit/rollback/close 시 예외 강제 발생
// maxAttempts = 5 설정 시 정확히 5번 재시도 후 예외 전파
val db = Database.connect(
    datasource = ConnectionSpy(dataSource) { throw SQLException("forced") },
    databaseConfig = DatabaseConfig { maxAttempts = 5 }
)
```

### 타임아웃 우선순위

```kotlin
// DatabaseConfig 기본값보다 트랜잭션 블록 내 설정이 우선 적용됨
val db = Database.connect(
    datasource = dataSource,
    databaseConfig = DatabaseConfig { defaultMaxAttempts = 3 }
)

transaction(db) {
    maxAttempts = 1  // 이 값이 우선 적용됨 (defaultMaxAttempts = 3 무시)
    // ...
}
```

### 커넥션 풀 고갈 복구 (H2)

```kotlin
// maximumPoolSize(10) * 2 + 1 = 21개 비동기 트랜잭션 동시 실행
// 풀이 소진되어도 커넥션 반환 후 재활용 → 모두 성공
val jobs = (1..21).map {
    suspendedTransactionAsync(Dispatchers.IO, db) {
        // SELECT/INSERT 작업
    }
}
jobs.awaitAll()
```

### 다중 DB 중첩 트랜잭션 (H2)

```kotlin
// 3단 중첩 트랜잭션 — 각 DB의 격리 수준이 독립적으로 유지됨
transaction(db1) {
    // db1 작업
    transaction(db2) {
        // db2 작업
        transaction(db1) {
            // db1 재진입
        }
    }
}
```

## 예제 구성

| 파일                             | 설명                                                |
|--------------------------------|---------------------------------------------------|
| `Ex01_Connection.kt`           | URL/DataSource 기반 기본 연결                           |
| `Ex02_ConnectionException.kt`  | `ConnectionSpy`로 예외 강제 + `maxAttempts` 재시도        |
| `Ex03_ConnectionTimeout.kt`    | `defaultMaxAttempts` vs 트랜잭션 내 `maxAttempts` 우선순위 |
| `DataSourceStub.kt`            | 테스트용 DataSource 스텁                                |
| `h2/Ex01_H2_ConnectionPool.kt` | HikariCP 풀 고갈 + 재활용 시나리오 (H2 전용)                  |
| `h2/Ex02_H2_MultiDatabase.kt`  | 다중 DB 중첩 트랜잭션 격리 검증 (H2 전용)                       |

## H2 전용 테스트 한계

`h2/` 디렉터리 파일들은 **H2 인메모리 DB 전용**입니다.

- H2는 `DB_CLOSE_DELAY=-1` 옵션으로 프로세스 종료 전까지 인메모리 DB를 유지합니다.
- `TransactionManager.defaultDatabase` 동작 검증은 H2 환경에서만 안정적으로 재현됩니다.
- 실제 운영 DB에서 다중 DB 연결이 필요하다면 각 드라이버의 연결 문자열과 커넥션 풀 설정을 별도 검토해야 합니다.

## 테스트 실행 방법

```bash
# 전체 모듈 테스트
./gradlew :04-exposed-ddl:01-connection:test

# 특정 테스트 클래스만 실행
./gradlew :04-exposed-ddl:01-connection:test \
    --tests "exposed.examples.connection.Ex01_Connection"

# H2 전용 테스트
./gradlew :04-exposed-ddl:01-connection:test \
    --tests "exposed.examples.connection.h2.*"
```

## 복잡한 시나리오

### 커넥션 풀 설정 (`h2/Ex01_H2_ConnectionPool.kt`)

HikariCP `maximumPoolSize`를 초과하는 수의 코루틴 트랜잭션을
`suspendedTransactionAsync`로 동시에 실행합니다. 풀이 소진되더라도 커넥션이 반환되면 재활용되어 모든 작업이 정상 완료됨을 확인합니다.

```
커넥션 풀 크기(10) * 2 + 1 = 21개 비동기 트랜잭션 → 모두 성공
```

### 다중 DB 중첩 트랜잭션 (`h2/Ex02_H2_MultiDatabase.kt`)

`transaction(db1) { ... transaction(db2) { ... transaction(db1) { } } }` 형태의 3단 중첩 트랜잭션을 통해 각 DB의 격리 수준이 올바르게 유지되는지 검증합니다.

### 커넥션 예외 재시도 (`Ex02_ConnectionException.kt`)

`ConnectionSpy`로 실제 연결을 래핑하여 commit/rollback/close 시 예외를 강제로 발생시킵니다.
`maxAttempts = 5` 설정 시 정확히 5번 재시도 후 예외가 전파되는지 검증합니다.

### 타임아웃 우선순위 (`Ex03_ConnectionTimeout.kt`)

`DatabaseConfig.defaultMaxAttempts`와 트랜잭션 블록 내 `maxAttempts` 중 어느 값이 우선 적용되는지 검증합니다. 트랜잭션 블록 내 설정이 항상 우선입니다.

## 실습 체크리스트

- 잘못된 URL/계정으로 실패 시나리오를 재현한다.
- 타임아웃 값을 조정하며 실패 시간을 비교한다.
- 과도한 재시도 루프를 방지한다.
- 테스트 간 DB 상태가 공유되지 않도록 분리한다.

## 다음 모듈

- [`../02-ddl/README.md`](../02-ddl/README.md)
