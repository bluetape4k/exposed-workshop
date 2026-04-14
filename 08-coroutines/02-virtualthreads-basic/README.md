# 08 Coroutines: Virtual Threads Basic (02-virtualthreads-basic)

English | [한국어](./README.ko.md)

A module for running Exposed transactions on Java 21 Virtual Threads. Covers patterns for achieving high concurrency while retaining a blocking code style.

## Learning Goals

- Learn how to use `newVirtualThreadJdbcTransaction`.
- Understand the Virtual Thread async execution pattern.
- Compare differences with platform thread and coroutine approaches.

## Prerequisites

- Java 21+
- [`../01-coroutines-basic/README.md`](../01-coroutines-basic/README.md)

## Key Concepts

### newVirtualThreadJdbcTransaction — Basic Usage

```kotlin
// Run a transaction on a Virtual Thread (retains blocking style)
newVirtualThreadJdbcTransaction {
    VTester.insert { }
    commit()
}

// Nest a Virtual Thread transaction within an existing transaction
fun JdbcTransaction.getTesterById(id: Int): ResultRow? =
    newVirtualThreadJdbcTransaction {
        VTester.selectAll()
            .where { VTester.id eq id }
            .singleOrNull()
    }
```

### virtualThreadJdbcTransactionAsync — Parallel Execution

```kotlin
// Run multiple transactions in parallel using Virtual Threads
val futures: List<VirtualFuture<EntityID<Int>>> = (1..10).map {
    virtualThreadJdbcTransactionAsync {
        VTester.insertAndGetId { }
    }
}
val ids = futures.awaitAll()
```

## Virtual Thread Transaction Flow

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
sequenceDiagram
    participant C as Caller (Regular Code)
    participant VT as newVirtualThreadJdbcTransaction
    participant VTA as virtualThreadJdbcTransactionAsync
    participant DB as Database

    C->>VT: newVirtualThreadJdbcTransaction { }
    Note over VT: JVM creates Virtual Thread, mounts on platform thread
    VT->>DB: BEGIN
    VT->>DB: INSERT / SELECT ...
    alt Success
        VT->>DB: COMMIT
        VT-->>C: Result returned
    else Exception
        VT->>DB: ROLLBACK
        VT-->>C: Exception propagated
    end

    C->>VTA: virtualThreadJdbcTransactionAsync { }
    VTA-->>C: VirtualFuture<T> (returned immediately)
    Note over C,VTA: Multiple Virtual Threads run in parallel
    C->>C: futures.awaitAll()
    VTA->>DB: BEGIN → SQL → COMMIT
    VTA-->>C: Result returned
```

## Coroutines vs Virtual Threads Practical Selection Guide

| Situation                                       | Recommended Approach  |
|------------------------------------------------|-----------------------|
| New async codebase                              | Kotlin Coroutines     |
| Adding concurrency to existing synchronous code | Virtual Threads       |
| Spring WebFlux / Reactive integration           | Kotlin Coroutines     |
| Spring MVC (servlet-based) + high concurrency   | Virtual Threads       |
| Fine-grained cancellation control               | Kotlin Coroutines     |
| Java 17 or lower environment                    | Kotlin Coroutines     |
| Java 21+ environment, minimize code changes     | Virtual Threads       |

## Virtual Thread Processing Model Flowchart

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart TD
    A["일반 코드 (블로킹 스타일)"] --> B["newVirtualThreadJdbcTransaction { }"]
    B --> C["JVM: Virtual Thread 생성"]
    C --> D["플랫폼 스레드에 마운트\n(OS Thread-N)"]
    D --> E["Exposed Transaction 시작\nBEGIN"]
    E --> F["DB 작업 수행\n(INSERT / SELECT / UPDATE)"]
    F --> G{Result}
    G -->|Success| H["COMMIT\nResult returned"]
    G -->|Exception| I["ROLLBACK\nException propagated"]
    H --> J["Virtual Thread terminated\n(platform thread released)"]
    I --> J

    B2["virtualThreadJdbcTransactionAsync { }"] --> K["VirtualFuture&lt;T&gt; returned immediately"]
    K --> L["Parallel Virtual Threads execute"]
    L --> M["futures.awaitAll() wait"]
    M --> N["All results collected"]

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef red fill:#FFEBEE,stroke:#EF9A9A,color:#C62828
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

    class A blue
    class B,C,D green
    class E,F orange
    class H green
    class I red
    class J purple
    class B2,K,L,M,N blue
```

## Virtual Thread vs Platform Thread Comparison Diagram

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    subgraph PlatformThread["플랫폼 스레드 (전통적 방식)"]
        direction TD
        P1["요청 1 → OS Thread-1 점유\n(I/O 대기 중에도 스레드 블로킹)"]
        P2["요청 2 → OS Thread-2 점유"]
        P3["요청 N → OS Thread-N 점유"]
        P1 & P2 & P3 --> PLimit["스레드 수 한계\n→ 동시성 제한"]
    end

    subgraph VirtualThread["Virtual Thread (Java 21+)"]
        direction TD
        V1["Virtual Thread 1"] & V2["Virtual Thread 2"] & V3["Virtual Thread N"]
        V1 & V2 & V3 --> |"I/O 대기 시 언마운트"| Carrier["소수의 Carrier(OS) Thread"]
        Carrier --> |"작업 재개 시 마운트"| V1
        Carrier --> VScale["수백만 개 동시 실행 가능"]
    end

    PlatformThread -. "Java 21 이상에서 대체" .-> VirtualThread

    classDef red fill:#FFEBEE,stroke:#EF9A9A,color:#C62828
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class P1,P2,P3,PLimit red
    class V1,V2,V3,Carrier,VScale green
```

## Table ERD (virtualthreads_table)

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    virtualthreads_table {
        SERIAL id PK
        VARCHAR_50 name "NULL"
    }
    virtualthreads_table_unique {
        INT id PK "UNIQUE INDEX"
    }
```

## Example Structure

Source location: `src/test/kotlin/exposed/examples/virtualthreads`

| File                       | Key Test Scenarios                                                                                                        |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `Ex01_VirtualThreads.kt` | Query non-existent ID, single insert/query, parallel insert, duplicate key exception, mixing regular `transaction`, nested exception handling |

### Key Test Scenarios

| Scenario                                      | API Used                                                 |
|------------------------------------|--------------------------------------------------------|
| Basic Virtual Thread transaction             | `newVirtualThreadJdbcTransaction`                      |
| Nested execution within existing transaction | `newVirtualThreadJdbcTransaction` (inner nesting)       |
| Async parallel insert (10 records)           | `virtualThreadJdbcTransactionAsync` + `awaitAll`       |
| Duplicate key insert → exception verification | `assertFailsWith<ExecutionException>`                  |
| Comparison with regular `transaction { }`   | `transaction { }` vs `newVirtualThreadJdbcTransaction` |
| Java 21-only execution condition             | `@EnabledOnJre(JRE.JAVA_21)` annotation                |

## How to Run

```bash
./gradlew :08-coroutines:02-virtualthreads-basic:test
```

> Runs only on Java 21+. Protected by the `@EnabledOnJre(JRE.JAVA_21)` annotation.

```bash
# Check Java version
java -version

# Run with a specific Java version
mise use java@21
./gradlew :08-coroutines:02-virtualthreads-basic:test
```

## Practice Checklist

- Measure throughput/latency changes as the number of concurrent tasks increases
- Verify rollback/cleanup behavior on exceptions
- Compare the same scenarios between the coroutine version and Virtual Thread version

## Performance & Stability Checkpoints

- Adjust Virtual Thread count together with DB connection count
- Isolate bottlenecks caused by long I/O or external calls
- Watch for `pinning`: blocking calls inside `synchronized` blocks pin a Virtual Thread to its platform thread

## Next Chapter

- [`../../09-spring/README.md`](../../09-spring/README.md)
