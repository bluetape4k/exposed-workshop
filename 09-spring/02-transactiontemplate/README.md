# 02-transactiontemplate

This module focuses on using Spring's
`TransactionTemplate` to programmatically manage database transactions with Exposed. It demonstrates a more explicit way to handle transactions compared to declarative transaction management, providing fine-grained control over transaction boundaries.

## Purpose

The primary goal of this module is to illustrate:

- How `TransactionTemplate` can be used to wrap a sequence of Exposed database operations within a transaction.
- The benefits of programmatic transaction management for complex transaction scenarios or when declarative
  `@Transactional` is not suitable.
- How to handle transaction commit and rollback explicitly or implicitly through the `TransactionTemplate`.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. The application will execute predefined database operations within transactions managed by
   `TransactionTemplate`. Observe the console output for transaction behavior.

## Key Features

- **Programmatic Transaction Control:** Direct control over transaction scope using `TransactionTemplate.execute()`.
- **Error Handling:** Graceful handling of exceptions within a transaction, leading to automatic rollback.
- **Flexibility:** Suitable for situations where transaction boundaries need to be defined precisely in code.

## Configuration

This module relies on standard Spring Boot data source configuration. No special Exposed or
`TransactionTemplate` configuration is usually required beyond the basic `spring.datasource` properties.

Example `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:transactiondb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
```

## Database Schema

A simple database schema (e.g., a `Products` or
`Accounts` table) is typically used to demonstrate transactional operations like creation, updates, and transfers.

## Examples

Using `TransactionTemplate` to perform a set of operations atomically:

```kotlin
@Service
class ProductService(private val transactionTemplate: TransactionTemplate) {

    init {
        transactionTemplate.execute {
            SchemaUtils.create(Products)
        }
    }

    fun createProduct(name: String, price: Double): ProductDto = transactionTemplate.execute {
        val id = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.price] = price
        }
        ProductDto(id.value, name, price)
    } ?: throw IllegalStateException("Failed to create product")

    fun updateProductPrice(id: Int, newPrice: Double): ProductDto? = transactionTemplate.execute {
        Products.update({ Products.id eq id }) {
            it[price] = newPrice
        }
        Products.select { Products.id eq id }.singleOrNull()?.let {
            ProductDto(it[Products.id].value, it[Products.name], it[Products.price])
        }
    }

    fun transferFunds(fromAccountId: Int, toAccountId: Int, amount: Double) {
        transactionTemplate.execute {
            // Deduct from sender
            Accounts.update({ Accounts.id eq fromAccountId }) {
                with(SqlExpressionBuilder) {
                    it[balance] = balance - amount
                }
            }

            // Simulate an error to demonstrate rollback
            if (amount > 1000) {
                throw RuntimeException("Transfer amount too high, rolling back!")
            }

            // Add to receiver
            Accounts.update({ Accounts.id eq toAccountId }) {
                with(SqlExpressionBuilder) {
                    it[balance] = balance + amount
                }
            }
        }
    }
}

object Products: IntIdTable("products") {
    val name = varchar("name", 255)
    val price = double("price")
}

object Accounts: IntIdTable("accounts") {
    val balance = double("balance")
}

data class ProductDto(val id: Int, val name: String, val price: Double)
```

## Further Reading

- [Exposed with Spring TransactionTemplate](https://debop.notion.site/JdbcTemplate-1c32744526b080959c0fcf671247e082)
- [Spring Framework Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Exposed Wiki: Transactions](https://github.com/JetBrains/Exposed/wiki/Transactions)
