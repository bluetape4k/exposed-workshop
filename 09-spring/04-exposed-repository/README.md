# 04-exposed-repository

This module demonstrates the implementation of a repository pattern using Exposed within a Spring Boot application. It illustrates how to create dedicated classes (repositories) for data access operations, providing a clear separation of concerns and encapsulating database interactions.

## Purpose

The primary goal of this module is to illustrate:

- How to design and implement a repository interface and its Exposed-based implementation.
- Encapsulation of Exposed table and query logic within repository methods.
- Integration of these repositories with Spring services for business logic.
- Promoting clean architecture and testability by abstracting data access.

## How to Run

1. Ensure you have Java Development Kit (JDK) 17 or higher installed.
2. Build the project using Gradle: `./gradlew clean build`
3. Run the Spring Boot application: `./gradlew bootRun`
4. The application will typically expose an API or run a command-line application that uses the defined repositories.

## Key Features

- **Separation of Concerns:** Business logic in services, data access logic in repositories.
- **Abstraction over Exposed:** Repositories hide the direct usage of Exposed `Table` objects and
  `transaction` blocks from service layers.
- **Testability:** Repositories can be easily mocked or stubbed for unit testing service layers.
- **Maintainability:** Changes to database schema or Exposed usage are localized within the repository.

## Configuration

Standard Spring Boot data source and auto-configuration for Exposed. No special configuration specific to the repository pattern itself, other than defining the component scans if necessary.

Example `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:repository_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
```

## Database Schema

A common example involves a `Users` or
`Posts` table, with a corresponding repository handling CRUD operations for that entity.

## Examples

Defining a `UserRepository` interface and its Exposed implementation:

```kotlin
// UserRepository.kt
interface UserRepository {
    fun findAll(): List<User>
    fun findById(id: Int): User?
    fun save(user: User): User
    fun deleteById(id: Int)
}

// UserExposedRepository.kt
@Repository
class UserExposedRepository: UserRepository {

    init {
        transaction {
            SchemaUtils.create(Users)
            // Initial data
            Users.insert { it[name] = "Alice" }
            Users.insert { it[name] = "Bob" }
        }
    }

    override fun findAll(): List<User> = transaction {
        Users.selectAll().map { toUser(it) }
    }

    override fun findById(id: Int): User? = transaction {
        Users.select { Users.id eq id }.singleOrNull()?.let { toUser(it) }
    }

    override fun save(user: User): User = transaction {
        if (user.id == null) {
            val id = Users.insertAndGetId { it[name] = user.name }.value
            user.copy(id = id)
        } else {
            Users.update({ Users.id eq user.id }) { it[name] = user.name }
            user
        }
    }

    override fun deleteById(id: Int) = transaction {
        Users.deleteWhere { Users.id eq id }
    }

    private fun toUser(row: ResultRow): User = User(row[Users.id].value, row[Users.name])
}

// UserService.kt
@Service
class UserService(private val userRepository: UserRepository) {
    fun getAllUsers(): List<User> = userRepository.findAll()
    fun getUser(id: Int): User? = userRepository.findById(id)
    fun createUser(name: String): User = userRepository.save(User(null, name))
    fun updateUser(id: Int, name: String): User = userRepository.save(User(id, name))
    fun deleteUser(id: Int) = userRepository.deleteById(id)
}

// User.kt (data class representing the domain entity)
data class User(val id: Int?, val name: String)

// Users.kt (Exposed Table object)
object Users: IntIdTable("app_users") {
    val name = varchar("name", 255)
}
```

## Further Reading

- [ExposedRepostiroy with Spring Web](https://debop.notion.site/ExposedRepository-1c32744526b080208e5ee03b900d2c5e)
- [Exposed Wiki: DAO](https://github.com/JetBrains/Exposed/wiki/DAO)
- [Spring Data JPA (Conceptual inspiration for repository pattern)](https://spring.io/projects/spring-data-jpa)
- [Hexagonal Architecture (Ports and Adapters)](https://www.baeldung.com/hexagonal-architecture-ports-adapters)
