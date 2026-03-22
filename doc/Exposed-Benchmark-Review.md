# Exposed / JPA / jOOQ / JDBC 성능 벤치마크 문헌 조사 (2025)

> **주의**: Exposed 직접 실측 벤치마크는 공개된 자료가 거의 없음.
> jOOQ와 동일한 SQL-first 설계 철학을 공유하므로, jOOQ 수치가 Exposed DSL의 유사한 성능 경향을 시사함.

---

## 1. Exposed DSL vs Native SQL (JDBC) — 단일 스레드 실측

**출처**: [JetBrains/Exposed Issue #1312 (2021)](https://github.com/JetBrains/Exposed/issues/1312)

| 방식                | 처리량 (단일 스레드) |
|-------------------|--------------|
| Native SQL (JDBC) | ~5,000 쿼리/초  |
| Exposed DSL       | ~400 쿼리/초    |

**격차 원인**: Exposed DSL의 쿼리 빌더가 매 호출마다 SQL 문자열을 동적으로 생성하는 오버헤드. 실제 I/O 바운드 운영 환경에서는 DB 왕복 지연이 지배적이므로 체감 차이는 훨씬 줄어듦.

추가 이슈: [Issue #898](https://github.com/JetBrains/Exposed/issues/898) — 35만 행 결과셋에서 쿼리 자체는 120ms이나 ResultRows 취득까지 약 1.5분 소요 (대용량 ResultSet 처리 오버헤드).

---

## 2. Native SQL vs HQL vs CriteriaBuilder vs jOOQ — JMH 실측

**출처
**: [Zeyad Ahmed, Medium (2024-01-25)](https://medium.com/@zeyadahmedcs/comprehensive-guide-to-decoding-the-java-persistence-puzzle-jmh-benchmarking-of-native-query-hql-108fd7220c54)
**환경**: JMH 1.37 / Java 17 / Spring Boot 3.1.2 / PostgreSQL (IMDb DB, avgt 모드)

| 쿼리 방식             | findAll() 소형 DB     | findAll() 대형 DB | findAllByCategory() 중형 DB |
|-------------------|---------------------|-----------------|---------------------------|
| Native SQL (JDBC) | 기준 (가장 빠름)          | 기준              | 기준                        |
| HQL (Hibernate)   | **+120% 느림**        | **+580ms 느림**   | 70% 느림                    |
| CriteriaBuilder   | HQL과 유사             | HQL+186ms 더 느림  | 63% 느림                    |
| Querydsl          | CriteriaBuilder와 유사 | —               | —                         |
| jOOQ              | Native SQL에 근접      | Native SQL에 근접  | Native SQL에 근접            |

> **Exposed DSL과의 관계**: jOOQ와 Exposed DSL은 동일한 SQL-first 설계 철학을 공유하므로, jOOQ 수치가 Exposed DSL의 유사한 성능 경향을 시사함.

---

## 3. Spring Boot JPQL vs Native SQL vs Specification — JMH 실측

**출처
**: [Stackademic (2025)](https://blog.stackademic.com/spring-boot-query-performance-deep-dive-jpql-vs-native-vs-specification-with-benchmarks-77c6b655bb78)
**환경**: Spring Boot 3.3.2 / PostgreSQL 15 / Apple M2 / 100만 행 / HikariCP

| 쿼리 방식             | 상대 성능                               |
|-------------------|-------------------------------------|
| Native SQL        | 기준 (가장 빠름)                          |
| JPQL              | Native SQL 대비 **약 35% 느림**          |
| Specification API | JPQL보다 소폭 느림 (동적 predicate 빌딩 오버헤드) |

---

## 4. Hibernate(CriteriaBuilder) vs jOOQ — 학술 JMH 논문

**출처
**: [Hetman & Miłosz, Lublin University of Technology, JCSI 35 (2025) 209–215](https://ph.pollub.pl/index.php/jcsi/article/download/7306/5024/34434)
**환경**: OpenJDK 17 / PostgreSQL 16.4 (bare metal) / Spring Boot 3.3.4 / Chinook DB (track 210만 건 등)
**JMH 설정**: warmup 3회, 측정 10회 × 각 30초

| 가설                                | 결과                    |
|-----------------------------------|-----------------------|
| H1. jOOQ가 Hibernate보다 평균 실행 시간 낮음 | **확인됨** (단순·복잡 쿼리 모두) |
| H2. jOOQ가 멀티코어에서 더 높은 처리량         | **확인됨**               |

> 구체적 수치(표/그래프)는 논문 PDF 원문에서 확인 필요. CC BY 4.0 라이선스로 공개.

---

## 5. Hibernate vs Exposed 심층 비교 — SoftwareMill 3부작 (2025)

**출처**: Szymon Winiarz 저 (SoftwareMill, 2025년 6월)

- [Round 1 (2025-06-03)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-1/) — 설정, 데이터 모델링, 스키마 생성 비교
- [Round 2 (2025-06-09)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-2/) — CRUD, 쿼리 방식, 트랜잭션 비교
- [Round 3 (2025-06-16)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-3/) —
  **성능 심화: N+1, lazy loading, batching**

### Round 3 핵심 발견사항

- **N+1 문제**: Hibernate의 lazy loading 기본값이 N+1 쿼리 유발. Exposed는 JOIN 명시 필수 구조로 N+1을 **구조적으로 방지**
- **Batching**: JDBC 드라이버 내장 배치 기능으로 네트워크 콜 최소화. Hibernate도 `hibernate.jdbc.batch_size` 설정으로 배치 가능하나 기본 비활성
- **결론**: 성능 최적화 관점에서 둘 다 적절히 설정하면 유사한 수준 달성 가능하나, Exposed는 SQL 제어권이 높아 **최적화 예측 가능성이 우수**

### N+1 문제 코드 예시

```java
// Hibernate - product.getTags() 접근 시마다 추가 SELECT 발생
Product product = productRepository.findById(1L).orElseThrow();
product.

getTags();      // SELECT * FROM tags WHERE product_id = 1
product.

getCategory();  // SELECT * FROM category WHERE product_id = 1
// → 단 1개 상품 조회에 수십 개 쿼리 실행
```

```kotlin
// Exposed - JOIN 명시 필수 → N+1 구조적으로 방지
(Products innerJoin Tags).select { Products.id eq 1 }
// → 항상 단일 쿼리
```

---

## 6. ORM Battle 2025: Hibernate vs jOOQ vs plain JDBC

**출처
**: [Devrim Ozcay, Medium (2025-06-19)](https://medium.com/javarevisited/the-great-orm-debate-hibernate-vs-jooq-vs-plain-jdbc-e271b95a2ef5)

핵심 발견사항:

- jOOQ의 plain JDBC 대비 오버헤드는 **쿼리당 1ms 미만**으로 사실상 무시 가능
- Hibernate는 CriteriaAPI/JPQL 사용 시 Native SQL 대비 **20~120% 느릴 수 있음**
- fetch join + DTO projection 적용 시 Hibernate도 Native SQL에 근접 가능

---

## 7. Virtual Threads + JDBC 성능 벤치마크

### 7.1 JMH 벤치마크 (adi.earth, 2024)

**출처**: [adi.earth JMH 벤치마크 (2024)](https://adi.earth/posts/database-virtual-threads-benchmark/)
**환경**: JMH / Java 21 / MacBook i9 8-core / Docker TestContainers / 5,000 queries/iteration

| 실행 방식                        | 단순 SELECT ops/s | 배율        |
|------------------------------|-----------------|-----------|
| CachedThreadPool (Platform)  | 1,997           | ×1        |
| VirtualThreadPerTaskExecutor | **5,094**       | **×2.54** |
| ForkJoinPool                 | 5,283           | ×2.64     |

### 7.2 MariaDB 공식 벤치마크 (2023)

**출처**: [MariaDB Blog (2023)](https://mariadb.com/resources/blog/benchmark-jdbc-connectors-and-java-21-virtual-threads/)

| 쿼리 유형                    | Virtual Threads 향상 배율 |
|--------------------------|-----------------------|
| `SELECT 1`               | **×9**                |
| 단일 행 SELECT (100 int 컬럼) | **×5**                |
| 1000행 SELECT             | **×3 이상**             |

> **시사점**: Exposed + Virtual Threads (Java 21) 조합은 Reactive/R2DBC 수준의 처리량을 **동기 코드**로 달성 가능.

---

## 8. JPAB (Java Persistence API Benchmark)

**출처**: [jpab.org](https://www.jpab.org/)

Hibernate, EclipseLink, OpenJPA, DataNucleus × MySQL/PostgreSQL/H2 등 조합 CRUD 벤치마크.

> **주의**: 사이트 데이터가 2010~2012년 기준으로 오래됨. 현대 버전과의 수치 차이 있을 수 있으므로 참고용으로만 활용 권장.

---

## 종합 비교표

| 비교                                  | 성능 우위                    | 근거                              |
|-------------------------------------|--------------------------|---------------------------------|
| Exposed DSL vs JDBC (단일 스레드 raw)    | JDBC ×12.5 빠름            | GitHub Issue #1312              |
| jOOQ vs JDBC                        | jOOQ ≈ JDBC (차이 <1ms/쿼리) | Zeyad Ahmed JMH + 공식 문서         |
| jOOQ vs Hibernate (단순 쿼리)           | jOOQ 최대 **120% 빠름**      | Zeyad Ahmed (2024)              |
| Native SQL vs JPQL                  | Native SQL **35% 빠름**    | Stackademic (2025)              |
| jOOQ vs Hibernate (학술 JMH)          | jOOQ 우위 확인               | Hetman & Miłosz JCSI 35 (2025)  |
| Virtual Threads vs Platform Threads | VT **×2.2 ~ ×9 향상**      | adi.earth + MariaDB (2024/2023) |
| Exposed DSL ≈ jOOQ (유추)             | 유사 (SQL-first 동일 설계)     | 아키텍처 유사성                        |

---

## 한계 및 해석 주의사항

1. **Exposed 직접 벤치마크 공개 자료가 사실상 없음** — jOOQ 수치로 유추하는 것이 현재 최선
2. 모든 수치는 합성 워크로드 기반; 실제 앱에서는 커넥션 풀·네트워크·인덱스가 지배적
3. Hibernate도 fetch join + DTO projection + 2차 캐시 적용 시 Native SQL에 근접 가능
4. Exposed의 낮은 raw 처리량(400 qps)은 I/O 바운드 환경에서 체감 차이 거의 없음

---

## 선택 가이드

| 선택             | 적합한 경우                                    |
|----------------|-------------------------------------------|
| **Exposed**    | Kotlin-first, SQL 제어권 필요, 낮은 오버헤드, N+1 방지 |
| **Hibernate**  | 2차 캐시 활용, 복잡한 도메인 모델, Spring Data JPA 생태계 |
| **jOOQ**       | SQL-first, 타입 세이프 쿼리, Java 프로젝트           |
| **Plain JDBC** | 최대 성능, 간단한 쿼리, 프레임워크 오버헤드 제거              |

---

## 참고 자료

- [JMH: Native Query / HQL / CriteriaBuilder / jOOQ 비교 - Zeyad Ahmed (2024)](https://medium.com/@zeyadahmedcs/comprehensive-guide-to-decoding-the-java-persistence-puzzle-jmh-benchmarking-of-native-query-hql-108fd7220c54)
- [Spring Boot JPQL vs Native vs Specification 벤치마크 - Stackademic (2025)](https://blog.stackademic.com/spring-boot-query-performance-deep-dive-jpql-vs-native-vs-specification-with-benchmarks-77c6b655bb78)
- [Hibernate vs jOOQ JMH 논문 - Lublin Univ. JCSI 35 (2025)](https://ph.pollub.pl/index.php/jcsi/article/download/7306/5024/34434)
- [Hibernate vs. Exposed Round 1 (설정·모델링) - SoftwareMill (2025-06-03)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-1/)
- [Hibernate vs. Exposed Round 2 (CRUD·트랜잭션) - SoftwareMill (2025-06-09)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-2/)
- [Hibernate vs. Exposed Round 3 (성능 심화) - SoftwareMill (2025-06-16)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-3/)
- [ORM Battle 2025: Hibernate vs jOOQ vs JDBC - Medium (2025-06-19)](https://medium.com/javarevisited/the-great-orm-debate-hibernate-vs-jooq-vs-plain-jdbc-e271b95a2ef5)
- [Virtual Threads JDBC Benchmark - adi.earth (2024)](https://adi.earth/posts/database-virtual-threads-benchmark/)
- [JDBC Connectors + Java 21 Virtual Threads - MariaDB Blog (2023)](https://mariadb.com/resources/blog/benchmark-jdbc-connectors-and-java-21-virtual-threads/)
- [JetBrains/Exposed Issue #1312 - DSL Low Performance](https://github.com/JetBrains/Exposed/issues/1312)
- [JetBrains/Exposed Issue #898 - Slow large result set](https://github.com/JetBrains/Exposed/issues/898)
- [TNG/java-virtual-thread-benchmark - GitHub](https://github.com/TNG/java-virtual-thread-benchmark)
- [JPAB - Java Persistence API Benchmark](https://www.jpab.org/)
