# 04 Exposed DDL: 연결 관리

이 모듈(`01-connection`)은 Exposed 프레임워크를 사용하여 데이터베이스 연결을 설정, 구성 및 관리하는 방법을 단계별로 학습합니다. 연결 처리의 기본부터 고급 기법까지 다양한 시나리오를 다룹니다.

## 학습 목표

- Exposed로 데이터베이스 연결을 설정하고 초기화하는 방법 이해
- 연결 관련 예외 처리 및 복구 전략 학습
- 연결 타임아웃 구성 및 관리 방법 습득
- 연결 풀링을 통한 성능 최적화 기법 익히기
- 다중 데이터베이스 연결 관리 방법 이해

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/connection` 아래에 있습니다.

### 기본 연결 관리

| 파일                            | 설명                    | 핵심 기능                       |
|-------------------------------|-----------------------|-----------------------------|
| `Ex01_Connection.kt`          | 기본 데이터베이스 연결 설정 및 초기화 | `Database.connect()`, 연결 확인 |
| `Ex02_ConnectionException.kt` | 연결 관련 예외 처리 및 복구 전략   | 예외 타입별 처리, 재시도 로직           |
| `Ex03_ConnectionTimeout.kt`   | 연결 타임아웃 구성 및 처리       | `connectTimeout`, 타임아웃 예외   |
| `DataSourceStub.kt`           | 테스트용 DataSource 시뮬레이션 | 격리된 테스트 환경 구성               |

### H2 특화 예제 (`h2/`)

| 파일                          | 설명              | 핵심 기능                  |
|-----------------------------|-----------------|------------------------|
| `Ex01_H2_ConnectionPool.kt` | H2 연결 풀 설정 및 활용 | 커넥션 풀 크기, 대기 시간 설정     |
| `Ex02_H2_MultiDatabase.kt`  | 다중 데이터베이스 연결 관리 | 데이터베이스 전환, 독립적 트랜잭션 관리 |

## 핵심 개념

### 1. 데이터베이스 연결 설정

```kotlin
// 기본 연결 설정
Database.connect(
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver",
    user = "sa",
    password = ""
)

// DataSource를 사용한 연결 설정
val dataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = "jdbc:h2:mem:test"
    username = "sa"
    password = ""
    maximumPoolSize = 10
})
Database.connect(dataSource)
```

### 2. 연결 예외 처리

```kotlin
try {
    transaction {
        // 데이터베이스 연산
    }
} catch (e: SQLTransientConnectionException) {
    // 일시적 연결 오류 - 재시도 가능
    println("Connection temporarily unavailable: ${e.message}")
} catch (e: SQLNonTransientConnectionException) {
    // 영구적 연결 오류 - 설정 확인 필요
    println("Connection failed: ${e.message}")
}
```

### 3. 연결 타임아웃 설정

```kotlin
// HikariCP를 사용한 타임아웃 설정
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/test"
    connectionTimeout = 30000      // 연결 타임아웃 (ms)
    idleTimeout = 600000           // 유휴 타임아웃 (ms)
    maxLifetime = 1800000          // 최대 생존 시간 (ms)
}
```

### 4. 다중 데이터베이스 연결

```kotlin
// 여러 데이터베이스에 연결
val db1 = Database.connect(
    url = "jdbc:h2:mem:db1",
    driver = "org.h2.Driver"
)

val db2 = Database.connect(
    url = "jdbc:h2:mem:db2", 
    driver = "org.h2.Driver"
)

// 특정 데이터베이스에서 트랜잭션 실행
transaction(db1) {
    // db1에서 실행
}

transaction(db2) {
    // db2에서 실행
}
```

## 연결 풀링 모범 사례

| 설정 항목               | 권장 값         | 설명          |
|---------------------|--------------|-------------|
| `maximumPoolSize`   | CPU 코어 수 * 2 | 최대 연결 수     |
| `minimumIdle`       | CPU 코어 수     | 최소 유휴 연결 수  |
| `connectionTimeout` | 30000ms      | 연결 대기 최대 시간 |
| `idleTimeout`       | 600000ms     | 유휴 연결 유지 시간 |
| `maxLifetime`       | 1800000ms    | 연결 최대 생존 시간 |

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :04-exposed-ddl:01-connection:test

# 특정 테스트만 실행
./gradlew :04-exposed-ddl:01-connection:test --tests "exposed.examples.connection.Ex01_Connection"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.

## 더 읽어보기

- [6.1 Connection Management](https://debop.notion.site/1ad2744526b080c5b15cc5b9a53c44ce?v=1ad2744526b080c1e839000ce56f3744)
- [HikariCP 공식 문서](https://github.com/brettwooldridge/HikariCP)
