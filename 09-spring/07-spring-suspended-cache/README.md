# 07-spring-suspended-cache

This module explores the integration of Spring's caching abstraction with Kotlin Coroutines for asynchronous, non-blocking cache operations within a Spring Boot application using Exposed. It demonstrates how to effectively cache results from
`suspend` functions, ensuring that caching mechanisms are compatible with the reactive programming paradigm offered by coroutines.

## Purpose

The primary goal of this module is to illustrate:

- How to apply Spring caching annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`) to `suspend` functions.
- The necessary setup to ensure Spring's caching infrastructure works correctly with coroutine contexts.
- Strategies for handling asynchronous cache interactions to avoid blocking calls in a coroutine-driven application.
- Enhancing performance of coroutine-based Exposed services by introducing a suspended caching layer.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. Interact with the application (e.g., via a WebFlux endpoint) that uses the suspended cached methods. Observe the console output for cache hits/misses and the non-blocking nature of the operations.

## Key Features

- **Coroutine-compatible Caching:** Use Spring's caching abstraction with Kotlin `suspend` functions.
- **Non-blocking Cache Operations:
  ** Cache interactions (read/write/evict) are performed asynchronously, aligning with coroutine principles.
- **Improved Responsiveness:** Leverage caching to further enhance the responsiveness of coroutine-based services.
- **Declarative Caching for Reactive APIs:
  ** Seamlessly apply caching to services exposed via reactive frameworks like Spring WebFlux.

## Configuration

Enabling caching and configuring a cache manager is similar to synchronous caching, but special attention might be needed for the
`PlatformTransactionManager` and the execution context when combining with Exposed and coroutines. Often, a reactive cache manager or a custom
`CacheAspect` might be necessary for full non-blocking behavior of the caching mechanism itself, although Spring's default
`@Cacheable` often works with `suspend` functions by wrapping the call in a `Mono` or `Flow`.

Example `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:suspended_cache_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# Optional: Configure a specific cache provider
```

## Database Schema

A typical example would involve a
`Products` table, where product details are frequently accessed by asynchronous services.

## Examples

Using `@Cacheable` on a `suspend` service method interacting with an Exposed-based repository:

```kotlin
// ProductService.kt
@Service
class ProductService(private val productRepository: ProductRepository) { // Assume ProductRepository uses Exposed with suspend functions

    @Cacheable("products", key = "#id")
    suspend fun getProductById(id: Int): Product? {
        println("Fetching product $id from database (suspended call)...")
        return productRepository.findById(id)
    }

    @CachePut("products", key = "#product.id")
    suspend fun updateProduct(product: Product): Product {
        println("Updating product ${product.id} in database and cache (suspended call)...")
        return productRepository.save(product)
    }

    @CacheEvict("products", key = "#id")
    suspend fun deleteProduct(id: Int) {
        println("Deleting product $id from database and evicting from cache (suspended call)...")
        productRepository.deleteById(id)
    }

    @CacheEvict(value = "products", allEntries = true)
    suspend fun clearAllProductsCache() {
        println("Evicting all products from cache (suspended call)...")
    }
}

// ProductRepository.kt (Exposed-based repository with suspend functions)
@Repository
class ProductRepository {

    init {
        runBlocking(Dispatchers.IO) { // Use runBlocking for initial setup that might block
            newSuspendedTransaction {
                SchemaUtils.create(Products)
                Products.insert { it[name] = "Laptop"; it[price] = 1200.0 }
                Products.insert { it[name] = "Mouse"; it[price] = 25.0 }
            }
        }
    }

    suspend fun findById(id: Int): Product? = newSuspendedTransaction(Dispatchers.IO) {
        Products.select { Products.id eq id }.singleOrNull()?.let { toProduct(it) }
    }

    suspend fun save(product: Product): Product = newSuspendedTransaction(Dispatchers.IO) {
        if (product.id == null) {
            val id = Products.insertAndGetId { it[name] = product.name; it[price] = product.price }.value
            product.copy(id = id)
        } else {
            Products.update({ Products.id eq product.id }) {
                it[name] = product.name
                it[price] = product.price
            }
            product
        }
    }

    suspend fun deleteById(id: Int) = newSuspendedTransaction(Dispatchers.IO) {
        Products.deleteWhere { Products.id eq id }
    }

    private fun toProduct(row: ResultRow): Product =
        Product(row[Products.id].value, row[Products.name], row[Products.price])
}

// Product.kt (data class)
data class Product(val id: Int?, val name: String, val price: Double)

// Products.kt (Exposed Table)
object Products: IntIdTable("products") {
    val name = varchar("name", 255)
    val price = double("price")
}

// Example Controller (if using WebFlux)
@RestController
@RequestMapping("/products")
class ProductController(private val productService: ProductService) {

    @GetMapping("/{id}")
    suspend fun getProduct(@PathVariable id: Int): Product? {
        return productService.getProductById(id)
    }

    @PostMapping
    suspend fun createProduct(@RequestBody product: Product): Product {
        return productService.updateProduct(product)
    }
}
```

*(Note: `newSuspendedTransaction` is a placeholder for a coroutine-aware Exposed transaction block. The exact
implementation depends on the Exposed version and how it integrates with coroutines and
Spring's `PlatformTransactionManager`.)*

## Further Reading

- [Exppose with Spring Suspended Cache](https://debop.notion.site/Exposed-with-Suspended-Spring-Cache-1db2744526b080769d2ef307e4a3c6c9)
- [Spring Caching Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
