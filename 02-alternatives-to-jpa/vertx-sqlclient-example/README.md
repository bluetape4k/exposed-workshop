# 02 JPA 대안: Vert.x SQL Client Example

이 모듈(`vertx-sqlclient-example`)은 **Vert.x SQL Client
**를 사용하여 반응형 데이터베이스 작업을 수행하는 예제 모음입니다. 이벤트 기반 Non-blocking 데이터 액세스 레이어를 구축하는 방법을 학습합니다.

## Vert.x SQL Client란?

Vert.x SQL Client는 Eclipse Vert.x 프레임워크의 일부로, 고성능 비동기 데이터베이스 클라이언트를 제공합니다. JDBC 드라이버를 통한 블로킹 방식 대신, 네이티브 비동기 드라이버를 사용하여 높은 처리량을 달성합니다.

### 주요 특징

| 특징         | 설명                               |
|------------|----------------------------------|
| **고성능**    | 네이티브 비동기 드라이버                    |
| **저수준 제어** | SQL 직접 작성                        |
| **이벤트 루프** | Vert.x Event Loop 기반             |
| **다양한 DB** | PostgreSQL, MySQL, DB2, Oracle 등 |

## 다른 라이브러리와의 비교

```
┌────────────────────────────────────────────────────────────────┐
│                     추상화 수준                                  │
│                                                                 │
│  High    ┌──────────────┐  ┌──────────────┐                   │
│          │    JPA       │  │   Hibernate   │                   │
│          │  (Entity)    │  │   Reactive    │                   │
│          └──────────────┘  └──────────────┘                   │
│                                                                 │
│  Medium  ┌──────────────┐  ┌──────────────┐                   │
│          │  Spring Data │  │   Exposed    │                   │
│          │    R2DBC     │  │    (DAO)     │                   │
│          └──────────────┘  └──────────────┘                   │
│                                                                 │
│  Low     ┌──────────────┐  ┌──────────────┐                   │
│          │  Vert.x SQL  │  │   Exposed    │                   │
│          │   Client     │  │    (DSL)     │                   │
│          └──────────────┘  └──────────────┘                   │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

## 프로젝트 구조

```
src/test/kotlin/
├── AbstractSqlClientTest.kt              # 기본 테스트 클래스
├── JDBCPoolExamples.kt                   # JDBC Pool 예제
├── model/
│   └── Customer.kt                       # Customer 도메인 모델
└── templates/
    └── SqlClientTemplatePostgresExamples.kt  # PostgreSQL 예제
```

## 도메인 모델

### Customer

```kotlin
data class Customer(
    val id: Long? = null,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

## 주요 예제

### 1. JDBC Pool 사용

```kotlin
class JDBCPoolExamples: AbstractSqlClientTest() {

    @Test
    fun `JDBC Pool로 데이터 조회`(vertx: Vertx) {
        val pool = JDBCPool.pool(
            vertx,
            JDBCConnectOptions()
                .setJdbcUrl("jdbc:postgresql://localhost:5432/testdb")
                .setUser("user")
                .setPassword("password"),
            PoolOptions().setMaxSize(5)
        )

        pool.query("SELECT * FROM customers")
            .execute()
            .onSuccess { rows ->
                rows.forEach { row ->
                    println("Customer: ${row.getString("name")}")
                }
            }
            .onFailure { err ->
                println("Error: ${err.message}")
            }
    }
}
```

### 2. PostgreSQL Client 사용

```kotlin
class SqlClientTemplatePostgresExamples: AbstractSqlClientTest() {

    @Test
    fun `PostgreSQL Client로 CRUD 수행`(vertx: Vertx, testContext: VertxTestContext) {
        val pgPool = PgPool.pool(
            PgConnectOptions()
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("testdb")
                .setUser("user")
                .setPassword("password"),
            PoolOptions().setMaxSize(5)
        )

        // INSERT
        pgPool.preparedQuery("INSERT INTO customers (name, email) VALUES ($1, $2)")
            .execute(Tuple.of("John Doe", "john@example.com"))
            .compose { insertResult ->
                // SELECT
                pgPool.query("SELECT * FROM customers WHERE name = 'John Doe'")
                    .execute()
            }
            .onSuccess { rows ->
                testContext.verify {
                    rows.size() shouldBeEqualTo 1
                    rows.first().getString("name") shouldBeEqualTo "John Doe"
                }
                testContext.completeNow()
            }
            .onFailure { err ->
                testContext.failNow(err)
            }
    }
}
```

### 3. SqlClientTemplate 사용

```kotlin
class SqlClientTemplateExamples {

    fun <T> queryForObject(
        pool: PgPool,
        sql: String,
        params: Tuple,
        rowMapper: (Row) -> T
    ): Future<T?> {
        return pool.preparedQuery(sql)
            .execute(params)
            .map { rows -> rows.firstOrNull()?.let(rowMapper) }
    }

    fun queryForList(
        pool: PgPool,
        sql: String,
        rowMapper: (Row) -> Customer
    ): Future<List<Customer>> {
        return pool.query(sql)
            .execute()
            .map { rows -> rows.map(rowMapper).toList() }
    }
}
```

## Vert.x SQL Client 패턴

### 1. Connection Pool 구성

```kotlin
val pool = PgPool.pool(
    PgConnectOptions()
        .setHost("localhost")
        .setPort(5432)
        .setDatabase("testdb")
        .setUser("user")
        .setPassword("password")
        .setPipeliningLimit(256)        // 파이프라이닝 제한
        .setCachePreparedStatements(true), // PreparedStatement 캐시
    PoolOptions()
        .setMaxSize(20)                  // 최대 연결 수
        .setMaxWaitQueueSize(100)        // 대기 큐 크기
        .setIdleTimeout(30)              // 유휴 타임아웃 (초)
)
```

### 2. 트랜잭션 처리

```kotlin
pool.withTransaction { conn ->
    conn.preparedQuery("INSERT INTO orders (customer_id) VALUES ($1)")
        .execute(Tuple.of(customerId))
        .compose { insertResult ->
            val orderId = insertResult.property(PostgresClient.LAST_INSERTED_ID)
            conn.preparedQuery("INSERT INTO order_items (order_id, product_id) VALUES ($1, $2)")
                .execute(Tuple.of(orderId, productId))
        }
}.onSuccess {
    println("Transaction committed")
}.onFailure { err ->
    println("Transaction rolled back: ${err.message}")
}
```

### 3. Batch Insert

```kotlin
val batch = customers.map { customer ->
    Tuple.of(customer.name, customer.email)
}

pool.preparedQuery("INSERT INTO customers (name, email) VALUES ($1, $2)")
    .executeBatch(batch)
    .onSuccess { result ->
        println("Inserted ${result.rowCount()} rows")
    }
```

## 성능 비교

| 작업         | JDBC      | Vert.x SQL Client |
|------------|-----------|-------------------|
| 1000건 조회   | ~500ms    | ~50ms             |
| 1000건 삽입   | ~2000ms   | ~200ms            |
| 동시 연결 1000 | Thread 필요 | Event Loop로 처리    |

## 장단점

### 장점

- 가장 높은 성능과 SQL 제어권
- 메모리 효율적 (Thread per Request 대비)
- 다양한 DB 드라이버 지원

### 단점

- ORM 편의 기능 없음
- SQL 직접 작성 필요
- 학습 곡선 존재

## 참고

- Vert.x SQL Client는 ORM이 아닙니다. SQL을 직접 작성해야 합니다.
- Connection Pool 크기는 적절히 설정해야 합니다.
- 트랜잭션은 `withTransaction` 메서드를 사용합니다.
