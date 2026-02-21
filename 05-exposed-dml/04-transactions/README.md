# 05 Exposed DML: 트랜잭션 관리

이 모듈(`04-transactions`)은 Exposed의 트랜잭션 관리 기능을 단계별로 학습합니다. 기본 트랜잭션부터 중첩 트랜잭션, 코루틴 통합까지 실무에서 필요한 모든 트랜잭션 패턴을 다룹니다.

## 학습 목표

- Exposed 트랜잭션의 기본 개념과 사용법 이해
- 트랜잭션 격리 수준 설정 방법 학습
- 중첩 트랜잭션과 세이브포인트 활용 기법 습득
- 트랜잭션 롤백 및 예외 처리 방법 익히기
- 코루틴 환경에서의 트랜잭션 관리 방법 이해

## 예제 구성

모든 예제는 `src/test/kotlin/exposed/examples/transactions` 아래에 있습니다.

### 기본 트랜잭션

| 파일                             | 설명            | 핵심 기능                     |
|--------------------------------|---------------|---------------------------|
| `TransactionTables.kt`         | 테스트용 테이블 정의   | 공통 스키마                    |
| `Ex01_TransactionIsolation.kt` | 트랜잭션 격리 수준 설정 | `isolation`, 격리 수준별 동작 확인 |
| `Ex02_TransactionExec.kt`      | 기본 트랜잭션 실행    | `transaction { }`, 커밋/롤백  |
| `Ex03_Parameterization.kt`     | 트랜잭션 매개변수화    | 트랜잭션 속성 설정                |
| `Ex04_QueryTimeout.kt`         | 쿼리 타임아웃 설정    | `queryTimeout`, 타임아웃 처리   |

### 고급 트랜잭션

| 파일                                      | 설명            | 핵심 기능                     |
|-----------------------------------------|---------------|---------------------------|
| `Ex05_NestedTransactions.kt`            | 중첩 트랜잭션 (동기)  | 세이브포인트, 중첩 커밋/롤백          |
| `Ex05_NestedTransactions_Coroutines.kt` | 중첩 트랜잭션 (코루틴) | `newSuspendedTransaction` |
| `Ex06_RollbackTransaction.kt`           | 트랜잭션 롤백       | 명시적/암시적 롤백                |
| `Ex07_ThreadLocalManager.kt`            | 스레드 로컬 관리     | 트랜잭션 컨텍스트 관리              |

## 핵심 개념

### 1. 기본 트랜잭션

```kotlin
// 기본 트랜잭션
transaction {
    // INSERT
    val userId = Users.insertAndGetId {
        it[name] = "John"
        it[email] = "john@example.com"
    }
    
    // UPDATE
    Users.update({ Users.id eq userId }) {
        it[name] = "John Doe"
    }
    
    // 자동 커밋 (블록 종료 시)
}

// 명시적 커밋/롤백
transaction {
    val userId = Users.insertAndGetId { /* ... */ }
    
    if (someCondition) {
        commit()  // 명시적 커밋
    } else {
        rollback()  // 명시적 롤백
    }
}
```

### 2. 트랜잭션 격리 수준

```kotlin
// 격리 수준 설정
transaction(TransactionIsolation.READ_COMMITTED) {
    // READ_COMMITTED 격리 수준으로 실행
}

// 데이터베이스 연결 시 기본 격리 수준 설정
Database.connect(
    datasource,
    databaseConfig = DatabaseConfig {
        defaultIsolationLevel = TransactionIsolation.REPEATABLE_READ
    }
)

// 지원되는 격리 수준
enum class TransactionIsolation {
    READ_UNCOMMITTED,  // 커밋되지 않은 읽기 허용
    READ_COMMITTED,    // 커밋된 읽기만 허용
    REPEATABLE_READ,   // 반복 가능한 읽기
    SERIALIZABLE       // 직렬화 가능
}
```

### 3. 중첩 트랜잭션 (세이브포인트)

```kotlin
transaction {
    // 외부 트랜잭션
    val orderId = Orders.insertAndGetId { /* ... */ }
    
    try {
        // 내부 트랜잭션 (세이브포인트)
        transaction {
            OrderItems.insert {
                it[order] = orderId
                it[product] = "Item 1"
            }
            // 예외 발생 시 이 부분만 롤백
        }
    } catch (e: Exception) {
        // 내부 트랜잭션 롤백됨, 외부는 계속
    }
    
    // 외부 트랜잭션 계속...
}
```

### 4. 트랜잭션 롤백

```kotlin
// 조건부 롤백
transaction {
    val user = Users.selectAll().where { Users.id eq userId }.singleOrNull()
    
    if (user == null) {
        rollback()
        return@transaction
    }
    
    // 계속 처리...
}

// 예외로 인한 자동 롤백
transaction {
    Users.insert { /* ... */ }
    throw RuntimeException("강제 예외")  // 자동 롤백
}
```

### 5. 코루틴 트랜잭션

```kotlin
// Suspend 트랜잭션
suspend fun createUser(name: String): Long {
    return newSuspendedTransaction(Dispatchers.IO) {
        Users.insertAndGetId {
            it[Users.name] = name
        }.value
    }
}

// 비동기 병렬 트랜잭션
val deferred1 = suspendedTransactionAsync(Dispatchers.IO) {
    // 첫 번째 트랜잭션
}

val deferred2 = suspendedTransactionAsync(Dispatchers.IO) {
    // 두 번째 트랜잭션
}

val (result1, result2) = awaitAll(deferred1, deferred2)
```

### 6. 쿼리 타임아웃

```kotlin
transaction {
    // 이 트랜잭션의 쿼리 타임아웃 설정 (초)
    queryTimeout = 10
    
    // 장기 실행 쿼리
    Users.selectAll().toList()
}
```

### 7. 트랜잭션 속성

```kotlin
transaction {
    // 트랜잭션 ID
    println("Transaction ID: ${this.id}")
    
    // 연결된 데이터베이스
    println("Database: ${this.db}")
    
    // 격리 수준
    println("Isolation: ${this.isolationLevel}")
    
    // 읽기 전용 여부
    readOnly = true
    
    // 재시도 횟수 (낙관적 잠금)
    maxAttempts = 3
}
```

## 트랜잭션 격리 수준 비교

| 격리 수준            | Dirty Read | Non-repeatable Read | Phantom Read |
|------------------|------------|---------------------|--------------|
| READ_UNCOMMITTED | O          | O                   | O            |
| READ_COMMITTED   | X          | O                   | O            |
| REPEATABLE_READ  | X          | X                   | O            |
| SERIALIZABLE     | X          | X                   | X            |

## 트랜잭션 모범 사례

1. **최소 범위**: 트랜잭션은 필요한 최소한의 코드만 포함
2. **적절한 격리 수준**: 데이터 일관성 요구사항에 맞는 격리 수준 선택
3. **예외 처리**: 예상 가능한 예외는 명시적 처리
4. **타임아웃 설정**: 장기 실행 쿼리에 대한 타임아웃 설정
5. **코루틴 사용**: 높은 동시성이 필요한 경우 코루틴 트랜잭션 사용

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :05-exposed-dml:04-transactions:test

# 특정 테스트만 실행
./gradlew :05-exposed-dml:04-transactions:test --tests "exposed.examples.transactions.Ex05_NestedTransactions"
```

모든 테스트는 `@ParameterizedTest`를 사용하여 H2, MySQL, PostgreSQL 등 다양한 데이터베이스 환경에서 실행됩니다.

## 더 읽어보기

- [7.4 Transactions](https://debop.notion.site/1ca2744526b080a69567d993571e21aa?v=1ca2744526b081bdab55000c5928063a)
- [Exposed Wiki: Transactions](https://github.com/JetBrains/Exposed/wiki/Transactions)
