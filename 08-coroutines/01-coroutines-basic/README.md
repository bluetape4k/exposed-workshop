# Exposed with Kotlin Coroutines

이 모듈은 Kotlin Coroutines와 함께 Exposed ORM을 사용하는 방법을 보여줍니다. Kotlin Coroutines는 비동기 프로그래밍을 간소화하고, 특히 I/O 바운드 작업인 데이터베이스 상호작용에서 애플리케이션의 응답성과 효율성을 크게 향상시킬 수 있습니다.

Exposed는 `newSuspendedTransaction` 및
`suspendedTransactionAsync`와 같은 특수 함수를 제공하여, 데이터베이스 작업을 non-blocking 방식으로 안전하게 수행할 수 있도록 지원합니다. 이를 통해 애플리케이션의 확장성과 성능을 최적화할 수 있습니다.

## 학습 목표

- Exposed의 `newSuspendedTransaction` 함수를 사용하여 코루틴 환경에서 non-blocking 데이터베이스 트랜잭션을 관리하는 방법 이해
- `suspendedTransactionAsync`를 활용하여 동시(concurrent) 데이터베이스 작업을 효율적으로 수행하는 방법 학습
- 다양한 `CoroutineDispatcher` (예: `Dispatchers.IO`, `Dispatchers.Default`)를 사용하여 트랜잭션 컨텍스트를 제어하는 방법 습득
- 중첩된 코루틴 트랜잭션의 동작 방식과 예외 처리 전략 파악
- Exposed 코루틴 API와 전통적인 블로킹 API를 혼용하는 시나리오 분석

## 핵심 Coroutine 함수

Exposed는 Kotlin Coroutines 환경에서 데이터베이스 작업을 수행하기 위한 여러 유용한 함수를 제공합니다.

- **
  `newSuspendedTransaction(context: CoroutineContext = EmptyCoroutineContext, db: Database? = null, block: suspend Transaction.() -> R): R`
  **
    * Coroutine 환경에서 새로운 데이터베이스 트랜잭션을 시작합니다. `block` 내의 모든 데이터베이스 작업은 이 트랜잭션 컨텍스트 내에서 실행됩니다.
    * `context` 인자를 통해 트랜잭션이 실행될 `CoroutineDispatcher`를 지정할 수 있습니다 (예:
      `Dispatchers.IO` for I/O bound operations). 이를 통해 스레드 풀 관리를 최적화하고 blocking 호출을 방지할 수 있습니다.
    * 자동으로 트랜잭션을 커밋하거나 예외 발생 시 롤백합니다.

- **
  `suspendedTransactionAsync(context: CoroutineContext = EmptyCoroutineContext, db: Database? = null, statement: suspend Transaction.() -> T): Deferred<T>`
  **
    * 비동기적으로 데이터베이스 트랜잭션을 시작하고 `Deferred` 객체를 반환합니다. `Deferred`를 통해 나중에 결과를 `await()`할 수 있습니다.
    * 여러 데이터베이스 작업을 동시에 시작하고, 모든 작업이 완료될 때까지 기다려야 할 때 유용합니다. (예: `awaitAll()`)
    * `maxAttempts` 속성을 사용하여 낙관적 잠금 실패(`OptimisticLockException`)와 같은 동시성 문제에 대한 재시도 로직을 구성할 수 있습니다.

- **`withSuspendTransaction(statement: suspend Transaction.() -> T): T`**
    * 이미 존재하는 `JdbcTransaction` 컨텍스트 내에서 suspend 가능한 데이터베이스 작업을 수행할 때 사용됩니다. 이 함수 자체는 새로운 트랜잭션을 시작하지 않습니다.
    * 일반적인 `transaction { ... }` 블록 내에서 suspend 함수를 호출해야 할 때 유용하게 사용될 수 있습니다.

## 예제 구성 (Ex01_Coroutines.kt)

`Ex01_Coroutines.kt` 파일은 Exposed 코루틴 API의 다양한 활용법을 보여주는 테스트 코드 예제로 구성되어 있습니다. 각 테스트 함수는 특정 시나리오에서의 Coroutines와 Exposed의 통합을 시연합니다.

| 테스트 함수명                                          | 설명                                                                                                                         |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `suspended transaction으로 시퀀셜 작업 수행하기`            | `newSuspendedTransaction`과 `singleThreadDispatcher`를 사용하여 코루틴 환경에서 순차적인 데이터베이스 작업을 안전하게 수행하는 방법을 보여줍니다.                    |
| `suspendedTransactionAsync으로 동시 작업 수행하기`         | `suspendedTransactionAsync`를 통해 `INSERT`와 `UPDATE`와 같은 동시 데이터베이스 작업을 실행하고, `maxAttempts`를 사용하여 잠금 충돌을 처리하는 방법을 시연합니다.      |
| `suspendedTransactionAsync 를 이용하여 여러 작업을 동시에 수행` | 여러 `suspendedTransactionAsync` 호출을 `awaitAll()`로 묶어 동시에 실행하고 결과를 취합하는 방법을 보여줍니다.                                           |
| `중첩된 suspend transaction 실행`                     | `newSuspendedTransaction` 블록 내에서 다른 suspend 함수를 호출하여 중첩된 코루틴 트랜잭션이 어떻게 동작하는지 설명합니다.                                        |
| `중첩된 suspend transaction async 실행`               | `newSuspendedTransaction` 블록 내에서 `suspendedTransactionAsync`를 사용하여 동시에 여러 작업을 수행하고 `awaitAll()`로 대기하는 복잡한 동시성 시나리오를 보여줍니다. |
| `다수의 비동기 작업을 수행 후 대기`                            | 여러 `INSERT` 작업을 `suspendedTransactionAsync`를 통해 동시에 실행하고, 모든 작업이 완료될 때까지 기다린 후 결과를 검증하는 예제입니다.                             |
| `suspended 와 일반 transaction 혼용하기`                | Exposed 코루틴 트랜잭션(`newSuspendedTransaction`)과 전통적인 블로킹 트랜잭션(`transaction`)이 동일한 애플리케이션 컨텍스트 내에서 어떻게 공존하고 사용될 수 있는지를 보여줍니다.  |
| `coroutines with exception within`               | 코루틴 트랜잭션 내에서 예외가 발생했을 때의 동작을 보여줍니다. 특히, 내부 트랜잭션에서 예외 발생 시 연결 처리 및 외부 트랜잭션에 미치는 영향을 확인할 수 있습니다.                             |

## 코드 스니펫

### 1. `newSuspendedTransaction`을 이용한 순차 작업

```kotlin
// Coroutines 환경에서 순차적으로 DB 작업을 수행
newSuspendedTransaction(context = singleThreadDispatcher) {
    val id = Tester.insertAndGetId { } // 데이터 삽입
    flushCache()
    entityCache.clear()

    val result = getTesterById(id.value) // 데이터 조회
    result!![Tester.id].value shouldBeEqualTo id.value
}
```

### 2. `suspendedTransactionAsync`를 이용한 동시 작업

```kotlin
// 비동기 방식으로 동시에 여러 작업을 수행하고 모든 작업이 완료될 때까지 대기
val (insertResult, updateResult) = listOf(
    suspendedTransactionAsync(Dispatchers.IO) {
        maxAttempts = 20 // 동시성 충돌 발생 시 재시도
        TesterUnique.insert { it[TesterUnique.id] = originId }
        TesterUnique.selectAll().count()
    },
    suspendedTransactionAsync(Dispatchers.Default) {
        maxAttempts = 20
        TesterUnique.update({ TesterUnique.id eq originId }) { it[TesterUnique.id] = updatedId }
        TesterUnique.selectAll().count()
    }
).awaitAll()
```

### 3. `suspendedTransaction`과 일반 `transaction` 혼용

```kotlin
// Coroutine 트랜잭션과 일반 블로킹 트랜잭션의 혼용 가능성
newSuspendedTransaction(db = db) {
    try {
        Tester.selectAll().toList()
    } catch (e: Throwable) {
        suspendedOk = false
    }
}

transaction(db) {
    try {
        Tester.selectAll().toList()
    } catch (e: Throwable) {
        normalOk = false
    }
}
```

## 테스트 실행

이 모듈의 테스트는 `Ex01_Coroutines.kt` 파일에 정의되어 있습니다. 다음 명령을 사용하여 실행할 수 있습니다.

```bash
# 전체 테스트 실행
./gradlew :08-coroutines:01-coroutines-basic:test

# 특정 테스트만 실행 (예: 'suspended transaction으로 시퀀셜 작업 수행하기' 테스트)
./gradlew :08-coroutines:01-coroutines-basic:test --tests "exposed.examples.coroutines.Ex01_Coroutines.suspended transaction으로 시퀀셜 작업 수행하기"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.
