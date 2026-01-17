# TODO

## 1. Exposed Quick starts examples

- [X] Spring Boot MVC Rest API with Exposed
- [X] Spring Boot Webflux REST API with Exposed Suspended

## 2. Alternatives to JPA - Asynchronous Database Access

- [X] Hibernate Reactive
- [X] R2DBC
- [X] Vertx SQL Client (with MyBatis SQL Builder)
- [X] Exposed with Coroutines
- [X] Virtual Threads with JPA

## 3. Test enviroment for Exposed

- [X] JUnit 5
- [X] TestContainers
- [X] Databases
    - [X] H2
    - [X] Postgres
    - [X] MySQL V8

## 3. Basic Features in Exposed

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

## 4. Advanced Features in Exposed

- [X] Expressions
- [X] Functions
- [X] Transformation (Column Values)
- [X] Composite Primary Key
- [X] CTE (Common Table Expression)

## 5. Define Custom ID Table & Entity

- [X] Entity
    - [X] Auto Increment ID
    - [X] Client Generated ID

## 6. Advanded Data types

- [X] Array Column for Postgres
- [X] JSON
    - [X] JSON Column
    - [X] JSONB Column
- [X] Java Time Column
- [X] Kotlin DateTime Column
- [X] Money Column

## 7. Define Other Data Types

- Custom EntityID
    - [X] Snowflake ID for EntityID
    - [X] Timebased UUID for EntityID
    - [X] Base62 encoded UUID for EntityID
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
- JSON Column using Jackson
    - [X] jackson function for JSON Column

## 7. Migration JPA Entity to Exposed Entity

- [X] Simple Entity
- [X] Relationships
    - [X] One To One
    - [X] One To Many
        - [X] List
        - [X] Set
        - [X] Map
    - [X] Many To One
    - [X] Many To Many
- [X] Hierarchy
    - [X] Self Referencing
    - [X] Relation Table
- [X] Inheritance
    - [X] Single Table
    - [X] Table Per Class
    - [X] Joined Table

## 8. Exposed with Coroutines

- [X] Transactional Coroutines
    - [X] newSuspendedTransaction

- [X] Dispatchers
    - [X] Dispatchers.IO
    - [X] Dispatchers.VT

## 9. Integration with Spring Boot

- [X] Using Spring Transaction
- [X] Spring Boot MVC
    - [X] Platform Threads
    - [X] Virtual Threads
- [X] Spring Boot Webflux + Coroutines
- [X] Implement ExposedRepository

## 10. Multi-tenant Application

- [X] Multitenant with Spring MVC
- [X] Multitenant with Spring MVC and Virtual Threads
- [X] Multitenant with Spring Webflux and Coroutines

## 11. Exposed with Redisson (Cache Strategy)

- [X] Read Through
- [X] Write Through
- [X] Write Behind

## 12. Migration Existing Database (Flyway)

- [ ] MigrationUtils in Exposed
- [ ] Using Flyway for Migration

## 13. Exposed with Spring Modulith

- [ ] Spring Modulith & Application Events
