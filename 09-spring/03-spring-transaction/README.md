# 03-spring-transaction

This module demonstrates the declarative transaction management capabilities of Spring with Exposed, primarily using the
`@Transactional` annotation. It illustrates how Spring manages transaction boundaries automatically around service methods, simplifying transaction handling for developers.

## Purpose

The primary goal of this module is to illustrate:

- How to apply the `@Transactional` annotation to service methods to define transaction boundaries.
- The automatic commit and rollback behavior of Spring transactions when using Exposed.
- Integration of Spring's transaction manager with Exposed's database operations.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. Interact with the application (e.g., via a REST endpoint or a command-line runner) that triggers transactional service methods. Observe the console output for transaction lifecycle events.

## Key Features

- **Declarative Transaction Management:
  ** Define transaction boundaries using simple annotations, reducing boilerplate code.
- **Automatic Transaction Lifecycle:** Spring automatically handles `beginTransaction()`, `commit()`, and
  `rollback()` based on method execution and exceptions.
- **Integration with Spring Services:** Seamlessly use
  `@Transactional` on methods within Spring-managed service components that interact with Exposed.

## Configuration

Standard Spring Boot data source configuration is required. Ensure that a
`PlatformTransactionManager` is configured, which Spring Boot usually provides automatically for JDBC data sources.

Example `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:transactional_spring_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none # Important: Let Exposed handle DDL
```

## Database Schema

A typical example involves a simple `Orders` and
`OrderItems` schema, where an order creation and its items must be saved atomically.

## Examples

Using `@Transactional` on a service method:

```kotlin
@Service
class OrderService {

    init {
        transaction {
            SchemaUtils.create(Orders, OrderItems)
        }
    }

    @Transactional
    fun createOrderWithItems(customerName: String, productQuantities: Map<String, Int>): Long {
        val orderId = Orders.insertAndGetId {
            it[customer] = customerName
            it[orderDate] = CurrentDateTime
        }.value

        productQuantities.forEach { (productName, quantity) ->
            OrderItems.insert {
                it[order] = orderId
                it[product] = productName
                it[qty] = quantity
            }
        }

        // Simulate an error to demonstrate rollback
        if (productQuantities.isEmpty()) {
            throw IllegalArgumentException("Cannot create an order without items.")
        }

        return orderId
    }

    @Transactional(readOnly = true)
    fun getOrderDetails(orderId: Long): OrderDetailsDto? = transaction {
        (Orders innerJoin OrderItems)
            .select { Orders.id eq orderId }
            .groupBy(Orders.id, Orders.customer, Orders.orderDate)
            .singleOrNull()?.let { row ->
                val items = OrderItems.select { OrderItems.order eq orderId }.map { itemRow ->
                    OrderItemDto(itemRow[OrderItems.product], itemRow[OrderItems.qty])
                }
                OrderDetailsDto(
                    row[Orders.id].value,
                    row[Orders.customer],
                    row[Orders.orderDate].toJavaInstant(),
                    items
                )
            }
    }
}

object Orders: LongIdTable("orders") {
    val customer = varchar("customer", 255)
    val orderDate = datetime("order_date")
}

object OrderItems: IntIdTable("order_items") {
    val order = reference("order_id", Orders)
    val product = varchar("product_name", 255)
    val qty = integer("quantity")
}

data class OrderDetailsDto(
    val orderId: Long,
    val customer: String,
    val orderDate: Instant,
    val items: List<OrderItemDto>
)
data class OrderItemDto(val product: String, val quantity: Int)
```

## Further Reading

- [Exposed SpringTransactionManager](https://debop.notion.site/Exposed-SpringTransactionManager-1c32744526b080aa8668e727ff644c34)
- [Spring Framework Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Exposed Wiki: Transactions](https://github.com/JetBrains/Exposed/wiki/Transactions)
- [Baeldung: Spring Transactional](https://www.baeldung.com/spring-transactional-annotation)
