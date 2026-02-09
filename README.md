# Exposed Workshop  (Kotlin Exposed 에 대한 학습 자료)

This repository contains a collection of examples and workshops demonstrating the usage of Exposed, a lightweight SQL framework for Kotlin.

## Kotlin Exposed 란?

Kotlin Exposed는 Kotlin 언어로 작성된 SQL 프레임워크입니다. 이 프레임워크는 SQL 쿼리를 작성하는 데 필요한 모든 기능을 제공하며, Kotlin의 강력한 타입 시스템을 활용하여 SQL 쿼리를 안전하게 작성할 수 있도록 도와줍니다.

![Kotlin Exposed Mindmap](doc/exposed-mindmap-felo-ai.jpg)

## Documents

여기 예제들의 상세 설명은 [Kotlin Exposed Book](https://debop.notion.site/Kotlin-Exposed-Book-1ad2744526b080428173e9c907abdae2) 에 있습니다.

## Modules

## Shared

### [Exposed Shared Tests](00-shared/exposed-shared-tests/README.md)

This module (`exposed-shared-tests`) serves as a foundational library within the
`exposed-workshop` project, providing a comprehensive suite of shared utilities and resources specifically designed for testing Exposed-based applications and examples.

## Spring Boot

### [Spring MVC with Exposed](01-spring-boot/spring-mvc-exposed/README.md)

This module (
`spring-mvc-exposed`) demonstrates how to build a Spring Boot application using the Spring MVC framework, with Exposed as the Object-Relational Mapping (ORM) library for database interactions.

### [Spring WebFlux with Exposed](01-spring-boot/spring-webflux-exposed/README.md)

This module (
`spring-webflux-exposed`) demonstrates how to build a reactive Spring Boot application using the Spring WebFlux framework, with Exposed as the Object-Relational Mapping (ORM) library for asynchronous database interactions.

## Alternatives to jpa

### [Hibernate Reactive Example](02-alternatives-to-jpa/hibernate-reactive-example/README.md)

This module (
`hibernate-reactive-example`) demonstrates how to build a reactive Spring Boot application using Hibernate Reactive as the Object-Relational Mapping (ORM) solution.

### [R2DBC Example](02-alternatives-to-jpa/r2dbc-example/README.md)

This module (
`r2dbc-example`) demonstrates how to build a reactive Spring Boot application leveraging R2DBC (Reactive Relational Database Connectivity) through Spring Data R2DBC.

### [Vert.x SQL Client Example](02-alternatives-to-jpa/vertx-sqlclient-example/README.md)

This module (
`vertx-sqlclient-example`) provides a collection of test-based examples demonstrating how to perform reactive database operations using the Vert.x SQL Client.

## Exposed Basic

### [Exposed DAO Example](03-exposed-basic/exposed-dao-example/README.md)

This module (
`exposed-dao-example`) provides practical examples of using the Data Access Object (DAO) pattern in Exposed, Kotlin's powerful ORM framework.

### [Exposed SQL DSL Example](03-exposed-basic/exposed-sql-example/README.md)

This module (
`exposed-sql-example`) provides practical examples of using Exposed's SQL Domain Specific Language (DSL) for direct interaction with a relational database.

## Exposed Ddl

### [Connection Management](04-exposed-ddl/01-connection/README.md)

This module (
`01-connection`) provides a set of examples and test cases demonstrating how to establish, configure, and manage database connections using the Exposed framework.

### [Schema Definition Language (DDL)](04-exposed-ddl/02-ddl/README.md)

This module (
`02-ddl`) provides a comprehensive set of examples and test cases demonstrating Exposed's Data Definition Language (DDL) capabilities.

## Exposed Dml

### [DML Basic Operations (01-dml)](05-exposed-dml/01-dml/README.md)

This module provides a comprehensive set of examples demonstrating various Data Manipulation Language (DML) operations using the Exposed framework.

### [Functions](05-exposed-dml/03-functions/README.md)

This module (
`03-functions`) provides a comprehensive set of examples and test cases demonstrating how to utilize various SQL functions within Exposed queries.

### [Entity API](05-exposed-dml/05-entities/README.md)

This module (
`05-entities`) provides a comprehensive set of examples and test cases demonstrating Exposed's powerful Entity API for Data Manipulation Language (DML) operations.

### [Transaction Management](05-exposed-dml/04-transactions/README.md)

This module (
`04-transactions`) provides a comprehensive set of examples and test cases demonstrating Exposed's robust transaction management capabilities.

### [Column types example](05-exposed-dml/02-types/README.md)

Exposed 의 다양한 컬럼 수형에 대한 예제를 제공합니다.

## Advanced

### [Exposed Crypt](06-advanced/01-exposed-crypt/README.md)

This module (
`01-exposed-crypt`) provides examples and test cases demonstrating the integration of encryption into Exposed applications using the
`exposed-crypt` extension.

### [Exposed Custom ColumnType example](06-advanced/06-custom-columns/README.md)

사용자 정의 컬럼 타입을 구현하는 방법을 보여주는 예제입니다.

### [Exposed Custom Column Type Table & Entity example](06-advanced/07-custom-entities/README.md)

`Exposed` 의 `IntIdEntity`, `LongIdEntity`, `UUIDIdEntity` 외에 다양한 `IdEntity` 를 구현해 봅니다.

### [Exposed Cryptography with Jasypt](06-advanced/10-exposed-jasypt/README.md)

Exposed Crypt 모듈은 **비결정적 암호화 방식** 을 사용하여, 암호화 방식이 암호화 할 때마다 매번 값이 변경됩니다.

### [Exposed JSON Column with Fastjson2](06-advanced/09-exposed-fastjson2/README.md)

DB의 JSON 컬럼을 [Fastjson2](https://github.com/alibaba/fastjson2) 라이브러리로 직렬화/역직렬화하는 방법을 알아봅니다.

### [Exposed JSON Column with Jackson](06-advanced/08-exposed-jackson/README.md)

DB의 JSON 컬럼을 [Jackson](https://github.com/fasterxml/jackson) 라이브러리로 직렬화/역직렬화하는 방법을 알아봅니다.

### [Exposed JSON Column with Jackson 3](06-advanced/11-exposed-jackson3/README.md)

DB의 JSON 컬럼을 [Jackson 3.x](https://github.com/fasterxml/jackson) 라이브러리로 직렬화/역직렬화하는 방법을 알아봅니다.

### [Exposed Json example](06-advanced/04-exposed-json/README.md)

`exposed-json` 모듈을 사용하여 `json`, `jsonb` 컬럼을 사용하는 예를 설명합니다.

### [Exposed JavaTime Integration](06-advanced/02-exposed-javatime/README.md)

This module (`02-exposed-javatime`) provides examples and test cases demonstrating the integration of
`java.time` (JSR 310) API types with the Exposed framework using the
`exposed-javatime` extension.

### [Exposed Kotlinx-Datetime Integration](06-advanced/03-exposed-kotlin-datetime/README.md)

This module (`03-exposed-kotlin-datetime`) provides examples and test cases demonstrating the integration of
`kotlinx.datetime` API types with the Exposed framework using the
`exposed-kotlin-datetime` extension.

### [Exposed Money example](06-advanced/05-exposed-money/README.md)

`exposed-money` 모듈을 사용하여 `Money` 수형에 대한 활용 방법을 보여주는 예제입니다.

## Jpa

### [Convert basic JPA features to Exposed](07-jpa/01-convert-jpa-basic/README.md)

기본적인 JPA 기능을 Exposed로 구현하기

### [Convert advanced JPA features to Exposed](07-jpa/02-convert-jpa-advanced/README.md)

Advanced JPA 기능을 Exposed로 구현하기

## Coroutines

### [Coroutines Examples](08-coroutines/01-coroutines-basic/README.md)

Exposed 를 Coroutines 환경에서 사용하는 방법을 알아봅니다.

### [Virtual Threads Examples](08-coroutines/02-virtualthreads-basic/README.md)

Exposed 를 Java 21 Virtual Threads 환경에서 사용하는 방법을 알아봅니다.

## Spring

### [Exposed + Spring Boot Cache](09-spring/06-spring-cache/README.md)

Spring Boot Cache 를 Exposed 와 함께 사용하는 방법을 알아보자

### [Exposed + Spring Boot Suspended Cache](09-spring/07-spring-suspended-cache/README.md)

Spring Boot 환경에서 Lettuce 를 활용한 Suspended Cache 를 Coroutines 환경에서 Exposed 와 함께 사용하는 방법을 알아보자

### [ExposedRepository with Coroutines](09-spring/05-exposed-repository-coroutines/README.md)

Repository 패턴을 사용한 ExposedRepository 를 Coroutines 환경에서 사용하기

### [ExposedRepository with Spring Web](09-spring/04-exposed-repository/README.md)

Spring Data Repository 패턴을 사용하여 Exposed를 사용한 Repository 구현하기

### [Spring Boot AutoConfigurations for Exposed](09-spring/01-springboot-autoconfigure/README.md)

Exposed 를 Spring Boot 에서 사용하기 위해 제공되는 AutoConfiguration 을 사용하는 방법을 알아봅니다.

### [Spring JdbcTemplate with Exposed](09-spring/02-transactiontemplate/README.md)

Spring의 `JdbcTemplate` 를 Exposed 와 함께 사용하는 밥법을 알아보자

### [Spring Trasnaction with Exposed](09-spring/03-spring-transaction/README.md)

Spring Boot의 AutoConfiguration을 사용하지 않고, Exposed 를 Spring Transaction 과 함께 사용하는 방법을 알아보자

## Multi tenant

### [Exposed + Spring Web + Multitenant](10-multi-tenant/01-multitenant-spring-web/README.md)

Spring Web Application 에서 Exposed 로 Multitenant 을 구현하는 방법을 설명합니다.

### [Exposed + Spring Web + VirtualThreads + Multitenant](10-multi-tenant/02-mutitenant-spring-web-virtualthread/README.md)

Spring Web Application 에서 Exposed, Virtual Threads 로 Multitenant 을 구현하는 방법을 설명합니다.

### [Exposed + Spring Webflux for Multitenant](10-multi-tenant/03-multitenant-spring-webflux/README.md)

Spring Webflux Application 에서 Coroutines, Exposed 를 이용하여 Multitenant 을 구현하는 방법을 설명합니다.

## High performance

### [캐시 전략 (Caching Strategies)](11-high-performance/01-cache-strategies/README.md)

다양한 캐시 전략에 대해 Redisson + Exposed 로 구현한 예제를 제공합니다.

### [캐시 전략 (Caching Strategies) with Coroutines](11-high-performance/02-cache-strategies-coroutines/README.md)

다양한 캐시 전략에 대해 Coroutines 환경에서 비동기로 작동하는 Redisson + Exposed 로 구현한 예제를 제공합니다.

### [고성능을 위한 유연한 RoutingDataSource 구성](11-high-performance/03-routing-datasource/README.md)

이 문서는 Spring Boot + Exposed 환경에서 Multi-Tenant 또는 Read Replica 구조를 구현할 때, 안전하고 유연한 방식으로 `DataSource` 라우팅을 구성하는 방법을 설명합니다.
