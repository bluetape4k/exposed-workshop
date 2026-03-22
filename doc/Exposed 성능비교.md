# Exposed vs JPA/Hibernate 성능 비교 (2025 기준)

> **주의**: Exposed 직접 실측 벤치마크는 공개된 자료가 거의 없음.
> 아래 수치는 설계 철학이 유사한 **jOOQ vs Hibernate JMH 실험** 결과를 Exposed에 유추 적용한 것임.

---

## 1. JMH 실측 데이터: Native SQL vs HQL vs CriteriaBuilder vs jOOQ

**출처**: Zeyad Ahmed, JMH 1.37 / Java 17 / Spring Boot 3.1.2 / PostgreSQL (IMDb DB, avgt 모드)

| 쿼리 방식             | findAll() 소형 DB     | findAll() 대형 DB | findAllByCategory() 중형 DB |
|-------------------|---------------------|-----------------|---------------------------|
| Native SQL (JDBC) | 기준                  | 기준              | 기준                        |
| HQL (Hibernate)   | **+120% 느림**        | **+580ms 느림**   | 70% 느림                    |
| CriteriaBuilder   | HQL과 유사             | HQL+186ms 더 느림  | **63% 느림**                |
| Querydsl          | CriteriaBuilder와 유사 | —               | —                         |
| jOOQ              | Native SQL에 근접      | Native SQL에 근접  | Native SQL에 근접            |

> jOOQ ≈ Exposed DSL (SQL-first 설계 동일) → Exposed도 유사한 경향 예상

**출처
**: [JMH Benchmarking: Native Query, HQL, CriteriaBuilder, Querydsl, jOOQ - Zeyad Ahmed (2024)](https://medium.com/@zeyadahmedcs/comprehensive-guide-to-decoding-the-java-persistence-puzzle-jmh-benchmarking-of-native-query-hql-108fd7220c54)

---

## 2. JMH 실측 데이터: Spring Boot JPQL vs Native SQL vs Specification

**출처**: Lakshika, Spring Boot 3.3.2 / PostgreSQL 15 / Apple M2 / 100만 행 / HikariCP

| 쿼리 방식             | 상대 성능                               |
|-------------------|-------------------------------------|
| Native SQL        | 기준 (가장 빠름)                          |
| JPQL              | Native SQL 대비 **약 35% 느림**          |
| Specification API | JPQL보다 소폭 느림 (동적 predicate 빌딩 오버헤드) |

**출처
**: [Spring Boot Query Performance: JPQL vs Native vs Specification - Stackademic (2025)](https://blog.stackademic.com/spring-boot-query-performance-deep-dive-jpql-vs-native-vs-specification-with-benchmarks-77c6b655bb78)

---

## 3. 학술 JMH 실측: Hibernate(CriteriaBuilder) vs jOOQ

**출처**: Hetman & Miłosz, Lublin University of Technology, JCSI 35 (2025) 209–215
**환경**: OpenJDK 17 / PostgreSQL 16.4 (bare metal) / Spring Boot 3.3.4 / Chinook DB (track 210만 건 등)
**JMH 설정**: warmup 3회, 측정 10회 × 각 30초

| 가설                                | 결과                    |
|-----------------------------------|-----------------------|
| H1. jOOQ가 Hibernate보다 평균 실행 시간 낮음 | **확인됨** (단순·복잡 쿼리 모두) |
| H2. jOOQ가 멀티코어에서 더 높은 처리량         | **확인됨**               |

> 구체적 수치(표/그래프)는 논문 PDF 원문 참조 필요
> **[PDF 원문](https://ph.pollub.pl/index.php/jcsi/article/download/7306/5024/34434)**

---

## 4. Exposed GitHub 이슈 실측 (단일 스레드)

**출처**: JetBrains/Exposed #776, #1312

| 방식                | 처리량 (단일 스레드) |
|-------------------|--------------|
| Native SQL (JDBC) | ~5,000 쿼리/초  |
| Exposed DSL       | ~400 쿼리/초    |

> **격차 원인**: Exposed DSL의 쿼리 빌더가 매 호출마다 SQL 문자열을 동적 생성하는 오버헤드.
> 실제 I/O 바운드 환경에서는 DB 왕복 지연이 지배적이므로 체감 차이는 훨씬 줄어듦.

---

## 5. N+1 문제 - 실전 사례

Hibernate lazy loading 기본값으로 인한 N+1이 실제 운영에서 8초 페이지 로딩을 유발한 사례:

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

## 6. 한계 및 해석 주의사항

- Exposed 직접 벤치마크 자료는 사실상 없음 → jOOQ 수치로 유추
- 위 수치는 모두 **합성 워크로드** 기반; 실제 앱은 커넥션 풀·네트워크·인덱스 등이 지배적
- Hibernate도 **fetch join + DTO projection** 적용 시 Native SQL에 근접 가능
- Exposed의 낮은 raw 처리량은 I/O 바운드 환경에서 체감 차이 거의 없음

---

## 7. 선택 가이드

| 선택            | 적합한 경우                                    |
|---------------|-------------------------------------------|
| **Exposed**   | Kotlin-first, SQL 제어권 필요, 낮은 오버헤드, N+1 방지 |
| **Hibernate** | 2차 캐시 활용, 복잡한 도메인 모델, Spring Data JPA 생태계 |

---

## 참고 자료

- [JMH: Native Query / HQL / CriteriaBuilder / jOOQ 비교 - Zeyad Ahmed (2024)](https://medium.com/@zeyadahmedcs/comprehensive-guide-to-decoding-the-java-persistence-puzzle-jmh-benchmarking-of-native-query-hql-108fd7220c54)
- [Spring Boot JPQL vs Native vs Specification 벤치마크 - Stackademic (2025)](https://blog.stackademic.com/spring-boot-query-performance-deep-dive-jpql-vs-native-vs-specification-with-benchmarks-77c6b655bb78)
- [Hibernate vs jOOQ JMH 논문 - Lublin Univ. JCSI 35 (2025)](https://ph.pollub.pl/index.php/jcsi/article/download/7306/5024/34434)
- [Hibernate vs. Exposed Round 3 (성능 심화) - SoftwareML (2025)](https://softwaremill.com/hibernate-vs-exposed-choosing-kotlins-best-persistence-tool-round-3/)
- [ORM Battle 2025: Hibernate vs jOOQ vs JDBC - Medium](https://medium.com/javarevisited/the-great-orm-debate-hibernate-vs-jooq-vs-plain-jdbc-e271b95a2ef5)
- [JetBrains/Exposed #776 - Exposed slow (GitHub)](https://github.com/JetBrains/Exposed/issues/776)
- [Java Persistence API Benchmark - jpab.org](https://www.jpab.org/)
