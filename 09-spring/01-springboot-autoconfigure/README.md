# 09 Spring: AutoConfiguration (01)

English | [한국어](./README.ko.md)

A module that integrates Exposed with minimal configuration using Spring Boot auto-configuration.
It uses the `SpringTransactionManager` and `DatabaseInitializer` beans provided by `spring-boot-autoconfigure`
to learn the pattern of connecting DataSource and transactions with a single `application.yml` file.

## Learning Goals

- Understand how Spring Boot auto-configuration registers `SpringTransactionManager` and `DatabaseInitializer` for Exposed.
- Learn to control DDL auto-generation and SQL logging via `application.yml` properties (`spring.exposed.*`).
- Customize Exposed behavior such as entity cache size by overriding the `DatabaseConfig` bean.
- Verify how to exclude conflicting Auto-Configurations using `@SpringBootApplication(exclude = [...])`.

## Prerequisites

- Spring Boot auto-configuration principles
- [`../04-exposed-ddl/01-connection/README.md`](../../04-exposed-ddl/01-connection/README.md)

## Architecture

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"'}}}%%
classDiagram
    class SpringBootApplication {
        +exclude: DataSourceTransactionManagerAutoConfiguration
    }
    class ExposedAutoConfiguration {
        <<AutoConfiguration>>
        +springTransactionManager() SpringTransactionManager
        +databaseInitializer() DatabaseInitializer
    }
    class SpringTransactionManager {
        +dataSource: DataSource
        +databaseConfig: DatabaseConfig
    }
    class DatabaseInitializer {
        +tables: List~Table~
        +generateDdl: Boolean
    }
    class DatabaseConfig {
        +maxEntitiesToStoreInCachePerEntity: Int
        +useNestedTransactions: Boolean
    }

    SpringBootApplication --> ExposedAutoConfiguration : auto-configure
    ExposedAutoConfiguration --> SpringTransactionManager : creates
    ExposedAutoConfiguration --> DatabaseInitializer : creates (when generate-ddl=true)
    SpringTransactionManager --> DatabaseConfig : uses

    style SpringBootApplication fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style ExposedAutoConfiguration fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style SpringTransactionManager fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style DatabaseInitializer fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style DatabaseConfig fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

## Key Concepts

### application.yml Configuration

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:test
    driver-class-name: org.h2.Driver
  exposed:
    generate-ddl: false   # When true, DatabaseInitializer runs SchemaUtils.create()
    show-sql: true        # Exposed SQL log output
```

### Overriding DatabaseConfig

```kotlin
@TestConfiguration
class CustomDatabaseConfigConfiguration {

    @Bean
    fun customDatabaseConfig(): DatabaseConfig = DatabaseConfig {
        maxEntitiesToStoreInCachePerEntity = 100
        useNestedTransactions = true
    }
}
```

### Excluding Auto-Configuration

```kotlin
@SpringBootApplication(
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class Application
```

The `DataSourceTransactionManager` registered by `DataSourceTransactionManagerAutoConfiguration` conflicts with Exposed's
`SpringTransactionManager`, so it must be excluded.

## Auto-Registered Bean Flow

```mermaid
%%{init: {'theme': 'base', 'backgroundColor': '#FAFAFA', 'themeVariables': {'background': '#FAFAFA', 'fontFamily': '"Comic Mono", "goorm sans code", "JetBrains Mono", "goorm sans"', 'actorBkg': '#E3F2FD', 'actorBorder': '#90CAF9', 'actorTextColor': '#1565C0', 'actorLineColor': '#90CAF9', 'activationBkgColor': '#E8F5E9', 'activationBorderColor': '#A5D6A7', 'labelBoxBkgColor': '#FFF3E0', 'labelBoxBorderColor': '#FFCC80', 'labelTextColor': '#E65100', 'loopTextColor': '#6A1B9A', 'noteBkgColor': '#F3E5F5', 'noteBorderColor': '#CE93D8', 'noteTextColor': '#6A1B9A', 'signalColor': '#1565C0', 'signalTextColor': '#1565C0'}}}%%
sequenceDiagram
    participant Boot as Spring Boot
    participant AC as ExposedAutoConfiguration
    participant STM as SpringTransactionManager
    participant DI as DatabaseInitializer

    Boot->>AC: Execute auto-configure
    AC->>STM: Register SpringTransactionManager bean
    Note over AC: Only when spring.exposed.generate-ddl=true
    AC->>DI: Register DatabaseInitializer bean
    DI->>DI: SchemaUtils.create(tables...)
    Boot->>STM: Delegate @Transactional requests
```

## Table Definition Example

```kotlin
object TestTable: IntIdTable("test_table") {
    val name = varchar("name", 100)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class TestEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TestEntity>(TestTable)

    var name by TestTable.name
    var createdAt by TestTable.createdAt
}
```

When `spring.exposed.generate-ddl=true` is set, `DatabaseInitializer` automatically runs `SchemaUtils.create()` on the scanned `Table` objects.

## How to Run

```bash
# Full module test
./gradlew :09-spring:01-springboot-autoconfigure:test

# Test log summary
./bin/repo-test-summary -- ./gradlew :09-spring:01-springboot-autoconfigure:test
```

## Practice Checklist

- Verify the difference in `DatabaseInitializer` bean presence when toggling `spring.exposed.generate-ddl=true/false`
- Validate that `maxEntitiesToStoreInCachePerEntity` value is reflected when overriding the `DatabaseConfig` bean
- Reproduce the transaction conflict error when starting without excluding `DataSourceTransactionManagerAutoConfiguration`

## Performance & Stability Checkpoints

- The auto-configuration default (`show-sql=true`) must be changed to `false` in production environments
- `generate-ddl=true` is for development/testing only; use migration tools (Flyway/Liquibase) in production

## Next Module

- [`../02-transactiontemplate/README.md`](../02-transactiontemplate/README.md)
