# Platform Threads vs Virtual Threads JDBC 성능 비교

> 모든 수치는 실측 JMH / 부하테스트 결과입니다. 환경마다 차이가 있으므로 절대값보다 **배율(×)** 위주로 해석하세요.

---

## 핵심 요약

| 워크로드                            | Virtual Threads 향상 배율        |
|---------------------------------|------------------------------|
| 단순 SELECT (indexed)             | **×2.2 – ×2.6**              |
| 복잡 쿼리 (UPDATE + 집계)             | **×1.8**                     |
| I/O 지연 시뮬레이션 (+10ms latency)    | **×2.3**                     |
| CPU bound 작업                    | ×1.02 – 1.03 (거의 없음)         |
| `SELECT 1` (MariaDB, 극단적 경량 쿼리) | **×5 – ×9** (MariaDB 공식 블로그) |

---

## 1. JMH 실측: PostgreSQL / MariaDB JDBC (adi.earth, 2024)

**환경**: JMH, Java 21, MacBook i9 8-core 2.3GHz, Docker (TestContainers), 5,000 queries/iteration

### 단순 쿼리 — findByCode (유니크 인덱스)

| 실행 방식                        |     ops/s |   오차 |        배율 |
|------------------------------|----------:|-----:|----------:|
| CachedThreadPool (Platform)  |     1,997 |  ±41 |        ×1 |
| VirtualThreadPerTaskExecutor | **5,094** | ±290 | **×2.54** |
| ForkJoinPool (commonPool)    |     5,283 | ±172 |     ×2.64 |

> PostgreSQL 기준. MariaDB: Platform 1,867 ops/s → Virtual **4,073 ops/s** (×2.18)

### 복잡 쿼리 — UPDATE + GROUP BY 집계

| 실행 방식                        |     ops/s |  오차 |        배율 |
|------------------------------|----------:|----:|----------:|
| CachedThreadPool (Platform)  |       724 | ±15 |        ×1 |
| VirtualThreadPerTaskExecutor | **1,298** | ±50 | **×1.79** |
| ForkJoinPool                 |     1,317 | ±33 |     ×1.82 |

### +10ms 네트워크 지연 시뮬레이션

| 실행 방식                        |     ops/s |        배율 | 비고            |
|------------------------------|----------:|----------:|---------------|
| CachedThreadPool             |     2,205 |        ×1 | 스레드 무한 증가 위험  |
| VirtualThreadPerTaskExecutor | **5,004** | **×2.27** | 최적            |
| ForkJoinPool                 |     1,254 |     ×0.57 | 스레드 수 제한으로 역전 |

> **핵심 인사이트**: 지연(I/O) 상황에서 ForkJoinPool은 스레드 수 제한 때문에 성능이 오히려 하락.
> Virtual Threads만 I/O 대기 시 carrier thread를 반납(parking) → 효율 유지.

**출처
**: [Benchmark Database Access with Java 21 Virtual Threads - adi.earth (2024)](https://adi.earth/posts/database-virtual-threads-benchmark/)

---

## 2. MariaDB 공식 JMH 벤치마크 (MariaDB, 2023)

**환경**: JMH, DigitalOcean 16GB 4-CPU, Ubuntu 22.04, MariaDB 10.11, 16 connections, 100 queries/batch

| 쿼리 유형                    | Platform Threads (pool=4) | Virtual Threads |        배율 |
|--------------------------|--------------------------:|----------------:|----------:|
| `SELECT 1`               |                        기준 |               — |    **×9** |
| 단일 행 SELECT (100 int 컬럼) |                        기준 |               — |    **×5** |
| `DO 1` (경량 DML)          |                        기준 |               — | **×5 이상** |
| 1000행 SELECT             |                        기준 |               — | **×3 이상** |

> ⚠️ Platform 기준을 `CachedThreadPool`(무제한 스레드)로 설정 → 스레드 경쟁으로 성능 저하 포함.
> `pool=4`로 줄이면 성능이 개선되지만 여전히 Virtual Threads가 우세.
> MySQL Connector는 동일 테스트에서 격차가 훨씬 작음 (MariaDB Connector 최적화 효과).

**출처
**: [Benchmark JDBC connectors and Java 21 virtual threads - MariaDB (2023)](https://mariadb.com/resources/blog/benchmark-jdbc-connectors-and-java-21-virtual-threads/)

---

## 3. Spring Boot I/O 부하 테스트 (Stackademic, 2025)

**환경**: Spring Boot 3.x, PostgreSQL, 100 동시 연결

| 방식                     |           처리량 |       레이턴시 |
|------------------------|--------------:|-----------:|
| Platform Threads (MVC) |     844 req/s |     448 ms |
| Virtual Threads (MVC)  | **967 req/s** | **319 ms** |
| 향상                     |          +14% | **-28.8%** |

**병렬 DB 쿼리 시나리오** (여러 쿼리를 동시 실행):

| 방식                    |    총 레이턴시 |              처리량 |
|-----------------------|----------:|-----------------:|
| Platform Threads (순차) |    115 ms |       ~220 req/s |
| Virtual Threads (병렬)  | **43 ms** | **~2,600 req/s** |
| 향상                    |  **-62%** |        **×11.8** |

> 병렬 DB 쿼리에서 Virtual Threads의 효과가 극대화됨.

**출처
**: [Spring Boot 2025: Parallel Database Queries with Virtual Threads - JavaScript in Plain English](https://javascript.plainenglish.io/spring-boot-2025-parallel-database-queries-with-virtual-threads-benchmarks-code-5140deae9a76)

---

## 4. TNG 오픈소스 벤치마크 (GitHub, 2024)

**환경**: JMH, Java 21, HikariCP (pool=10), 1,000 concurrent requests, JDBC / Hibernate / Reactor Core

- Platform Threads + JDBC
- Virtual Threads + JDBC
- Virtual Threads + Hibernate
- Reactor Core + R2DBC (비교 대상)

결과: ops/s 기준 Virtual Threads + JDBC ≈ Reactor Core + R2DBC (거의 동등)

> 즉, 비동기 코드 리팩터링 없이 Virtual Threads만으로 Reactive 수준의 처리량 달성 가능.

**출처**: [TNG/java-virtual-thread-benchmark (GitHub)](https://github.com/TNG/java-virtual-thread-benchmark)

---

## 5. 연산 유형별 특성 요약

| 상황                 | 추천                              | 이유                                                             |
|--------------------|---------------------------------|----------------------------------------------------------------|
| **JDBC 쿼리 집중**     | Virtual Threads                 | I/O 대기 중 carrier 반납 → 효율 ×2~5                                  |
| **CPU bound 연산**   | Platform Threads (ForkJoinPool) | Virtual 오버헤드 무의미, carrier 수 = CPU 코어 수가 최적                     |
| **+latency 환경**    | Virtual Threads                 | ForkJoinPool 역전, CachedPool 메모리 위험                             |
| **대규모 병렬 쿼리**      | Virtual Threads                 | `Thread.startVirtualThread { repo.find() }` 패턴으로 WebFlux 수준 달성 |
| **Reactive 대체 여부** | Virtual Threads ≈ R2DBC         | ops/s 동등, 코드 복잡도는 Virtual Threads가 훨씬 낮음                       |

---

## 6. Pinning 주의사항 (성능 함정)

Virtual Threads가 **platform thread에 고정(pinned)** 되는 경우 → 이점 소멸:

```java
// ❌ Pinning 발생 — synchronized 블록 내 I/O
synchronized (lock){
        connection.

executeQuery(sql);  // Virtual Thread가 unmount 불가 → blocking
}

// ✅ ReentrantLock 사용 → Pinning 없음
        lock.

lock();
try{
        connection.

executeQuery(sql);
}finally{
        lock.

unlock();
}
```

**Pinning 유발 원인**:

- `synchronized` 블록 / 메서드 내 blocking I/O
- JNI 호출
- 일부 구버전 JDBC 드라이버 (MariaDB Connector 3.3.0+, PostgreSQL 42.6+ 에서 해결)

---

## 7. Kotlin/Spring Boot 적용 예시

```kotlin
// Virtual Threads로 병렬 DB 쿼리 (Spring Boot 3.x + Java 21)
// application.yaml
// spring.threads.virtual.enabled: true

// 병렬 쿼리 — 3개 쿼리를 동시에 실행, 총 지연 = max(각 쿼리 시간)
fun loadUserSummary(id: Long): UserSummary {
    val posts = Thread.startVirtualThread { postRepo.findByUser(id) }
    val orders = Thread.startVirtualThread { orderRepo.findByUser(id) }
    val profile = Thread.startVirtualThread { userRepo.findProfile(id) }
    return UserSummary(posts.join(), orders.join(), profile.join())
}
```

---

## 참고 자료

- [Benchmark Database Access with Java 21 Virtual Threads - adi.earth (2024)](https://adi.earth/posts/database-virtual-threads-benchmark/)
- [Benchmark JDBC connectors and Java 21 Virtual Threads - MariaDB Blog (2023)](https://mariadb.com/resources/blog/benchmark-jdbc-connectors-and-java-21-virtual-threads/)
- [Spring Boot 2025: Parallel DB Queries with Virtual Threads - Stackademic](https://javascript.plainenglish.io/spring-boot-2025-parallel-database-queries-with-virtual-threads-benchmarks-code-5140deae9a76)
- [TNG/java-virtual-thread-benchmark - GitHub](https://github.com/TNG/java-virtual-thread-benchmark)
- [Comparing Hibernate / Hibernate Reactive / Virtual Threads / R2DBC - Valensas (2024)](https://blog.valensas.com/comparing-hibernate-hibernate-reactive-hibernate-with-virtual-threads-and-r2dbc-a-performance-fa98c28df675)
- [Virtual Threads + HikariCP 2025 - Medium](https://medium.com/@karunakunwar899/virtual-threads-hikaricp-the-2025-formula-for-blazing-fast-spring-boot-database-performance-d3deb5b713ff)
