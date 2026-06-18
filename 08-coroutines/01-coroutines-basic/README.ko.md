# 08 Coroutines: 기본 (01-coroutines-basic)

[English](./README.md) | 한국어

이 모듈은 트랜잭션 경계가 눈에 보이도록 예제를 작게 유지합니다. `Ex01_Coroutines.kt`는 `Tester`, `TesterUnique`를 사용해 suspended transaction, 중첩 조회, 비동기 insert/update 경합, 트랜잭션 격리, 예외 발생 후 정리 동작을 검증합니다.

## 학습 목표

- 테스트가 사용하는 fixture 흐름 안에서 `newSuspendedTransaction`, `withSuspendTransaction`, `suspendedTransactionAsync`를 익힌다.
- 순차 suspended 작업과 병렬 insert/update 작업의 차이를 비교한다.
- 중복 키나 중첩 트랜잭션 실패가 발생했을 때 rollback과 connection 정리가 어떻게 검증되는지 확인한다.

## 선수 지식

- Kotlin Coroutines 기본
- [`../../05-exposed-dml/04-transactions/README.md`](../../05-exposed-dml/04-transactions/README.md)

## 핵심 개념

### newSuspendedTransaction — 기본 사용

```kotlin
// suspend 함수 내에서 트랜잭션 시작
newSuspendedTransaction(Dispatchers.IO) {
    Tester.insert { }  // DB 작업
}

// 기존 트랜잭션에서 중첩 실행 (withSuspendTransaction)
suspend fun JdbcTransaction.getTesterById(id: Int): ResultRow? =
    withSuspendTransaction {
        Tester.selectAll()
            .where { Tester.id eq id }
            .singleOrNull()
    }
```

### suspendedTransactionAsync — 병렬 실행

```kotlin
// 여러 트랜잭션을 병렬로 실행
val jobs: List<Deferred<EntityID<Int>>> = (1..10).map {
    suspendedTransactionAsync(Dispatchers.IO) {
        Tester.insertAndGetId { }
    }
}
val ids = jobs.awaitAll()
```

### Dispatcher 선택 기준

```kotlin
// I/O 바운드 DB 작업은 Dispatchers.IO 사용
newSuspendedTransaction(Dispatchers.IO) { ... }

// 단일 스레드 Dispatcher — 순서 보장이 필요한 경우
val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
newSuspendedTransaction(singleThreadDispatcher) { ... }
```

## 코루틴 트랜잭션 시퀀스 다이어그램

![01 coroutines basic Sequence Flow diagram](../../docs/images/readme-diagrams/08-coroutines-01-coroutines-basic-sequence-01.png)

## newSuspendedTransaction 처리 시퀀스 다이어그램

![newSuspendedTransaction diagram](../../docs/images/readme-diagrams/08-coroutines-01-coroutines-basic-sequence-02.png)

## 테이블 ERD (coroutines_tester)

![ERD (coroutines_tester) diagram](../../docs/images/readme-diagrams/08-coroutines-01-coroutines-basic-erd-03.png)

## 예제 구성

소스 위치: `src/test/kotlin/exposed/examples/coroutines`

| 파일                   | 주요 테스트 시나리오 |
|----------------------|---|
| `Ex01_Coroutines.kt` | 존재하지 않는 ID 조회, 순차 suspended transaction, `TesterUnique` insert/update 경합, 중첩 suspended transaction, 병렬 fan-out, 일반 `transaction { }` 혼용, 중복 엔티티 ID 예외 정리 |

### 주요 테스트 시나리오

| 시나리오 | 사용 API |
|---|---|
| 기본 suspended transaction과 누락 행 조회 | `newSuspendedTransaction`, `withSuspendTransaction` |
| `TesterUnique` insert/update 경합 | `suspendedTransactionAsync`, `awaitAll`, `maxAttempts` |
| 중첩 suspended transaction fan-out | `newSuspendedTransaction`, `suspendedTransactionAsync` |
| 중복 엔티티 ID 예외 정리 | `assertFailsWith<ExposedSQLException>` |
| 트랜잭션 격리 수준 설정 | `connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED` |

## 실행 방법

```bash
./gradlew :01-coroutines-basic:test
```

테스트 환경 변수:

```bash
# H2만 사용하는 빠른 테스트
USE_FAST_DB=true ./gradlew :01-coroutines-basic:test
```

## 실습 체크리스트

- 순차/병렬 트랜잭션 결과와 소요 시간을 비교
- 취소(cancellation) 상황에서 롤백 동작 확인
- `Dispatchers.IO` vs `singleThreadDispatcher` 동작 차이 비교

## 성능·안정성 체크포인트

- 이벤트 루프/기본 디스패처(`Dispatchers.Default`)에서 DB 블로킹 호출 금지
- `Dispatchers.IO`는 I/O 바운드 작업 전용으로 사용
- 트랜잭션 범위를 최소화해 경합 감소
- 코루틴 취소 시 `finally` 블록에서 자원 정리 보장

## 다음 모듈

- [`../02-virtualthreads-basic/README.md`](../02-virtualthreads-basic/README.md)
