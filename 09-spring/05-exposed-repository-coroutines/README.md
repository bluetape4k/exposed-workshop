# 05-exposed-repository-coroutines

이 모듈은 Spring Boot 애플리케이션에서 Exposed와 Kotlin Coroutines를 통합하는 방법을 탐색하며, 특히 논블로킹 비동기 리포지토리 구축에 초점을 맞춥니다. 코루틴을 활용하여 데이터베이스 연산을 수행함으로써 특히 높은 동시성 환경에서 애플리케이션 응답성과 리소스 활용도를 향상시키는 방법을 보여줍니다.

## 목적

이 모듈의 주요 목표는 다음을 보여주는 것입니다:

- Kotlin Coroutines를 사용하여 Exposed 데이터베이스 연산을 비동기로 수행하는 방법
- 논블로킹 I/O를 달성하기 위해 리포지토리 메서드 내에 `suspend` 함수 통합
- Exposed를 위한 코루틴 친화적 트랜잭션 관리자(예: `asCoroutineContextElement()`가 있는 `SpringTransactionManager`) 구성
- Spring WebFlux(해당하는 경우) 또는 다른 코루틴 기반 프레임워크로 반응형 스타일 데이터 접근 계층 구축

## 실행 방법

1. JDK 17 이상이 설치되어 있는지 확인합니다.
2. Gradle을 사용하여 프로젝트를 빌드합니다: `./gradlew clean build`
3. Spring Boot 애플리케이션을 실행합니다: `./gradlew bootRun`
4. 애플리케이션은 일반적으로 코루틴 기반 리포지토리를 활용하는 논블로킹 API 엔드포인트(예: Spring WebFlux 사용)를 노출합니다.

## 주요 기능

- **논블로킹 데이터베이스 접근**: 호출 스레드를 차단하지 않고 Exposed 쿼리를 실행하여 확장성 향상
- **구조화된 동시성**: 안전하고 효율적인 동시 데이터베이스 연산을 위해 Kotlin Coroutines 사용
- **Spring 통합**: 코루틴 기반 Exposed 리포지토리를 Spring 애플리케이션 컨텍스트에 원활하게 통합
- **향상된 응답성**: 마이크로서비스와 반응형 시스템과 같이 높은 처리량과 낮은 지연 시간이 필요한 애플리케이션에 이상적

## 설정

이 모듈은 `transaction` 함수가 코루틴 인식이 되도록 설정해야 합니다. 이는 종종 `CoroutineDispatcher`를 제공하고 트랜잭션 컨텍스트가 올바르게 전파되도록 보장하는 것을 포함합니다.

`application.properties` 예시:

```properties
spring.datasource.url=jdbc:h2:mem:coroutines_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=none
```

코루틴용 사용자 정의 `transaction` 블록:

```kotlin
// DatabaseConfig.kt
@Configuration
class DatabaseConfig(private val dataSource: DataSource, private val transactionManager: PlatformTransactionManager) {
    @Bean
    fun initExposed() = runBlocking { // 또는 이것이 차단되지 않도록 다른 스코프 사용
        Database.connect(dataSource)
      // 코루틴용 트랜잭션 관리자 설정
        transactionManager.apply {
            TransactionManager.default = SpringTransactionManager(dataSource)
        }
        transaction {
            SchemaUtils.create(Customers)
        }
    }
}

// 사용자 정의 코루틴 인식 트랜잭션 유틸리티 (종종 Exposed 확장이나 사용자 정의로 제공)
// `SuspendedTransactionManager`를 사용하거나 표준 `transaction`을 
// `withContext(Dispatchers.IO)`와 사용자 정의 컨텍스트 요소로 래핑하는 
// 사용자 정의 `transaction` 래퍼가 필요할 수 있습니다.
```

## 데이터베이스 스키마

일반적인 예제는 고객 엔티티에 대한 비동기 CRUD 연산을 처리하는 해당 리포지토리와 함께 `Customers` 테이블을 포함합니다.

## 예제

`suspend` 함수가 있는 `CustomerRepository` 정의:

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

// Customer.kt (데이터 클래스)
data class Customer(val id: Int?, val name: String, val email: String)

// Customers.kt (Exposed Table)
object Customers: IntIdTable("app_customers") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
}
```

## 더 읽어보기

- [ExposedRepostiroy with Coroutines](https://debop.notion.site/ExposedRepository-with-Coroutines-1c32744526b080a1a6cbe2c86c2cb889)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
- [Exposed Wiki: Coroutines (사용 가능한 경우)](https://github.com/JetBrains/Exposed/wiki/Coroutines)
