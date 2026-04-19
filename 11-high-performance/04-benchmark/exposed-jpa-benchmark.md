# Exposed vs JPA (Hibernate) CRUD 벤치마크

> **환경**: JMH (kotlinx-benchmark) / Java 21 / **PostgreSQL (Testcontainers)** / HikariCP (maxPoolSize=100) / Fork 1
> **측정**: Warmup 3~5회 × 1~2s, Measurement 5~10회 × 1~2s, Mode=avgt (낮을수록 빠름)
> **모델**: Person(10컬럼, 4KB picture), Department(8컬럼) → Employee(12컬럼, 8KB picture)
> **인덱스**: email(unique), name(composite), zipcode, position, salary, dept_id, code(unique)

---

## 1. Single Entity (Person — 10컬럼) CRUD

| 연산                     |     Exposed (μs) |         JPA (μs) | Exposed 배율  |
|------------------------|-----------------:|-----------------:|-------------|
| **create**             |   **1,196 ± 28** |      4,623 ± 100 | **3.9× 빠름** |
| **read** (DAO find)    |   **1,972 ± 54** |     11,602 ± 409 | **5.9× 빠름** |
| **update**             |   **1,980 ± 67** |     15,278 ± 596 | **7.7× 빠름** |
| **delete**             |   **1,945 ± 55** |     12,059 ± 398 | **6.2× 빠름** |
| **batchCreate** (100건) | **51,203 ± 640** | 423,816 ± 37,053 | **8.3× 빠름** |
| **readAll** (엔티티 로드)   |         713 ± 13 |         705 ± 20 | 유사 (1.0×)   |

---

## 2. One-to-Many (Department → 20 Employees — 8+12컬럼) CRUD

| 연산                               |        Exposed (μs) |         JPA (μs) | Exposed 배율   |
|----------------------------------|--------------------:|-----------------:|--------------|
| **create** (1부서+20명)             |    **13,489 ± 675** |   87,556 ± 6,091 | **6.5× 빠름**  |
| **read** (DAO eager load)        |    **16,493 ± 840** | 209,932 ± 15,693 | **12.7× 빠름** |
| **update** (부서+전직원)              |    **14,448 ± 523** |  269,027 ± 5,002 | **18.6× 빠름** |
| **delete** (CASCADE)             |  **15,136 ± 1,024** | 226,083 ± 12,795 | **14.9× 빠름** |
| **batchCreate** (10부서×20명)       | **136,876 ± 5,550** | 838,145 ± 59,213 | **6.1× 빠름**  |
| **readAll** (eager + JOIN FETCH) |             716 ± 9 |         723 ± 20 | 유사 (1.0×)    |

---

## 3. 동시성 벤치마크 — Platform Threads vs Virtual Threads

50건 동시 실행 (FixedThreadPool(50) vs VirtualThreadPerTaskExecutor), HikariCP maxPoolSize=100

### Exposed

| 연산                            |      Platform (ms) |   Virtual (ms) | VT 효과    |
|-------------------------------|-------------------:|---------------:|----------|
| **concurrentCreate** (50건)    |    **9,182 ± 374** |    9,331 ± 419 | 유사       |
| **concurrentRead** (50건)      | **64,464 ± 7,601** | 72,543 ± 8,829 | PT 소폭 우위 |
| **concurrentMixed** (70R/30W) | **39,298 ± 1,379** |   39,786 ± 990 | 유사       |

### JPA

| 연산                            |        Platform (ms) |         Virtual (ms) | VT 효과    |
|-------------------------------|---------------------:|---------------------:|----------|
| **concurrentCreate** (50건)    |       25,673 ± 3,207 |   **25,297 ± 2,203** | 유사       |
| **concurrentRead** (50건)      | **272,958 ± 16,981** |     285,892 ± 14,908 | PT 소폭 우위 |
| **concurrentMixed** (70R/30W) |     164,089 ± 22,645 | **157,837 ± 21,420** | 4% 향상    |

### Exposed vs JPA 동시성 비교

| 연산                   | Exposed (ms) | JPA (ms) | Exposed 배율  |
|----------------------|-------------:|---------:|-------------|
| **concurrentCreate** |        9,182 |   25,297 | **2.8× 빠름** |
| **concurrentRead**   |       64,464 |  272,958 | **4.2× 빠름** |
| **concurrentMixed**  |       39,298 |  157,837 | **4.0× 빠름** |

---

## 4. Virtual Threads 분석

| 관찰               | 설명                                              |
|------------------|-------------------------------------------------|
| **VT 효과 미미**     | maxPoolSize=100에서도 VT가 유의미한 향상을 보이지 않음          |
| **원인 1**         | Docker 로컬 PostgreSQL — 네트워크 지연이 거의 없어 I/O 대기 최소 |
| **원인 2**         | 50건 동시 < 100 커넥션 — 커넥션 대기 없이 즉시 할당              |
| **원인 3**         | JMH 단일 fork — OS 스케줄링 오버헤드가 VT 이점을 상쇄           |
| **VT 효과 극대화 조건** | 원격 DB + 높은 네트워크 지연 + 커넥션 풀 < 동시 요청 수            |

---

## 5. 종합 비교

```
                  Exposed 우위                                              JPA 우위
                  ◄────────────────────────────────────────┼──────────────────►
Single Entity:
  create          █████████ (3.9×)
  read            ██████████████ (5.9×)
  update          ███████████████████ (7.7×)
  delete          ███████████████ (6.2×)
  batchCreate     ████████████████████ (8.3×)
  readAll                                                  ■ (유사)

One-to-Many:
  create          ████████████████ (6.5×)
  read            ████████████████████████████████ (12.7×)
  update          ██████████████████████████████████████████████ (18.6×)
  delete          █████████████████████████████████████████ (14.9×)
  batchCreate     ███████████████ (6.1×)
  readAll                                                  ■ (유사)

Concurrent (50 tasks):
  create          ██████████ (2.8×)
  read            █████████████ (4.2×)
  mixed           ████████████ (4.0×)
```

---

## 6. 결론

| 패턴                   | 결과                       | 이유                                        |
|----------------------|--------------------------|-------------------------------------------|
| **단건 CRUD**          | **Exposed 3.9~7.7× 빠름**  | SQL 직접 생성, dirty checking/프록시 없음          |
| **One-to-Many CRUD** | **Exposed 6.1~18.6× 빠름** | DSL 직접 SQL, JPA는 엔티티 그래프 로드+변경 감지+cascade |
| **배치 INSERT**        | **Exposed 6.1~8.3× 빠름**  | 단일 트랜잭션 순차 INSERT vs JPA cascade+flush    |
| **전체 조회 (readAll)**  | **유사**                   | 둘 다 DB→엔티티 매핑, 차이 미미                      |
| **동시성**              | **Exposed 2.8~4.2× 빠름**  | 경량 트랜잭션 + 적은 오버헤드                         |
| **Virtual Threads**  | **효과 미미**                | 로컬 Docker DB + 충분한 커넥션 풀에서는 I/O 대기 없음     |

### 성능 차이의 근본 원인

| JPA (Hibernate) 오버헤드        | Exposed 접근 방식               |
|-----------------------------|-----------------------------|
| 프록시 객체 생성 + lazy loading    | DAO 직접 매핑, eager loading 명시 |
| dirty checking (필드 변경 감지)   | DSL로 SQL 직접 생성              |
| 1차 캐시 관리                    | 트랜잭션 범위 내 직접 쿼리             |
| cascade 전파 + orphan removal | 명시적 INSERT/DELETE           |
| JPQL → SQL 파싱 변환            | Kotlin DSL → SQL 직접 빌드      |

### 프레임워크 선택 기준

- **성능 최우선, SQL 제어 필요** → **Exposed** (전 영역 3~19배 빠름)
- **복잡한 도메인 모델 + 2차 캐시 + Spring Data 생태계** → JPA
- **높은 동시성** → Exposed 권장 (VT는 원격 DB 환경에서 추가 이점)

---

## 벤치마크 환경 상세

| 항목              | 값                                       |
|-----------------|-----------------------------------------|
| JMH             | 1.37 (via kotlinx-benchmark)            |
| Kotlin          | 2.3.20                                  |
| Java            | 21                                      |
| Exposed         | 1.1.1                                   |
| Hibernate       | 6.6.44.Final                            |
| Database        | **PostgreSQL (Testcontainers, Docker)** |
| Connection Pool | HikariCP 7.0.2 (**maxPoolSize=100**)    |
| OS              | macOS (Darwin)                          |
| Fork            | 1                                       |
| Warmup          | 3-5 iterations × 1-2s                   |
| Measurement     | 5-10 iterations × 1-2s                  |
| Person          | 10컬럼 (4KB picture, 500자 bio)            |
| Department      | 8컬럼                                     |
| Employee        | 12컬럼 (8KB picture, 450자 bio)            |
| One-to-Many     | Department → **20 Employees**           |
| 동시성             | **50 concurrent tasks**                 |

---

## 7. 캐시 전략 비교 벤치마크 (CacheStrategyComparisonBenchmark)

> **환경**: JMH / Java 21 / Caffeine near-cache + 인메모리 Map / Fork 1
> **측정**: Warmup 5회 × 500ms, Measurement 10회 × 1s, Mode=avgt (낮을수록 빠름)
> **모델**: UserPayload (id + 256 bytes)

### 전략별 성능 비교

| 전략             | 워크로드       | Score (µs/op) | NoCache 대비    |
|----------------|------------|---------------|---------------|
| `NO_CACHE`     | READ_HEAVY | 517.520       | —             |
| `NO_CACHE`     | WRITE_HEAVY| 507.766       | —             |
| `READ_THROUGH` | READ_HEAVY | 94.320        | **5.5× 빠름**   |
| `READ_THROUGH` | WRITE_HEAVY| 468.735       | 1.1× 빠름       |
| `WRITE_THROUGH`| READ_HEAVY | 52.103        | **9.9× 빠름**   |
| `WRITE_THROUGH`| WRITE_HEAVY| 445.363       | 1.1× 빠름       |

### 워크로드 분석

| 워크로드       | 읽기:쓰기 비율 | 최적 전략                | 근거                                        |
|------------|----------|----------------------|-------------------------------------------|
| READ_HEAVY | 90:10    | **WRITE_THROUGH**    | 쓰기 시 캐시 워밍으로 후속 읽기 모두 히트, 9.9× 향상        |
| WRITE_HEAVY| 10:90    | 전략 무관 (NoCache와 유사) | 읽기 비율 낮아 캐시 효과 미미, DB 쓰기가 병목              |

### 결론

| 패턴                    | 권장 전략              | 이유                                         |
|-----------------------|--------------------|--------------------------------------------|
| **읽기 위주 (90%+)**     | Write-Through      | 쓰기가 캐시를 워밍 → 읽기 히트율 극대화 (9.9× 향상)         |
| **읽기 위주 (캐시 미스 빈번)** | Read-Through       | 미스 시 자동 로딩으로 점진적 워밍 (5.5× 향상)             |
| **쓰기 위주 (90%+)**     | NoCache 또는 Write-Behind | 캐시 오버헤드 대비 효과 미미, 비동기 전파 고려               |
| **혼합 워크로드**           | Write-Through      | 쓰기로 캐시 일관성 유지 + 읽기 성능 보장                   |

---

## 벤치마크 실행 방법

```bash
# smoke 프로파일 (빠른 검증, ~3분)
./gradlew :04-benchmark:smokeBenchmark

# main 프로파일 (정밀 측정, ~14분)
./gradlew :04-benchmark:benchmark

# Markdown 리포트 생성
./gradlew :04-benchmark:benchmarkMarkdown
```

> **주의**: Docker가 실행 중이어야 PostgreSQL Testcontainer가 시작됩니다.
