# 06-spring-cache

이 모듈은 Exposed 데이터베이스 연산과 Spring의 캐싱 추상화 통합을 보여줍니다. `@Cacheable`, `@CachePut`,
`@CacheEvict` 어노테이션을 활용하여 Exposed 쿼리 결과를 캐싱함으로써 애플리케이션 성능을 향상시키고 데이터베이스 부하를 줄이는 방법을 보여줍니다.

## 목적

이 모듈의 주요 목표는 다음을 보여주는 것입니다:

- Spring Boot 애플리케이션에서 Spring의 캐싱 메커니즘을 활성화하고 구성하는 방법
- Exposed 리포지토리와 상호작용하거나 직접 Exposed 쿼리를 수행하는 서비스 메서드에 캐싱 어노테이션 적용
- 중복 데이터베이스 호출을 줄여 캐싱의 성능 이점 관찰

## 실행 방법

1. JDK 17 이상이 설치되어 있는지 확인합니다.
2. Gradle을 사용하여 프로젝트를 빌드합니다: `./gradlew clean build`
3. Spring Boot 애플리케이션을 실행합니다: `./gradlew bootRun`
4. 애플리케이션(예: REST 엔드포인트 또는 테스트)과 상호작용하고 캐싱 동작(예: 캐시 히트/미스를 나타내는 로그)을 관찰합니다.

## 주요 기능

- **선언적 캐싱**: 어노테이션을 사용하여 쉽게 메서드 결과 캐싱
- **성능 향상**: 자주 요청되는 데이터에 대한 데이터베이스 접근 시간 단축
- **데이터베이스 부하 감소**: 데이터베이스 서버에 대한 부하 감소
- **캐시 무효화**: 기본 데이터가 변경될 때 캐시된 데이터를 업데이트하거나 제거하는 메커니즘

## 설정

캐싱을 활성화하려면 일반적으로 Spring 설정 클래스에 `@EnableCaching`을 추가하고 캐시 관리자를 구성해야 합니다. 다른 캐시 관리자가 지정되지 않은 경우 Spring Boot는 종종 기본적으로 간단한
`ConcurrentHashMapCacheManager`를 자동 구성합니다.

`application.properties` 예시:

```properties
spring.datasource.url=jdbc:h2:mem:cached_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# 선택 사항: 특정 캐시 제공자 구성, 예: Caffeine
# spring.cache.type=caffeine
```

설정 클래스 예시:

```kotlin
@Configuration
@EnableCaching
class CachingConfig {
    // 필요한 경우 사용자 정의 캐시 관리자를 정의할 수 있습니다.
    // 그렇지 않으면 Spring Boot가 기본값(예: ConcurrentHashMapCacheManager)을 자동 구성합니다.
}
```

## 데이터베이스 스키마

일반적인 예제는 책 세부 정보가 자주 접근되는 `Books` 테이블을 포함합니다.

## 예제

Exposed와 상호작용하는 서비스에 `@Cacheable`, `@CachePut`, `@CacheEvict` 사용:

```kotlin
// BookService.kt
@Service
class BookService(private val bookRepository: BookRepository) { // BookRepository가 Exposed를 사용한다고 가정

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

// BookRepository.kt (Exposed 기반 리포지토리)
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

// Book.kt (데이터 클래스)
data class Book(val id: Int?, val title: String, val author: String)

// Books.kt (Exposed Table)
object Books: IntIdTable("books") {
    val title = varchar("title", 255)
    val author = varchar("author", 255)
}
```

## 더 읽어보기

- [Exposed with Spring Boot Cache](https://debop.notion.site/Exposed-with-Spring-Boot-Cache-1d82744526b08062bfcce52d6aab3ef7)
- [Spring Caching Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Baeldung: Spring Cache](https://www.baeldung.com/spring-cache-tutorial)
