# TODO

## 1. Exposed 빠른 시작 예제

- [X] Exposed 기반 Spring Boot MVC REST API
- [X] Exposed Suspended 기반 Spring Boot WebFlux REST API

## 2. JPA 대안 - 비동기 데이터베이스 접근

- [X] Hibernate Reactive
- [X] R2DBC
- [X] Vert.x SQL Client (MyBatis SQL Builder 사용)
- [X] Coroutines 기반 Exposed
- [X] JPA와 Virtual Threads

## 3. Exposed 테스트 환경

- [X] JUnit 5
- [X] Testcontainers
- [X] 데이터베이스
    - [X] H2
    - [X] PostgreSQL
    - [X] MySQL V8

## 4. Exposed 기본 기능

- [X] SQL DSL
- [X] Table 정의 및 생성
    - [X] Table 종류
    - [X] 다양한 컬럼 정의
    - [X] Primary Key
        - [X] Auto Increment
        - [X] Client Generated
    - [X] Foreign Key
    - [X] Index
    - [X] Unique Index
    - [X] Default Value (DB Default Value, Client Default Value)

- [X] SELECT
- [X] INSERT
- [X] UPDATE
- [X] DELETE
- [X] ORDER BY
- [X] JOIN
- [X] GROUP BY
- [X] HAVING
- [X] LIMIT & OFFSET
- [X] Subquery
- [X] Aggregation
- [X] Transaction
- [X] Batch Insert
- [X] Batch Update
- [X] Batch Delete
- [X] INSERT INTO SELECT
- [X] MergeFrom

## 5. Exposed 고급 기능

- [X] Expressions
- [X] Functions
- [X] Transformation (Column Values)
- [X] Composite Primary Key
- [X] CTE (Common Table Expression)

## 6. 커스텀 ID Table 및 Entity 정의

- [X] Entity
    - [X] Auto Increment ID
    - [X] Client Generated ID

## 7. 고급 데이터 타입

- [X] PostgreSQL Array Column
- [X] JSON
    - [X] JSON Column
    - [X] JSONB Column
- [X] Java Time Column
- [X] Kotlin DateTime Column
- [X] Money Column

## 8. 기타 데이터 타입 정의

- Custom EntityID
    - [X] EntityID용 Snowflake ID
    - [X] EntityID용 time-based UUID
    - [X] Base62 encoded UUID 기반 EntityID
- Object Column
    - [X] Binary Serialized Column
        - [X] JDK Built-in Serializer
        - [X] Kryo Serializer
        - [X] Protobuf Serializer
        - [X] Avro Serializer
        - [X] Fury Serializer
    - [X] Compressed Column
        - [X] GZIP Column
        - [X] LZ4 Column
        - [X] Snappy Column
        - [X] ZSTD Column
    - [X] Encrypted Column
- Jackson 기반 JSON Column
    - [X] JSON Column용 Jackson 함수

## 9. JPA Entity를 Exposed Entity로 마이그레이션

- [X] 단순 Entity
- [X] 관계
    - [X] One To One
    - [X] One To Many
        - [X] List
        - [X] Set
        - [X] Map
    - [X] Many To One
    - [X] Many To Many
- [X] 계층 구조
    - [X] Self Referencing
    - [X] Relation Table
- [X] 상속
    - [X] Single Table
    - [X] Table Per Class
    - [X] Joined Table

## 10. Coroutines와 Exposed

- [X] 트랜잭션 Coroutines
    - [X] newSuspendedTransaction

- [X] Dispatchers
    - [X] Dispatchers.IO
    - [X] Dispatchers.VT

## 11. Spring Boot 통합

- [X] Spring Transaction 사용
- [X] Spring Boot MVC
    - [X] Platform Threads
    - [X] Virtual Threads
- [X] Spring Boot WebFlux + Coroutines
- [X] ExposedRepository 구현

## 12. 멀티테넌트 애플리케이션

- [X] Spring MVC 기반 멀티테넌시
- [X] Spring MVC와 Virtual Threads 기반 멀티테넌시
- [X] Spring WebFlux와 Coroutines 기반 멀티테넌시

## 13. Redisson과 Exposed (Cache Strategy)

- [X] Read Through
- [X] Write Through
- [X] Write Behind

## 14. 기존 데이터베이스 마이그레이션 (Flyway)

- [ ] Exposed의 MigrationUtils
- [ ] Flyway 기반 Migration

## 15. Spring Modulith와 Exposed

- [ ] Spring Modulith 및 Application Events
