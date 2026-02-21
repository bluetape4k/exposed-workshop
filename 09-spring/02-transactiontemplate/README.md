# 09 Spring: TransactionTemplate 활용

이 모듈은 Spring의 `TransactionTemplate`을 사용하여 Exposed로 프로그래밍 방식으로 트랜잭션을 관리하는 방법을 단계별로 학습합니다. 선언적 트랜잭션(
`@Transactional`)보다 더 세밀한 제어가 필요할 때 유용합니다.

## 학습 목표

- `TransactionTemplate`을 사용한 트랜잭션 관리 방법 이해
- 프로그래밍 방식 트랜잭션의 장단점 파악
- 트랜잭션 내 예외 처리 및 롤백 제어 학습
- 복잡한 트랜잭션 시나리오 구현 기법 습득

## 주요 기능

| 기능               | 설명                          |
|------------------|-----------------------------|
| 프로그래밍 방식 트랜잭션 제어 | `execute()`로 명시적 트랜잭션 범위 지정 |
| 예외 처리            | 예외 발생 시 자동 롤백               |
| 유연성              | 코드에서 정확한 트랜잭션 경계 정의         |
| 반환 값 처리          | 트랜잭션 결과를 명시적으로 반환           |

## 설정

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:transactiondb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

### TransactionTemplate 설정

```kotlin
@Configuration
class TransactionConfig {
    
    @Bean
    fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
        return TransactionTemplate(transactionManager).apply {
            // 격리 수준 설정
            isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
            
            // 전파 속성 설정
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
            
            // 타임아웃 설정 (초)
            timeout = 30
        }
    }
}
```

## 코드 예제

### 기본 CRUD

```kotlin
@Repository
class ProductRepository(
    private val transactionTemplate: TransactionTemplate
) {
    
    init {
        transactionTemplate.execute {
            SchemaUtils.create(Products)
        }
    }
    
    fun findById(id: Int): ProductDto? = transactionTemplate.execute {
        Products.select { Products.id eq id }
            .singleOrNull()
            ?.toProductDto()
    }
    
    fun findAll(): List<ProductDto> = transactionTemplate.execute {
        Products.selectAll().map { it.toProductDto() }
    } ?: emptyList()
    
    fun save(product: ProductDto): ProductDto = transactionTemplate.execute {
        val id = Products.insertAndGetId {
            it[name] = product.name
            it[price] = product.price
        }
        product.copy(id = id.value)
    }!!
    
    fun update(product: ProductDto): ProductDto? = transactionTemplate.execute {
        Products.update({ Products.id eq product.id!! }) {
            it[name] = product.name
            it[price] = product.price
        }
        product
    }
    
    fun delete(id: Int) = transactionTemplate.execute {
        Products.deleteWhere { Products.id eq id }
    }
}
```

### 이체 로직 (복합 연산)

```kotlin
@Service
class AccountService(
    private val transactionTemplate: TransactionTemplate
) {
    
    fun transferFunds(
        fromAccountId: Int,
        toAccountId: Int,
        amount: Double
    ) {
        transactionTemplate.execute {
            // 송금인 계좌에서 차감
            val fromAccount = Accounts.select { Accounts.id eq fromAccountId }.single()
            val fromBalance = fromAccount[Accounts.balance]
            
            if (fromBalance < amount) {
                throw InsufficientFundsException("잔액 부족")
            }
            
            Accounts.update({ Accounts.id eq fromAccountId }) {
                with(SqlExpressionBuilder) {
                    it[balance] = balance - amount.toBigDecimal()
                }
            }
            
            // 수취인 계좌에 입금
            Accounts.update({ Accounts.id eq toAccountId }) {
                with(SqlExpressionBuilder) {
                    it[balance] = balance + amount.toBigDecimal()
                }
            }
            
            // 이체 기록
            Transfers.insert {
                it[fromAccount] = fromAccountId
                it[toAccount] = toAccountId
                it[Transfers.amount] = amount.toBigDecimal()
                it[transferredAt] = LocalDateTime.now()
            }
        }
    }
}
```

### 롤백 제어

```kotlin
@Service
class OrderService(
    private val transactionTemplate: TransactionTemplate
) {
    
    fun createOrder(orderRequest: OrderRequest): OrderResult {
        return transactionTemplate.execute { status ->
            try {
                // 주문 생성
                val orderId = Orders.insertAndGetId {
                    it[customerId] = orderRequest.customerId
                    it[status] = OrderStatus.PENDING
                    it[createdAt] = LocalDateTime.now()
                }
                
                // 주문 항목 추가
                orderRequest.items.forEach { item ->
                    val product = Products.select { Products.id eq item.productId }.single()
                    
                    if (product[Products.stock] < item.quantity) {
                        // 수동 롤백 트리거
                        status.setRollbackOnly()
                        return@execute OrderResult.Failed("재고 부족: ${product[Products.name]}")
                    }
                    
                    OrderItems.insert {
                        it[order] = orderId
                        it[product] = item.productId
                        it[quantity] = item.quantity
                        it[price] = product[Products.price]
                    }
                    
                    // 재고 차감
                    Products.update({ Products.id eq item.productId }) {
                        with(SqlExpressionBuilder) {
                            it[stock] = stock - item.quantity
                        }
                    }
                }
                
                OrderResult.Success(orderId.value)
            } catch (e: Exception) {
                status.setRollbackOnly()
                OrderResult.Failed(e.message ?: "주문 실패")
            }
        }!!
    }
}
```

### 트랜잭션 없이 읽기

```kotlin
@Service
class ReadOnlyService(
    private val transactionTemplate: TransactionTemplate
) {
    
    // 읽기 전용 트랜잭션
    fun getProductStats(productId: Int): ProductStats {
        val readOnlyTx = TransactionTemplate(transactionManager).apply {
            isReadOnly = true
        }
        
        return readOnlyTx.execute {
            val orders = OrderItems.selectAll()
                .where { OrderItems.product eq productId }
                .toList()
            
            ProductStats(
                totalOrders = orders.size,
                totalQuantity = orders.sumOf { it[OrderItems.quantity] },
                totalRevenue = orders.sumOf { it[OrderItems.price].toDouble() }
            )
        }!!
    }
}
```

## @Transactional vs TransactionTemplate

| 구분     | @Transactional  | TransactionTemplate |
|--------|-----------------|---------------------|
| 스타일    | 선언적             | 프로그래밍 방식            |
| 장점     | 간결함, AOP 기반     | 세밀한 제어, 명시적 범위      |
| 단점     | 프록시 제약, 디버깅 어려움 | 상용구 코드 증가           |
| 적합한 경우 | 일반적인 CRUD       | 복잡한 트랜잭션 로직, 조건부 롤백 |

## 실행

```bash
# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew :09-spring:02-transactiontemplate:test
```

## 더 읽어보기

- [Exposed with Spring TransactionTemplate](https://debop.notion.site/JdbcTemplate-1c32744526b080959c0fcf671247e082)
- [Spring Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
