# 04 Exposed DDL: 연결 관리 (01-connection)

Exposed 데이터베이스 연결 설정과 연결 안정성 검증을 다루는 모듈입니다. 연결 예외, 타임아웃, H2 연결 풀 및 다중 DB 연결 시나리오를 실습합니다.

## 학습 목표

- `Database.connect` 구성 방식을 이해한다.
- 연결 예외/타임아웃 처리 패턴을 익힌다.
- 다중 DB 연결 시 테스트 격리 전략을 익힌다.

## 선수 지식

- JDBC DataSource 기본
- [`../README.md`](../README.md)

## 핵심 개념

- 연결 초기화와 재시도
- timeout 설정
- 연결 풀/다중 DB 분리

## 예제 구성

| 파일                             | 설명              |
|--------------------------------|-----------------|
| `Ex01_Connection.kt`           | 기본 연결           |
| `Ex02_ConnectionException.kt`  | 연결 예외 처리        |
| `Ex03_ConnectionTimeout.kt`    | 연결 타임아웃         |
| `DataSourceStub.kt`            | 테스트용 DataSource |
| `h2/Ex01_H2_ConnectionPool.kt` | H2 풀 설정         |
| `h2/Ex02_H2_MultiDatabase.kt`  | 다중 DB 연결        |

## 실행 방법

```bash
# 전체 모듈 테스트
./gradlew :04-exposed-ddl:01-connection:test

# 특정 테스트 클래스만 실행
./gradlew :04-exposed-ddl:01-connection:test --tests "exposed.examples.connection.Ex01_Connection"
./gradlew :04-exposed-ddl:01-connection:test --tests "exposed.examples.connection.h2.*"
```

## H2 전용 테스트 한계

`h2/` 디렉터리 아래의 파일(`Ex01_H2_ConnectionPool.kt`, `Ex02_H2_MultiDatabase.kt`)은
**H2 인메모리 DB 전용**으로 작성되어 있으며, 다른 DB(PostgreSQL, MySQL 등)에서는 실행되지 않습니다.

- H2는 `DB_CLOSE_DELAY=-1` 옵션으로 프로세스 종료 전까지 인메모리 DB를 유지합니다.
- `TransactionManager.defaultDatabase` 동작 검증은 H2 환경에서만 안정적으로 재현됩니다.
- 실제 운영 DB에서 다중 DB 연결이 필요하다면 각 DB 드라이버의 연결 문자열과 커넥션 풀 설정을 별도로 검토해야 합니다.

## 복잡한 시나리오

### 커넥션 풀 설정 (`h2/Ex01_H2_ConnectionPool.kt`)

HikariCP `maximumPoolSize`를 초과하는 수의 코루틴 트랜잭션을 `suspendedTransactionAsync`로 동시에 실행합니다.
풀이 소진되더라도 커넥션이 반환되면 재활용되어 모든 작업이 정상 완료됨을 확인합니다.

```
커넥션 풀 크기(10) * 2 + 1 = 21개 비동기 트랜잭션 → 모두 성공
```

### 다중 DB 중첩 트랜잭션 (`h2/Ex02_H2_MultiDatabase.kt`)

`transaction(db1) { ... transaction(db2) { ... transaction(db1) { } } }` 형태의 3단 중첩 트랜잭션을
통해 각 DB의 격리 수준이 올바르게 유지되는지 검증합니다.

### 커넥션 예외 재시도 (`Ex02_ConnectionException.kt`)

`ConnectionSpy`로 실제 연결을 래핑하여 commit/rollback/close 시 예외를 강제로 발생시킵니다.
`maxAttempts = 5` 설정 시 정확히 5번 재시도 후 예외가 전파되는지 검증합니다.

### 타임아웃 우선순위 (`Ex03_ConnectionTimeout.kt`)

`DatabaseConfig.defaultMaxAttempts`와 트랜잭션 블록 내 `maxAttempts` 중 어느 값이 우선 적용되는지 검증합니다.
트랜잭션 블록 내 설정이 항상 우선입니다.

## 실습 체크리스트

- 잘못된 URL/계정으로 실패 시나리오를 재현한다.
- 타임아웃 값을 조정하며 실패 시간을 비교한다.

## 성능·안정성 체크포인트

- 과도한 재시도 루프를 방지
- 테스트 간 DB 상태가 공유되지 않도록 분리

## 다음 모듈

- [`../02-ddl/README.md`](../02-ddl/README.md)
