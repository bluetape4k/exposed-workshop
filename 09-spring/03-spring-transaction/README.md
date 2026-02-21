# 09 Spring: Spring Transaction 통합

이 모듈은 Spring의
`@Transactional` 어노테이션을 사용하여 Exposed와 선언적 트랜잭션을 통합하는 방법을 단계별로 학습합니다. Spring이 자동으로 트랜잭션 경계를 관리하여 간결하고 선언적인 코드를 작성할 수 있습니다.

## 학습 목표

- `@Transactional` 어노테이션으로 트랜잭션 경계 정의 방법 이해
- 트랜잭션 속성(격리 수준, 전파 속성, 타임아웃) 설정 방법 학습
- 읽기 전용 트랜잭션 최적화 기법 습득
- 예외 기반 롤백 제어 방법 익히기

## 주요 기능

| 기능          | 설명                           |
|-------------|------------------------------|
| 선언적 트랜잭션 관리 | 어노테이션만으로 트랜잭션 정의             |
| 자동 커밋/롤백    | 메서드 성공 시 커밋, 예외 시 롤백         |
| 다양한 속성 설정   | 격리 수준, 전파 속성, 타임아웃, 읽기 전용 설정 |
| Spring 통합   | Spring 트랜잭션 관리자와 완벽한 통합      |

## 설정

### application.yml

```yaml
spring:
    datasource:
        url: jdbc:h2:mem:txdb;DB_CLOSE_DELAY=-1
        driver-class-name: org.h2.Driver
        username: sa
        password:
```

### 트랜잭션 관리자 설정

```kotlin
@Configuration
@EnableTransactionManagement
class TransactionConfig {

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }
}
```

## 코드 예제

### 기본 서비스

```kotlin
@Service
class OrderService {

    init {
        transaction {
            SchemaUtils.create(Orders, OrderItems)
        }
    }

    @Transactional
    fun createOrder(customerId: Long, items: List<OrderItemRequest>): Long {
        val orderId = Orders.insertAndGetId {
            it[Orders.customerId] = customerId
            it[status] = OrderStatus.PENDING
            it[createdAt] = LocalDateTime.now()
        }.value

        items.forEach { item ->
            OrderItems.insert {
                it[order] = orderId
                it[productId] = item.productId
                it[quantity] = item.quantity
                it[price] = item.price
            }
        }

        return orderId
    }
}
```

### 읽기 전용 트랜잭션

```kotlin
@Service
class ProductService {

    @Transactional(readOnly = true)
    fun findById(id: Long): ProductDto? {
        return Products.select { Products.id eq id }
            .singleOrNull()
            ?.toDto()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ProductDto> {
        return Products.selectAll()
            .orderBy(Products.name to SortOrder.ASC)
            .map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun search(query: String): List<ProductDto> {
        return Products.select { Products.name like "%$query%" }
            .map { it.toDto() }
    }
}
```

### 트랜잭션 속성

```kotlin
@Service
class PaymentService {

    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        timeout = 30,
        readOnly = false,
        rollbackFor = [Exception::class]
    )
    fun processPayment(orderId: Long, paymentInfo: PaymentInfo): PaymentResult {
        val order = Orders.select { Orders.id eq orderId }.single()

        // 결제 처리
        val paymentId = Payments.insertAndGetId {
            it[Payments.orderId] = orderId
            it[amount] = order[Orders.totalAmount]
            it[status] = PaymentStatus.PROCESSING
            it[processedAt] = LocalDateTime.now()
        }.value

        // 외부 결제 게이트웨이 호출 (롤백 불가능한 작업)
        val gatewayResult = paymentGateway.charge(paymentInfo)

        if (!gatewayResult.success) {
            throw PaymentFailedException(gatewayResult.errorMessage)
        }

        // 주문 상태 업데이트
        Orders.update({ Orders.id eq orderId }) {
            it[status] = OrderStatus.PAID
        }

        Payments.update({ Payments.id eq paymentId }) {
            it[status] = PaymentStatus.COMPLETED
            it[transactionId] = gatewayResult.transactionId
        }

        return PaymentResult.Success(paymentId, gatewayResult.transactionId)
    }
}
```

### 롤백 제어

```kotlin
@Service
class InventoryService {

    @Transactional
    fun reserveStock(orderId: Long, items: List<OrderItemRequest>) {
        items.forEach { item ->
            val product = Products.select { Products.id eq item.productId }.single()
            val currentStock = product[Products.stock]

            if (currentStock < item.quantity) {
                // 예외 발생 시 자동 롤백
                throw InsufficientStockException(
                    "재고 부족: ${product[Products.name]} (재고: $currentStock, 요청: ${item.quantity})"
                )
            }

            Products.update({ Products.id eq item.productId }) {
                with(SqlExpressionBuilder) {
                    it[stock] = stock - item.quantity
                    it[reservedStock] = reservedStock + item.quantity
                }
            }

            StockReservations.insert {
                it[StockReservations.orderId] = orderId
                it[productId] = item.productId
                it[quantity] = item.quantity
                it[reservedAt] = LocalDateTime.now()
            }
        }
    }

    // 특정 예외만 롤백
    @Transactional(rollbackFor = [CriticalException::class], noRollbackFor = [WarningException::class])
    fun processWithSelectiveRollback(data: ProcessData) {
        // ...
    }
}
```

### 중첩 트랜잭션

```kotlin
@Service
class OrderService(
    private val inventoryService: InventoryService,
    private val paymentService: PaymentService
) {

    @Transactional
    fun placeOrder(customerId: Long, items: List<OrderItemRequest>, paymentInfo: PaymentInfo) {
        // 주문 생성
        val orderId = createOrder(customerId, items)

        // 재고 예약 (별도 트랜잭션이지만 같은 트랜잭션에 참여)
        inventoryService.reserveStock(orderId, items)

        // 결제 처리
        paymentService.processPayment(orderId, paymentInfo)

        // 주문 완료 상태로 변경
        Orders.update({ Orders.id eq orderId }) {
            it[status] = OrderStatus.COMPLETED
        }
    }
}
```

## 트랜잭션 속성 요약

### 격리 수준

| 속성                 | 설명            |
|--------------------|---------------|
| `DEFAULT`          | DB 기본 격리 수준   |
| `READ_UNCOMMITTED` | 커밋되지 않은 읽기 허용 |
| `READ_COMMITTED`   | 커밋된 읽기만 허용    |
| `REPEATABLE_READ`  | 반복 가능한 읽기     |
| `SERIALIZABLE`     | 직렬화 가능        |

### 전파 속성

| 속성              | 설명                        |
|-----------------|---------------------------|
| `REQUIRED`      | 기존 트랜잭션 참여 또는 새로 생성 (기본값) |
| `REQUIRES_NEW`  | 항상 새 트랜잭션 생성              |
| `SUPPORTS`      | 트랜잭션 있으면 참여, 없으면 비트랜잭션 실행 |
| `NOT_SUPPORTED` | 트랜잭션 없이 실행                |
| `MANDATORY`     | 트랜잭션 필수, 없으면 예외           |
| `NEVER`         | 트랜잭션 없어야 함, 있으면 예외        |
| `NESTED`        | 중첩 트랜잭션 (세이브포인트)          |

## 실행

```bash
# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew :09-spring:03-spring-transaction:test
```

## 더 읽어보기

- [Exposed SpringTransactionManager](https://debop.notion.site/Exposed-SpringTransactionManager-1c32744526b080aa8668e727ff644c34)
- [Spring @Transactional](https://www.baeldung.com/spring-transactional-annotation)
