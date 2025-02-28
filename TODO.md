# TODO

## 1. Exposed Quick starts examples

- [ ] Spring Boot MVC Rest API with Exposed
- [ ] Spring Boot Webflux REST API with Exposed Suspended

## 2. Alternatives to JPA - Asynchronous Database Access

- [ ] Hibernate Reactive
- [ ] R2DBC
- [ ] Vertx SQL Client (with MyBatis SQL Builder)
- [ ] Exposed with Coroutines
- [ ] Virtual Threads with JPA

## 3. Test enviroment for Exposed

- [ ] JUnit 5
- [ ] TestContainers
- [ ] Databases
    - [ ] H2
    - [ ] Postgres
    - [ ] MySQL V8

## 3. Basic Features in Exposed

- [ ] SQL DSL
- [ ] Table 정의 및 생성
    - [ ] Table 종류
    - [ ] 다양한 컬럼 정의
    - [ ] Primary Key
        - [ ] Auto Increment
        - [ ] Client Generated
    - [ ] Foreign Key
    - [ ] Index
    - [ ] Unique Index
    - [ ] Default Value (DB Default Value, Client Default Value)

- [ ] SELECT
- [ ] INSERT
- [ ] UPDATE
- [ ] DELETE
- [ ] ORDER BY
- [ ] JOIN
- [ ] GROUP BY
- [ ] HAVING
- [ ] LIMIT & OFFSET
- [ ] Subquery
- [ ] Aggregation
- [ ] Transaction
- [ ] Batch Insert
- [ ] Batch Update
- [ ] Batch Delete
- [ ] INSERT INTO SELECT
- [ ] MergeFrom

## 4. Advanced Features in Exposed

- [ ] Expressions
- [ ] Functions
- [ ] Transformation (Column Values)
- [ ] Composite Primary Key
- [ ] CTE (Common Table Expression)

## 5. Define Custom ID Table & Entity

- [ ] Entity
    - [ ] Auto Increment ID
    - [ ] Client Generated ID

## 6. Advanded Data types

- [ ] Array Column for Postgres
- [ ] JSON
    - [ ] JSON Column
    - [ ] JSONB Column
- [ ] Java Time Column
- [ ] Kotlin DateTime Column
- [ ] Money Column

## 7. Define Other Data Types

- Custom EntityID
    - [ ] Snowflake ID for EntityID
    - [ ] Timebased UUID for EntityID
    - [ ] Base62 encoded UUID for EntityID
- Object Column
    - [ ] Binary Serialized Column
        - [ ] JDK Built-in Serializer
        - [ ] Kryo Serializer
        - [ ] Protobuf Serializer
        - [ ] Avro Serializer
        - [ ] Fury Serializer
    - [ ] Compressed Column
        - [ ] GZIP Column
        - [ ] LZ4 Column
        - [ ] Snappy Column
        - [ ] ZSTD Column
    - [ ] Encrypted Column
- JSON Column using Jackson
    - [ ] jackson function for JSON Column

## 7. Migration JPA Entity to Exposed Entity

- [ ] Simple Entity
- [ ] Relationships
    - [ ] One To One
    - [ ] One To Many
        - [ ] List
        - [ ] Set
        - [ ] Map
    - [ ] Many To One
    - [ ] Many To Many
- [ ] Hierarchy
    - [ ] Self Referencing
    - [ ] Relation Table
- [ ] Inheritance
    - [ ] Single Table
    - [ ] Table Per Class
    - [ ] Joined Table

## 8. Exposed with Coroutines

- [ ] Transactional Coroutines
    - [ ] newSuspendedTransaction

- [ ] Dispatchers
    - [ ] Dispatchers.IO
    - [ ] Dispatchers.VT

## 9. Integration with Spring Boot

- [ ] Using Spring Transaction
- [ ] Spring Boot MVC
    - [ ] Platform Threads
    - [ ] Virtual Threads
- [ ] Spring Boot Webflux + Coroutines
- [ ] Implement ExposedRepository

## 10. Migration Existing Database (Flyway)

- [ ] MigrationUtils in Exposed
- [ ] Using Flyway for Migration
