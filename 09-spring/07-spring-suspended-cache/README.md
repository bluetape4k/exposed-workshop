# 07-spring-suspended-cache

이 모듈은 Exposed를 사용하는 Spring Boot 애플리케이션 내에서 Kotlin Coroutines와 함께 Spring의 캐싱 추상화를 통합하여 비동기, 논블로킹 캐시 연산을 탐색합니다.
`suspend` 함수의 결과를 효과적으로 캐싱하여 캐싱 메커니즘이 코루틴이 제공하는 반응형 프로그래밍 패러다임과 호환되도록 하는 방법을 보여줍니다.

## 목적

이 모듈의 주요 목표는 다음을 보여주는 것입니다:

- `suspend` 함수에 Spring 캐싱 어노테이션(`@Cacheable`, `@CachePut`, `@CacheEvict`)을 적용하는 방법
- Spring의 캐싱 인프라가 코루틴 컨텍스트에서 올바르게 작동하도록 하는 필요한 설정
- 코루틴 기반 애플리케이션에서 블로킹 호출을 피하기 위한 비동기 캐시 상호작용 처리 전략
- 중단 가능한 캐싱 계층을 도입하여 코루틴 기반 Exposed 서비스의 성능 향상

## 실행 방법

1. JDK 17 이상이 설치되어 있는지 확인합니다.
2. Gradle을 사용하여 프로젝트를 빌드합니다: `./gradlew clean build`
3. Spring Boot 애플리케이션을 실행합니다: `./gradlew bootRun`
4. 중단 가능한 캐시된 메서드를 사용하는 애플리케이션(예: WebFlux 엔드포인트)과 상호작용합니다. 캐시 히트/미스와 연산의 논블로킹 특성에 대한 콘솔 출력을 관찰합니다.

## 주요 기능

- **코루틴 호환 캐싱**: Kotlin `suspend` 함수와 함께 Spring의 캐싱 추상화 사용
- **논블로킹 캐시 연산**: 캐시 상호작용(읽기/쓰기/제거)이 코루틴 원칙에 맞춰 비동기로 수행됨
- **향상된 응답성**: 캐싱을 활용하여 코루틴 기반 서비스의 응답성을 더욱 향상
- **반응형 API를 위한 선언적 캐싱**: Spring WebFlux와 같은 반응형 프레임워크를 통해 노출된 서비스에 캐싱을 원활하게 적용

## 설정

캐싱 활성화와 캐시 관리자 구성은 동기식 캐싱과 유사하지만, Exposed 및 코루틴과 결합할 때
`PlatformTransactionManager`와 실행 컨텍스트에 특별한 주의가 필요할 수 있습니다. 종종 완전한 논블로킹 캐싱 메커니즘 동작을 위해 반응형 캐시 관리자나 사용자 정의
`CacheAspect`가 필요할 수 있지만, Spring의 기본 `@Cacheable`은 종종 호출을 `Mono`나 `Flow`로 래핑하여 `suspend` 함수와 작동합니다.

`application.properties` 예시:

```properties
spring.datasource.url=jdbc:h2:mem:suspended_cache_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none

# 선택 사항: 특정 캐시 제공자 구성
```

## 데이터베이스 스키마

일반적인 예제는 비동기 서비스에서 자주 접근하는 제품 세부 정보를 위한 `Products` 테이블을 포함합니다.

## 예제

Exposed 기반 리포지토리와 상호작용하는 `suspend` 서비스 메서드에 `@Cacheable` 사용:

```kotlin
// ProductService.kt
@Service
class ProductService(private val productRepository: ProductRepository) { // ProductRepository가 suspend 함수로 Exposed를 사용한다고 가정

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

// ProductRepository.kt (suspend 함수가 있는 Exposed 기반 리포지토리)
@Repository
class ProductRepository {

    init {
      runBlocking(Dispatchers.IO) { // 차단될 수 있는 초기 설정에 runBlocking 사용
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

// Product.kt (데이터 클래스)
data class Product(val id: Int?, val name: String, val price: Double)

// Products.kt (Exposed Table)
object Products: IntIdTable("products") {
    val name = varchar("name", 255)
    val price = double("price")
}

// 예제 컨트롤러 (WebFlux를 사용하는 경우)
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

## 더 읽어보기

- [Exposed with Spring Suspended Cache](https://debop.notion.site/Exposed-with-Suspended-Spring-Cache-1db2744526b080769d2ef307e4a3c6c9)
- [Spring Caching Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
