# 08 Coroutines

English | [한국어](./README.ko.md)

Covers patterns for running Exposed in Kotlin Coroutines and Java Virtual Thread concurrency models, and provides guidelines for designing asynchronous transaction boundaries.

## Chapter Goals

- Understand the asynchronous access flow based on `newSuspendedTransaction`.
- Compare the pros and cons of the coroutine model vs. Virtual Thread model and establish practical selection criteria.
- Design stable transaction boundaries in concurrent environments.

## Prerequisites

- Kotlin Coroutines basic syntax / Context structure
- Transaction patterns from `05-exposed-dml/04-transactions`

## Coroutines vs Virtual Threads Comparison

| Item               | Kotlin Coroutines                                      | Java Virtual Threads (Java 21+)                                        |
|------------------|--------------------------------------------------------|------------------------------------------------------------------------|
| API              | `newSuspendedTransaction`, `suspendedTransactionAsync` | `newVirtualThreadJdbcTransaction`, `virtualThreadJdbcTransactionAsync` |
| Code Style       | `suspend` functions, `await()`                         | Blocking style can be retained                                          |
| Thread Usage     | Few threads + Dispatcher scheduling                     | JVM automatically mounts/unmounts on platform threads                   |
| Cancellation     | `Job.cancel()` + structured concurrency                | `Future.cancel()` / `Thread.interrupt()`                               |
| DB Connection    | Dispatcher.IO pool + connection pool coordination       | Adjust Virtual Thread count together with connection pool               |
| Migration        | Requires adding `suspend` keyword                      | Blocking code can be used as-is                                         |
| Primary Use Case | New async codebase, Spring WebFlux integration          | Adding concurrency to existing synchronous codebase                     |
| Min Java Version | Any                                                     | Java 21+                                                               |

## Concurrency Model Comparison Diagrams

### Coroutines vs Virtual Thread Processing Flow

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart LR
    subgraph Coroutines["Kotlin Coroutines"]
        direction TD
        A1["suspend 함수 호출"] --> B1["newSuspendedTransaction\n(Dispatchers.IO)"]
        B1 --> C1["코루틴 스케줄러\n(적은 스레드 재사용)"]
        C1 --> D1["DB 작업 수행"]
        D1 --> E1["suspend 복귀\n(비동기)"]
    end

    subgraph VirtualThreads["Java Virtual Threads (Java 21+)"]
        direction TD
        A2["일반 블로킹 코드 호출"] --> B2["newVirtualThreadJdbcTransaction"]
        B2 --> C2["JVM Virtual Thread 생성\n(플랫폼 스레드 자동 마운트)"]
        C2 --> D2["DB 작업 수행\n(블로킹 스타일)"]
        D2 --> E2["결과 반환\n(동기)"]
    end

    Coroutines -. "신규 비동기 코드베이스" .-> Use1[["권장: WebFlux / 취소 제어"]]
    VirtualThreads -. "기존 동기 코드베이스" .-> Use2[["권장: Spring MVC / 코드 변경 최소화"]]

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

    class A1,B1,C1,D1,E1 blue
    class A2,B2,C2,D2,E2 green
    class Use1 purple
    class Use2 orange
```

### Thread Model Structure Comparison

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart TD
    subgraph Platform["플랫폼 스레드 (기존)"]
        PT1["Thread-1 (OS Thread)"] --> DB1["DB Connection 1"]
        PT2["Thread-2 (OS Thread)"] --> DB2["DB Connection 2"]
        PT3["Thread-3 (OS Thread)"] --> DB3["DB Connection 3"]
    end

    subgraph CoroutineModel["코루틴 모델"]
        CT1["Dispatchers.IO\n(스레드 풀)"] --> |"suspend/resume"| CR1["Coroutine-1"]
        CT1 --> |"suspend/resume"| CR2["Coroutine-2"]
        CT1 --> |"suspend/resume"| CR3["Coroutine-N"]
        CR1 & CR2 & CR3 --> DBPool1["DB Connection Pool"]
    end

    subgraph VTModel["Virtual Thread 모델 (Java 21+)"]
        VT1["Virtual Thread-1"] --> |"마운트"| OS1["OS Thread-A"]
        VT2["Virtual Thread-2"] --> |"마운트"| OS1
        VT3["Virtual Thread-N"] --> |"마운트"| OS2["OS Thread-B"]
        VT1 & VT2 & VT3 --> DBPool2["DB Connection Pool"]
    end

    classDef red fill:#FFEBEE,stroke:#EF9A9A,color:#C62828
    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class PT1,PT2,PT3 red
    class DB1,DB2,DB3 red
    class CT1,CR1,CR2,CR3,DBPool1 blue
    class VT1,VT2,VT3,OS1,OS2,DBPool2 green
```

## Included Modules

| Module                    | Description                                        |
|---------------------------|--------------------------------------------------|
| `01-coroutines-basic`     | Basic Exposed examples using coroutines           |
| `02-virtualthreads-basic` | Concurrency examples using Virtual Threads        |

## Recommended Learning Order

1. `01-coroutines-basic`
2. `02-virtualthreads-basic`

## How to Run

```bash
# Run individual submodules
./gradlew :08-coroutines:01-coroutines-basic:test
./gradlew :08-coroutines:02-virtualthreads-basic:test

# Run full chapter
./gradlew :08-coroutines:test
```

## Transaction Flow Comparison

### Coroutines Transaction Flow

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart LR
    A["Caller (Coroutine)"] --> B["newSuspendedTransaction\n(Dispatchers.IO)"]
    A --> C["suspendedTransactionAsync\n(Dispatchers.IO)"]
    B --> D["DB Operation (DSL/DAO)"]
    C --> D
    D --> E{Result}
    E -->|Success| F["COMMIT"]
    E -->|Exception| G["ROLLBACK"]
    C --> H["Deferred&lt;T&gt;"]
    H --> I["await()"]

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef red fill:#FFEBEE,stroke:#EF9A9A,color:#C62828

    class A blue
    class B,C blue
    class D orange
    class F green
    class G red
    class H,I blue
```

### Virtual Thread Transaction Flow

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
flowchart LR
    A["Caller (Blocking Code)"] --> B["newVirtualThreadJdbcTransaction"]
    A --> C["virtualThreadJdbcTransactionAsync"]
    B --> D["DB Operation (DSL/DAO)"]
    C --> D
    D --> E{Result}
    E -->|Success| F["COMMIT"]
    E -->|Exception| G["ROLLBACK"]
    C --> H["VirtualFuture&lt;T&gt;"]
    H --> I["awaitAll()"]

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef red fill:#FFEBEE,stroke:#EF9A9A,color:#C62828

    class A blue
    class B,C blue
    class D orange
    class F green
    class G red
    class H,I blue
```

## Test Points

- Verify that resource cleanup works correctly when cancellation occurs.
- Validate that data consistency is maintained during parallel processing.

## Performance & Stability Checkpoints

- Ensure blocking calls do not occupy the Reactor/EventLoop.
- Tune thread/connection pool settings together with concurrency levels.

## Complex Scenario Guide

### Coroutine Transaction Patterns (`01-coroutines-basic/`)

| Scenario | Implementation File |
|---|---|
| Basic usage of `newSuspendedTransaction` | [`Ex01_Coroutines.kt`](01-coroutines-basic/src/test/kotlin/exposed/examples/coroutines/Ex01_Coroutines.kt) |
| Parallel execution with `suspendedTransactionAsync` | [`Ex01_Coroutines.kt`](01-coroutines-basic/src/test/kotlin/exposed/examples/coroutines/Ex01_Coroutines.kt) |

### Virtual Thread Transaction Patterns (`02-virtualthreads-basic/`)

| Scenario | Implementation File |
|---|---|
| Basic usage of `newVirtualThreadJdbcTransaction` | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |
| Async parallel execution with `virtualThreadJdbcTransactionAsync` | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |
| Mixing Virtual Thread with regular `transaction` | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |
| Nested transaction exception handling | [`Ex01_VirtualThreads.kt`](02-virtualthreads-basic/src/test/kotlin/exposed/examples/virtualthreads/Ex01_VirtualThreads.kt) |

## Next Chapter

- [09-spring](../09-spring/README.md): Continue learning Exposed integration patterns in a Spring integration environment.
