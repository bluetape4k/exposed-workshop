# Benchmark (04-benchmark)

English | [한국어](./README.ko.md)

A module that runs `kotlinx-benchmark`-based microbenchmarks against the cache/routing examples from Chapter 11. Provides a fast smoke profile and a precise main profile, with results saveable as a Markdown table.

---

## Overview

Uses Caffeine near-cache + in-memory storage instead of real Redis/DB I/O to reliably compare the overhead of the cache layer itself. Because it is JMH-based, measurement results after JVM warm-up are trustworthy.

---

## Domain ERD

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
erDiagram
    persons {
        BIGSERIAL id PK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR email UK
        VARCHAR phone
        INT age
        VARCHAR address
        VARCHAR zipcode
        TEXT bio
        BLOB picture
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    departments {
        BIGSERIAL id PK
        VARCHAR name UK
        VARCHAR code UK
        TEXT description
        DECIMAL budget
        INT head_count
        BOOLEAN is_active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    employees {
        BIGSERIAL id PK
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR email UK
        VARCHAR phone
        VARCHAR position
        DECIMAL salary
        TIMESTAMP hire_date
        BOOLEAN is_active
        TEXT bio
        BLOB picture
        BIGINT department_id FK
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    departments ||--o{ employees : "has"
```

---

## Exposed vs JPA Benchmark Structure

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart TD
    JMH[JMH Benchmark Runner]

    subgraph SingleEntity["SingleEntityCrudBenchmark (Person)"]
        SE_EXP[Exposed DSL\nPersonTable]
        SE_JPA[JPA\nPersonJpa]
    end

    subgraph OneToMany["OneToManyCrudBenchmark (Department + Employee)"]
        OTM_EXP[Exposed DSL/DAO\nDepartmentTable + EmployeeTable]
        OTM_JPA[JPA\nDepartmentJpa + EmployeeJpa]
    end

    subgraph Concurrent["ConcurrentCrudBenchmark"]
        CC_EXP[Exposed DSL\nMulti-thread]
        CC_JPA[JPA\nMulti-thread]
    end

    DB[(H2 InMemory)]

    JMH --> SingleEntity
    JMH --> OneToMany
    JMH --> Concurrent

    SE_EXP --> DB
    SE_JPA --> DB
    OTM_EXP --> DB
    OTM_JPA --> DB
    CC_EXP --> DB
    CC_JPA --> DB

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class JMH purple
    class SE_EXP,OTM_EXP,CC_EXP green
    class SE_JPA,OTM_JPA,CC_JPA blue
    class DB orange
```

---

## Benchmarks

| Benchmark Class                       | Measured Items                              | Unit             |
|---------------------------------------|---------------------------------------------|------------------|
| `ReadThroughCacheBenchmark`           | DB direct read / cache hit / cache miss cost | µs (AverageTime) |
| `RoutingKeyResolverBenchmark`         | `currentLookupKey()` string construction cost | ns (AverageTime) |
| `CacheStrategyComparisonBenchmark`    | NoCache/ReadThrough/WriteThrough strategy comparison across READ_HEAVY/WRITE_HEAVY workloads | µs (AverageTime) |

---

## Class Structure

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
classDiagram
    class ReadThroughCacheBenchmark {
        +payloadBytes: Int [256, 4096]
        -db: Map~Long, UserPayload~
        -cache: Cache~Long, UserPayload~
        -missKeys: LongArray
        -hotKey: Long = 1L
        +setupTrial()
        +setupIteration()
        +dbOnlyRead(): UserPayload
        +readThroughCacheHit(): UserPayload
        +readThroughCacheMiss(): UserPayload
    }

    class RoutingKeyResolverBenchmark {
        +tenant: String ["tenant-a", ""]
        +readOnly: Boolean [true, false]
        -resolver: ContextAwareRoutingKeyResolver
        +setup()
        +currentLookupKey(): String
    }

    class UserPayload {
        +id: Long
        +bytes: ByteArray
    }

    class ContextAwareRoutingKeyResolver {
        +currentLookupKey(): String
    }

    class CacheStrategyComparisonBenchmark {
        +strategyType: CacheStrategy [NO_CACHE, READ_THROUGH, WRITE_THROUGH]
        +workloadPattern: WorkloadPattern [READ_HEAVY, WRITE_HEAVY]
        +payloadBytes: Int [256, 4096]
        -strategy: CacheStrategy
        -pattern: WorkloadPattern
        -operationCounter: AtomicLong
        +setupTrial()
        +tearDownTrial()
        +executeOperation(): Unit
    }

    class CacheStrategy {
        <<enum>>
        NO_CACHE
        READ_THROUGH
        WRITE_THROUGH
    }

    class WorkloadPattern {
        <<enum>>
        READ_HEAVY (90:10)
        WRITE_HEAVY (10:90)
    }

    ReadThroughCacheBenchmark --> UserPayload
    RoutingKeyResolverBenchmark --> ContextAwareRoutingKeyResolver
    CacheStrategyComparisonBenchmark --> CacheStrategy
    CacheStrategyComparisonBenchmark --> WorkloadPattern
    CacheStrategyComparisonBenchmark --> UserPayload

    style ReadThroughCacheBenchmark fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style RoutingKeyResolverBenchmark fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style CacheStrategyComparisonBenchmark fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style UserPayload fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style ContextAwareRoutingKeyResolver fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style CacheStrategy fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style WorkloadPattern fill:#E0F2F1,stroke:#80CBC4,color:#00695C
```

---

## Benchmark Parameters

### ReadThroughCacheBenchmark

| Parameter      | Value     | Description               |
|----------------|-----------|---------------------------|
| `payloadBytes` | 256, 4096 | UserPayload byte size      |
| DB size        | 2,048 entries | In-memory Map          |
| Caffeine max size | 4,096  | Near-cache limit           |
| Miss key count | 256       | Cyclic miss scenario       |

Measured methods:

- `dbOnlyRead` — Direct lookup from Map without Caffeine
- `readThroughCacheHit` — Path where hotKey(1L) is always present in cache
- `readThroughCacheMiss` — DB fallback path after cache invalidation each iteration

### RoutingKeyResolverBenchmark

| Parameter  | Value              | Description                               |
|------------|--------------------|-------------------------------------------|
| `tenant`   | `"tenant-a"`, `""` | Real tenant / empty (defaultTenant fallback) |
| `readOnly` | `true`, `false`    | `:ro` / `:rw` branch                      |

### CacheStrategyComparisonBenchmark

| Parameter         | Value                                    | Description                                           |
|-------------------|------------------------------------------|-------------------------------------------------------|
| `strategyType`    | `NO_CACHE`, `READ_THROUGH`, `WRITE_THROUGH` | Cache strategy under test                            |
| `workloadPattern` | `READ_HEAVY` (90:10), `WRITE_HEAVY` (10:90) | Read/write operation ratio                           |
| `payloadBytes`    | 256, 4096                                | UserPayload byte size                                 |
| DB size           | 2,048 entries                            | In-memory Map                                         |
| Caffeine max size | 4,096                                    | Near-cache limit (for READ_THROUGH/WRITE_THROUGH)    |

Measured method:

- `executeOperation` — Executes a mixed read/write workload against the selected strategy. Each iteration performs either a read or write based on the workload ratio.

---

## JMH Common Configuration

| Item             | Value               |
|------------------|---------------------|
| `@Fork`          | 1                   |
| `@Warmup`        | 5 iterations, 500ms |
| `@Measurement`   | 10 iterations, 1s   |
| `@BenchmarkMode` | `Mode.AverageTime`  |

---

## Benchmark Flow

```mermaid
%%{init: {"theme": "neutral", "themeVariables": {"fontFamily": "'Comic Mono', 'goorm sans code', 'JetBrains Mono', 'goorm sans'"}}}%%
flowchart LR
    Start([JMH Start])
    Fork[Fork JVM x1]
    Warmup[Warmup\n5 iterations × 500ms]
Measure[Measurement\n10 iterations × 1s]
Report[JSON Report]
MD[Markdown Report]

Start --> Fork --> Warmup --> Measure --> Report --> MD

subgraph ReadThroughCacheBenchmark
direction TB
RT1[dbOnlyRead\nDirect Map lookup]
RT2[readThroughCacheHit\nCaffeine hit]
RT3[readThroughCacheMiss\ncache.invalidate + DB fallback]
end

subgraph RoutingKeyResolverBenchmark
direction TB
RK1[currentLookupKey\ntenant-a:rw]
RK2[currentLookupKey\ntenant-a:ro]
RK3[currentLookupKey\ndefault:rw]
RK4[currentLookupKey\ndefault:ro]
end

Measure --> ReadThroughCacheBenchmark
Measure --> RoutingKeyResolverBenchmark

    classDef blue fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef green fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef purple fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef orange fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef yellow fill:#FFFDE7,stroke:#FFF176,color:#F57F17

    class Start purple
    class Fork,Warmup,Measure blue
    class Report,MD yellow
    class RT1,RT2,RT3 green
    class RK1,RK2,RK3,RK4 orange
```

---

## How to Run

```bash
# Fast smoke run (CI / quick trend check)
./gradlew :11-high-performance:04-benchmark:smokeBenchmark

# Default profile run (precise measurement)
./gradlew :11-high-performance:04-benchmark:benchmark

# Generate Markdown report (main profile)
./gradlew :11-high-performance:04-benchmark:benchmarkMarkdown

# Save smoke results as Markdown
./gradlew :11-high-performance:04-benchmark:benchmarkMarkdown -PbenchmarkProfile=smoke
```

---

## Result File Locations

| Format   | Path                                                     |
|----------|----------------------------------------------------------|
| JSON     | `build/reports/benchmarks/<profile>/.../jvm.json`        |
| Markdown | `build/reports/benchmarks/<profile>/benchmark-report.md` |

---

## Latest Benchmark Results

> The results below are reference values measured with the **smoke profile** (as of `2026-03-18`).
> For precise measurements, run `./gradlew :04-benchmark:benchmark` and check `benchmark-report.md` in `build/reports/benchmarks/main/`.

### ReadThroughCacheBenchmark

| Method                 | payloadBytes | Score (µs/op) | Error (±) | Interpretation                                         |
|------------------------|--------------|---------------|-----------|--------------------------------------------------------|
| `dbOnlyRead`           | 256          | 0.001         | 0.000     | HashMap direct lookup — baseline                       |
| `dbOnlyRead`           | 4096         | 0.001         | 0.000     | Large payload has same Map lookup cost                 |
| `readThroughCacheHit`  | 256          | 0.003         | 0.000     | Caffeine hit — ~3× vs Map (wrapper overhead)           |
| `readThroughCacheHit`  | 4096         | 0.003         | 0.000     | Payload size irrelevant — reference only returned      |
| `readThroughCacheMiss` | 256          | 0.119         | 0.282     | cache invalidate + DB fallback — ~40× vs hit           |
| `readThroughCacheMiss` | 4096         | 0.085         | 0.155     | Miss cost depends on cache invalidation, not payload   |

### RoutingKeyResolverBenchmark

| tenant     | readOnly | Score (µs/op) | Error (±) | Interpretation                         |
|------------|----------|---------------|-----------|----------------------------------------|
| `tenant-a` | `true`   | 0.004         | 0.001     | Real tenant + read-only key            |
| `tenant-a` | `false`  | 0.004         | 0.002     | Real tenant + read-write key           |
| `` (empty) | `true`   | 0.004         | 0.004     | defaultTenant fallback branch (higher error) |
| `` (empty) | `false`  | 0.004         | 0.002     | defaultTenant fallback branch          |

> **Summary**: Routing key computation cost (~4 ns) is practically negligible. Cache miss cost is up to 40× higher than a hit, making **reducing miss frequency the key optimization target**.

### CacheStrategyComparisonBenchmark

| Strategy       | Workload     | payloadBytes | Score (µs/op) | vs NoCache   | Interpretation                                          |
|----------------|--------------|--------------|---------------|--------------|---------------------------------------------------------|
| `NO_CACHE`     | READ_HEAVY   | 256          | 517.520       | baseline     | Every read hits DB — worst case for read-heavy loads    |
| `NO_CACHE`     | WRITE_HEAVY  | 256          | 507.766       | baseline     | Writes dominate — DB overhead similar regardless        |
| `READ_THROUGH` | READ_HEAVY   | 256          | 94.320        | **5.5× faster** | Cache absorbs 90% reads — major speedup               |
| `READ_THROUGH` | WRITE_HEAVY  | 256          | 468.735       | 1.1× faster  | Few reads to cache — minimal benefit                   |
| `WRITE_THROUGH`| READ_HEAVY   | 256          | 52.103        | **9.9× faster** | Cache warmed by writes + read hits — best performance |
| `WRITE_THROUGH`| WRITE_HEAVY  | 256          | 445.363       | 1.1× faster  | Write overhead to cache+DB, but reads still benefit    |

> **Summary**: For READ_HEAVY workloads, WriteThrough achieves **9.9× speedup** and ReadThrough achieves **5.5× speedup** over NoCache. For WRITE_HEAVY workloads, caching provides marginal improvement (~1.1×) since most operations bypass the cache read path. **WriteThrough is optimal when data is frequently written then re-read; ReadThrough excels for read-dominant access patterns.**

---

## Interpretation Notes

- `RoutingKeyResolverBenchmark`: Compares the overhead of tenant presence and read-only flag on routing key computation. An empty tenant adds a `defaultTenant` fallback branch, which may cause minor differences.
- `ReadThroughCacheBenchmark`: The order `dbOnlyRead` < `readThroughCacheHit` < `readThroughCacheMiss` is typical for the same payload size. The 256B vs 4096B comparison also reveals the impact of serialization cost.
- Use microbenchmark results for relative comparisons and trend analysis rather than absolute values.
- Use the smoke profile for quick trend checks, and the main profile for precise measurement.
