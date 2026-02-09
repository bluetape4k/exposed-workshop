# 06-spring-cache

This module demonstrates the integration of Spring's caching abstraction with Exposed database operations. It showcases how to leverage
`@Cacheable`, `@CachePut`, and
`@CacheEvict` annotations to cache the results of Exposed queries, thereby improving application performance and reducing database load.

## Purpose

The primary goal of this module is to illustrate:

- How to enable and configure Spring's caching mechanism in a Spring Boot application.
- Applying caching annotations to service methods that interact with Exposed repositories or directly perform Exposed queries.
- Observing the performance benefits of caching by reducing redundant database calls.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. Interact with the application (e.g., via a REST endpoint or a test) and observe the caching behavior (e.g., logs indicating cache hits/misses).

## Key Features

- **Declarative Caching:** Easily cache method results using annotations.
- **Improved Performance:** Reduce database access times for frequently requested data.
- **Reduced Database Load:** Less pressure on the database server.
- **Cache Invalidation:** Mechanisms to update or remove cached data when underlying data changes.

## Configuration

To enable caching, you typically need to add
`@EnableCaching` to a Spring configuration class and configure a cache manager. Spring Boot often auto-configures a simple
`ConcurrentHashMapCacheManager` by default if no other cache manager is specified.

Example `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:cached_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# Optional: Configure a specific cache provider, e.g., Caffeine
# spring.cache.type=caffeine
```

Example Configuration Class:

```kotlin
@Configuration
@EnableCaching
class CachingConfig {
    // You can define custom cache managers here if needed,
    // otherwise, Spring Boot will auto-configure a default one (e.g., ConcurrentHashMapCacheManager).
}
```

## Database Schema

A typical example involves a `Books` table where book details are frequently accessed.

## Examples

Using `@Cacheable`, `@CachePut`, and `@CacheEvict` on a service interacting with Exposed:

```kotlin
// BookService.kt
@Service
class BookService(private val bookRepository: BookRepository) { // Assume BookRepository uses Exposed

    @Cacheable("books", key = "#id")
    fun getBookById(id: Int): Book? {
        println("Fetching book $id from database...")
        return bookRepository.findById(id)
    }

    @CachePut("books", key = "#book.id")
    fun updateBook(book: Book): Book {
        println("Updating book ${book.id} in database and cache...")
        return bookRepository.save(book)
    }

    @CacheEvict("books", key = "#id")
    fun deleteBook(id: Int) {
        println("Deleting book $id from database and evicting from cache...")
        bookRepository.deleteById(id)
    }

    @CacheEvict(value = "books", allEntries = true)
    fun clearAllBooksCache() {
        println("Evicting all books from cache...")
    }
}

// BookRepository.kt (Exposed-based repository)
@Repository
class BookRepository {

    init {
        transaction {
            SchemaUtils.create(Books)
            Books.insert { it[title] = "The Hitchhiker's Guide to the Galaxy"; it[author] = "Douglas Adams" }
            Books.insert { it[title] = "1984"; it[author] = "George Orwell" }
        }
    }

    fun findById(id: Int): Book? = transaction {
        Books.select { Books.id eq id }.singleOrNull()?.let { toBook(it) }
    }

    fun save(book: Book): Book = transaction {
        if (book.id == null) {
            val id = Books.insertAndGetId { it[title] = book.title; it[author] = book.author }.value
            book.copy(id = id)
        } else {
            Books.update({ Books.id eq book.id }) {
                it[title] = book.title
                it[author] = book.author
            }
            book
        }
    }

    fun deleteById(id: Int) = transaction {
        Books.deleteWhere { Books.id eq id }
    }

    private fun toBook(row: ResultRow): Book = Book(row[Books.id].value, row[Books.title], row[Books.author])
}

// Book.kt (data class)
data class Book(val id: Int?, val title: String, val author: String)

// Books.kt (Exposed Table)
object Books: IntIdTable("books") {
    val title = varchar("title", 255)
    val author = varchar("author", 255)
}
```

## Further Reading

- [Exppose with Spring Boot Cache](https://debop.notion.site/Exposed-with-Spring-Boot-Cache-1d82744526b08062bfcce52d6aab3ef7)
- [Spring Caching Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Baeldung: Spring Cache](https://www.baeldung.com/spring-cache-tutorial)
