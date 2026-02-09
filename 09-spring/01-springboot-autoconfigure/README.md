# 01-springboot-autoconfigure

This module demonstrates the automatic configuration of Exposed within a Spring Boot application. It showcases how Spring Boot's auto-configuration mechanism can simplify the setup of Exposed, allowing developers to quickly get started with database interactions using the Exposed framework without extensive manual configuration.

## Purpose

The primary goal of this module is to illustrate:

- How Spring Boot detects and configures Exposed components (like `Database` connection,
  `TransactionManager`) when the necessary dependencies are present.
- The minimal setup required in `application.properties` or `application.yml` to connect Exposed to a database.
- The use of an `ExposedAutoConfiguration` class (or similar) that handles the creation of
  `Database` instances and transaction management.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. The application will initialize the Exposed database connection and, potentially, perform some basic schema creation or data insertion as part of its startup.

## Key Features

- **Automatic Database Connection:** Spring Boot automatically configures the `Database` object for Exposed based on
  `spring.datasource` properties.
- **Transaction Management:** Integration with Spring's transaction management system, enabling the use of
  `@Transactional` annotations with Exposed operations.
- **Simplified Setup:** Reduces boilerplate code for setting up Exposed.

## Configuration

Example `application.properties` (or `application.yml`):

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none # Important: Let Exposed handle DDL

# Exposed specific configuration (if any custom auto-configuration is provided)
# For example, specifying a package to scan for Exposed tables
# exposed.scan-packages=com.example.app.tables
```

## Database Schema

This module typically involves a simple schema (e.g.,
`Users` table) to demonstrate basic CRUD operations via Exposed. The schema creation might happen in an
`ApplicationRunner` or similar Spring Boot component.

## Examples

Basic interaction demonstrating auto-configuration:

```kotlin
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Component
class DataLoader(private val transactionTemplate: TransactionTemplate): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        transactionTemplate.execute {
            // Your Exposed database operations here
            // Example: Schema creation and data insertion
            SchemaUtils.create(Users)
            Users.insert {
                it[name] = "John Doe"
                it[email] = "john.doe@example.com"
            }
            Users.selectAll().forEach { println("User: ${it[Users.name]}, Email: ${it[Users.email]}") }
        }
    }
}

object Users: Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}
```

## Further Reading

- [Spring Boot AutoConfiguration](https://debop.notion.site/Spring-Boot-AutoConfiguration-1c32744526b080079af9eb44b62466d0)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/index.html)
- [Exposed Wiki](https://github.com/JetBrains/Exposed/wiki)
- [Exposed Spring Boot Starter (if applicable)](https://github.com/JetBrains/Exposed/tree/master/exposed-spring-boot-starter)
