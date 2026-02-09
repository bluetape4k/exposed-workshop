# 05-exposed-repository-coroutines

This module explores integrating Exposed with Kotlin Coroutines in a Spring Boot application, specifically focusing on building non-blocking, asynchronous repositories. It demonstrates how to leverage coroutines for database operations to improve application responsiveness and resource utilization, especially in high-concurrency environments.

## Purpose

The primary goal of this module is to illustrate:

- How to perform Exposed database operations asynchronously using Kotlin Coroutines.
- Integrating `suspend` functions within repository methods to achieve non-blocking I/O.
- Configuring a coroutine-friendly transaction manager (e.g., `SpringTransactionManager` with
  `asCoroutineContextElement()`) for Exposed.
- Building reactive-style data access layers with Spring WebFlux (if applicable) or other coroutine-based frameworks.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. The application will typically expose non-blocking API endpoints (e.g., using Spring WebFlux) that utilize the coroutine-based repositories.

## Key Features

- **Non-blocking Database Access:** Execute Exposed queries without blocking the calling thread, improving scalability.
- **Structured Concurrency:** Use Kotlin Coroutines for safe and efficient concurrent database operations.
- **Spring Integration:** Seamlessly integrate coroutine-based Exposed repositories into a Spring application context.
- **Improved Responsiveness:
  ** Ideal for applications requiring high throughput and low latency, such as microservices and reactive systems.

## Configuration

This module requires setting up the `transaction` function to be coroutine-aware. This often involves providing a
`CoroutineDispatcher` and ensuring the transaction context is correctly propagated.

Example `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:coroutines_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
```

Custom `transaction` block for coroutines:

```kotlin
// DatabaseConfig.kt
@Configuration
class DatabaseConfig(private val dataSource: DataSource, private val transactionManager: PlatformTransactionManager) {
    @Bean
    fun initExposed() = runBlocking { // Or use a different scope if this is not meant to block
        Database.connect(dataSource)
        // Set up transaction manager for coroutines
        transactionManager.apply {
            TransactionManager.default = SpringTransactionManager(dataSource)
        }
        transaction {
            SchemaUtils.create(Customers)
        }
    }
}

// Custom Coroutine-aware transaction utility (often provided by Exposed extensions or custom)
// You might need a custom `transaction` wrapper that uses `SuspendedTransactionManager`
// or wraps the standard `transaction` in `withContext(Dispatchers.IO)` and a custom context element.
// For simplicity, often the default `transaction` provided by Exposed can work if `Database.connect` is set up correctly
// with a SpringTransactionManager that supports coroutines (e.g., via `asCoroutineContextElement()`).
```

## Database Schema

A typical example would involve a
`Customers` table, with a corresponding repository handling asynchronous CRUD operations for customer entities.

## Examples

Defining a `CustomerRepository` with `suspend` functions:

```kotlin
// CustomerRepository.kt
interface CustomerRepository {
    suspend fun findAll(): List<Customer>
    suspend fun findById(id: Int): Customer?
    suspend fun save(customer: Customer): Customer
    suspend fun deleteById(id: Int)
}

// CustomerExposedRepository.kt
@Repository
class CustomerExposedRepository: CustomerRepository {

    override suspend fun findAll(): List<Customer> = newSuspendedTransaction(Dispatchers.IO) {
        Customers.selectAll().map { toCustomer(it) }
    }

    override suspend fun findById(id: Int): Customer? = newSuspendedTransaction(Dispatchers.IO) {
        Customers.select { Customers.id eq id }.singleOrNull()?.let { toCustomer(it) }
    }

    override suspend fun save(customer: Customer): Customer = newSuspendedTransaction(Dispatchers.IO) {
        if (customer.id == null) {
            val id = Customers.insertAndGetId { it[name] = customer.name; it[email] = customer.email }.value
            customer.copy(id = id)
        } else {
            Customers.update({ Customers.id eq customer.id }) {
                it[name] = customer.name
                it[email] = customer.email
            }
            customer
        }
    }

    override suspend fun deleteById(id: Int) = newSuspendedTransaction(Dispatchers.IO) {
        Customers.deleteWhere { Customers.id eq id }
    }

    private fun toCustomer(row: ResultRow): Customer =
        Customer(row[Customers.id].value, row[Customers.name], row[Customers.email])
}

// CustomerService.kt
@Service
class CustomerService(private val customerRepository: CustomerRepository) {
    suspend fun getAllCustomers(): List<Customer> = customerRepository.findAll()
    suspend fun getCustomer(id: Int): Customer? = customerRepository.findById(id)
    suspend fun createCustomer(name: String, email: String): Customer =
        customerRepository.save(Customer(null, name, email))
    suspend fun updateCustomer(id: Int, name: String, email: String): Customer =
        customerRepository.save(Customer(id, name, email))
    suspend fun deleteCustomer(id: Int) = customerRepository.deleteById(id)
}

// Customer.kt (data class)
data class Customer(val id: Int?, val name: String, val email: String)

// Customers.kt (Exposed Table)
object Customers: IntIdTable("app_customers") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
}
```

*(Note: `newSuspendedTransaction` is a hypothetical function. Actual implementation may vary based on Exposed and Spring
versions, often involving `transaction { ... }` combined with `withContext(Dispatchers.IO)` or a
custom `SuspendedTransactionManager`.)*

## Further Reading

- [ExposedRepostiroy with Coroutines](https://debop.notion.site/ExposedRepository-with-Coroutines-1c32744526b080a1a6cbe2c86c2cb889)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
- [Exposed Wiki: Coroutines (if available)](https://github.com/JetBrains/Exposed/wiki/Coroutines)
