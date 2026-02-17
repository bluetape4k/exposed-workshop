# Exposed with Kotlin Virtual Threads (Java 21+)

이 모듈은 Project Loom의 Java Virtual Threads를 Kotlin Coroutines와 함께 Exposed ORM에서 사용하는 방법을 보여줍니다. Virtual Threads는 경량 스레드로, 높은 동시성 환경에서 I/O 바운드 작업의 성능을 크게 향상시키고, 기존의 블로킹 코드 스타일을 유지하면서도 비동기 처리가 가능하게 합니다.

Exposed는 Virtual Threads와 통합될 수 있는 기능을 제공하여, 개발자가 더 이상 복잡한 비동기 프로그래밍 모델(콜백, 퓨처 등)에 얽매이지 않고도 고성능 애플리케이션을 구축할 수 있도록 돕습니다. 이 모듈의 예제는
**Java 21 이상** 환경에서 실행 가능합니다.

## 학습 목표

- Exposed의 `newVirtualThreadTransaction` 함수를 사용하여 Virtual Threads 환경에서 데이터베이스 트랜잭션을 처리하는 방법 이해
- `virtualThreadTransactionAsync`를 활용하여 Virtual Threads를 이용한 동시(concurrent) 데이터베이스 작업을 효율적으로 수행하는 방법 학습
- Virtual Threads와 Exposed 트랜잭션의 통합으로 블로킹 코드 스타일을 유지하면서도 고성능 비동기 처리를 달성하는 방법 습득
- Virtual Thread 트랜잭션과 전통적인 플랫폼 스레드 트랜잭션을 혼용하는 시나리오 분석
- Virtual Thread 환경에서 발생하는 예외 처리 전략 파악

## 핵심 Virtual Thread 함수

Exposed는 Java Virtual Threads 환경에서 데이터베이스 작업을 수행하기 위한 특화된 함수들을 제공합니다.

- **`newVirtualThreadTransaction(db: Database? = null, block: Transaction.() -> R): R`**
    * Virtual Thread 환경에서 새로운 데이터베이스 트랜잭션을 시작합니다. `block` 내의 모든 데이터베이스 작업은 Virtual Thread에서 실행되며, Exposed의
      `transaction` 함수와 유사하게 동작하지만 Virtual Thread의 이점을 활용합니다.
    * 기존의 블로킹 코드 스타일을 유지하면서도, 플랫폼 스레드 오버헤드 없이 높은 동시성을 달성할 수 있습니다.
    * 자동으로 트랜잭션을 커밋하거나 예외 발생 시 롤백합니다.

- **`virtualThreadTransactionAsync(db: Database? = null, statement: Transaction.() -> T): VirtualFuture<T>`**
    * 비동기적으로 데이터베이스 트랜잭션을 Virtual Thread에서 시작하고 `VirtualFuture` 객체를 반환합니다. `VirtualFuture`를 통해 나중에 결과를
      `await()`할 수 있습니다.
    * 여러 데이터베이스 작업을 Virtual Thread를 사용하여 동시에 시작하고, 모든 작업이 완료될 때까지 기다려야 할 때 유용합니다 (예: `awaitAll()`).
    * `maxAttempts` 속성을 사용하여 동시성 문제에 대한 재시도 로직을 구성할 수 있습니다. `VirtualFuture`는
      `java.util.concurrent.Future` 인터페이스를 확장하며, Virtual Threads의 효율성을 유지합니다.

## 예제 구성 (Ex01_VritualThreads.kt)

`Ex01_VritualThreads.kt` 파일은 Exposed와 Java Virtual Threads를 함께 사용하는 다양한 방법을 보여주는 테스트 코드 예제로 구성되어 있습니다. 각 테스트 함수는 특정 시나리오에서의 Virtual Threads와 Exposed의 통합을 시연합니다.

| 테스트 함수명                                       | 설명                                                                                                                                       |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `virtual threads 를 이용하여 순차 작업 수행하기`           | `newVirtualThreadTransaction`을 사용하여 Virtual Thread 환경에서 순차적인 데이터베이스 작업을 안전하게 수행하는 방법을 보여줍니다. 기존 블로킹 코드와 유사한 형태로 비동기 처리가 가능합니다.           |
| `중첩된 virtual thread 용 트랜잭션을 async로 실행`        | `newVirtualThreadTransaction` 블록 내에서 `virtualThreadTransactionAsync`를 사용하여 동시에 여러 작업을 수행하고 `awaitAll()`로 대기하는 복잡한 동시성 시나리오를 보여줍니다.       |
| `다수의 비동기 작업을 수행 후 대기`                         | 여러 `INSERT` 작업을 `virtualThreadTransactionAsync`를 통해 동시에 실행하고, 모든 작업이 완료될 때까지 기다린 후 결과를 검증하는 예제입니다. `maxAttempts`를 통한 재시도 메커니즘도 포함됩니다.    |
| `virtual threads 용 트랜잭션과 일반 transaction 홉용하기` | Exposed Virtual Thread 트랜잭션(`newVirtualThreadTransaction`)과 전통적인 블로킹 트랜잭션(`transaction`)이 동일한 애플리케이션 컨텍스트 내에서 어떻게 공존하고 사용될 수 있는지를 보여줍니다. |
| `virtual thread 트랜잭션에서 예외 처리`                 | Virtual Thread 트랜잭션 내에서 예외가 발생했을 때의 동작을 보여줍니다. 특히, 내부 트랜잭션에서 예외 발생 시 연결 처리 및 외부 트랜잭션에 미치는 영향을 확인할 수 있습니다.                                |

## 코드 스니펫

### 1. `newVirtualThreadTransaction`을 이용한 순차 작업

```kotlin
// Virtual Thread 환경에서 순차적으로 DB 작업을 수행
newVirtualThreadTransaction {
    val id = VTester.insertAndGetId { } // 데이터 삽입
    commit() // 수동 커밋

    // 내부적으로 새로운 트랜잭션을 생성하여 비동기 작업을 수행
    getTesterById(id.value)!![VTester.id].value shouldBeEqualTo id.value
}
```

### 2. `virtualThreadTransactionAsync`를 이용한 동시 작업

```kotlin
// 비동기 방식으로 동시에 여러 작업을 수행하고 모든 작업이 완료될 때까지 대기
val results: List<Int> = List(recordCount) { index ->
    virtualThreadTransactionAsync {
        maxAttempts = 5 // 동시성 충돌 발생 시 재시도
        log.debug { "Task[$index] inserting ..." }
        VTester.insert { }
        index + 1
    }
}.awaitAll()
```

### 3. `newVirtualThreadTransaction`과 일반 `transaction` 혼용

```kotlin
// Virtual Thread 트랜잭션과 일반 블로킹 트랜잭션의 혼용 가능성
val row = newVirtualThreadTransaction {
    try {
        VTester.selectAll().toList()
    } catch (e: Throwable) {
        virtualThreadOk = false
        null
    }
}

val row2 = transaction {
    try {
        VTester.selectAll().toList()
    } catch (e: Throwable) {
        platformThreadOk = false
        null
    }
}
```

## 테스트 실행

이 모듈의 테스트는 `Ex01_VritualThreads.kt` 파일에 정의되어 있습니다. 다음 명령을 사용하여 실행할 수 있습니다.

**참고**: 이 테스트는 Java 21 이상 환경에서만 실행할 수 있습니다.

```bash
# 전체 테스트 실행
./gradlew :08-coroutines:02-virtualthreads-basic:test

# 특정 테스트만 실행 (예: 'virtual threads 를 이용하여 순차 작업 수행하기' 테스트)
./gradlew :08-coroutines:02-virtualthreads-basic:test --tests "exposed.examples.virtualthreads.Ex01_VritualThreads.virtual threads 를 이용하여 순차 작업 수행하기"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.
