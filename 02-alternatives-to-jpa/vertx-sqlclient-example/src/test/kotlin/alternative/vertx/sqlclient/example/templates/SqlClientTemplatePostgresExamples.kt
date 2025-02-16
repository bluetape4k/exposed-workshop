package alternative.vertx.sqlclient.example.templates

import alternative.vertx.sqlclient.example.AbstractSqlClientTest
import alternative.vertx.sqlclient.example.model.Customer
import alternative.vertx.sqlclient.example.model.CustomerRowMapper
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.templates.tupleMapperOfRecord
import io.bluetape4k.vertx.sqlclient.tests.testWithTransactionSuspending
import io.bluetape4k.vertx.sqlclient.withTransactionSuspending
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.templates.SqlTemplate
import io.vertx.sqlclient.templates.TupleMapper
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SqlClientTemplatePostgresExamples: AbstractSqlClientTest() {

    companion object: KLogging()

    @BeforeAll
    fun setup(vertx: Vertx) {
        // setup 에서는 testContext 가 불필요합니다. 만약 injection을 받으면 꼭 completeNow() 를 호출해야 합니다.
        runBlocking(vertx.dispatcher()) {
            val pool = vertx.getPostgresPool()
            try {
                pool.withTransactionSuspending { conn ->
                    // conn.query("DROP TABLE customers IF EXISTS;").execute().coAwait()
                    conn.query(
                        """
                        CREATE TABLE IF NOT EXISTS customers(
                             id BIGINT PRIMARY KEY,
                             first_name VARCHAR(255) NOT NULL,
                             last_name VARCHAR(255) NOT NULL,
                             email VARCHAR(255),
                             mobile VARCHAR(255),
                             age INT 
                        );
                        """.trimIndent()
                    ).execute().coAwait()

                    val insertQuery =
                        "INSERT INTO customers (id, first_name, last_name) VALUES (1, 'John', 'Doe'), (2, 'Jane', 'Doe');"
                    conn.query(insertQuery).execute().coAwait()
                }
            } finally {
                pool.close().coAwait()
            }
        }
    }

    @Test
    fun `query with parameters`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getPostgresPool()
        try {
            vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                val query = "SELECT * FROM customers WHERE id = #{ID}"
                val parameters = mapOf("ID" to 1)
                val rowSet = SqlTemplate.forQuery(pool, query).execute(parameters).coAwait()

                val customers = rowSet.map { row ->
                    CustomerRowMapper.map(row)
                }

                customers.forEach { customer ->
                    log.debug { "customer: $customer" }
                }

                customers.size shouldBeEqualTo 1
                customers.single().id shouldBeEqualTo 1L
                customers.single().firstName shouldBeEqualTo "John"
                customers.single().lastName shouldBeEqualTo "Doe"
            }
        } finally {
            pool.close().coAwait()
        }
    }

    @Test
    fun `insert with parameters`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getPostgresPool()
        try {
            vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                val insertStmt =
                    "INSERT INTO customers (id, first_name, last_name) VALUES (#{id}, #{firstName}, #{lastName});"
                val parameters = mapOf(
                    "id" to 3,
                    "firstName" to "Jane",
                    "lastName" to "Smith"
                )
                val result = SqlTemplate
                    .forUpdate(pool, insertStmt)
                    .execute(parameters)
                    .coAwait()

                result.rowCount() shouldBeEqualTo 1
            }
        } finally {
            pool.close().coAwait()
        }
    }

    @Test
    fun `query with row mapper`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getPostgresPool()
        try {
            vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                val query = "SELECT * FROM customers WHERE id = #{ID}"
                val parameters = mapOf("ID" to 1)

                val customers = SqlTemplate
                    .forQuery(pool, query)
                    .mapTo(CustomerRowMapper)
                    .execute(parameters)
                    .coAwait()

                customers.forEach { customer ->
                    log.debug { "customer: $customer" }
                }

                customers.size() shouldBeEqualTo 1
                customers.single().id shouldBeEqualTo 1L
                customers.single().firstName shouldBeEqualTo "John"
                customers.single().lastName shouldBeEqualTo "Doe"
            }
        } finally {
            pool.close().coAwait()
        }
    }

    @Test
    fun `binding row with anemic JsonMapper`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getPostgresPool()
        try {
            vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                val query = "SELECT * FROM customers WHERE id = #{ID}"
                val parameters = mapOf("ID" to 1)

                val customers = SqlTemplate
                    .forQuery(pool, query)
                    .mapTo(Row::toJson)    // mapping Row -> Json Object
                    .execute(parameters)
                    .coAwait()

                customers.size() shouldBeEqualTo 1
                customers.forEach { json ->
                    log.debug { "customer: ${json.encode()}" }
                }
            }
        } finally {
            pool.close().coAwait()
        }
    }

    @Test
    fun `insert with parameters binding customer mapper`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getPostgresPool()
        try {
            vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                val insertStmt =
                    "INSERT INTO customers (id, first_name, last_name) VALUES (#{id}, #{firstName}, #{lastName});"
                val customer = Customer(
                    id = 4,
                    firstName = "Iron",
                    lastName = "Man"
                )

                val result = SqlTemplate
                    .forUpdate(pool, insertStmt)
                    .mapFrom(tupleMapperOfRecord<Customer>())
                    .execute(customer)
                    .coAwait()

                result.rowCount() shouldBeEqualTo 1
            }
        } finally {
            pool.close().coAwait()
        }
    }

    @Test
    fun `insert with parameters binding anemic json mapper`(vertx: Vertx, testContext: VertxTestContext) =
        runSuspendIO {
            val pool = vertx.getPostgresPool()
            try {
                vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                    val insertStmt =
                        "INSERT INTO customers (id, first_name, last_name) VALUES (#{id}, #{firstName}, #{lastName});"
                    val customer = json {
                        obj {
                            put(Customer::id.name, 5)
                            put(Customer::firstName.name, "Moon")
                            put(Customer::lastName.name, "Knight")
                        }
                    }

                    val result = SqlTemplate
                        .forUpdate(pool, insertStmt)
                        .mapFrom(TupleMapper.jsonObject())
                        .execute(customer)
                        .coAwait()

                    result.rowCount() shouldBeEqualTo 1
                }
            } finally {
                pool.close().coAwait()
            }
        }

    @Test
    fun `insert with parameters binding jackson databind`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getPostgresPool()
        try {
            vertx.testWithTransactionSuspending(testContext, pool) { conn ->
                val insertStmt =
                    "INSERT INTO customers (id, first_name, last_name) VALUES (#{id}, #{firstName}, #{lastName});"
                val customer = Customer(6, faker.name().firstName(), faker.name().lastName())

                val result = SqlTemplate
                    .forUpdate(pool, insertStmt)
                    .mapFrom(Customer::class.java)
                    .execute(customer)
                    .coAwait()

                result.rowCount() shouldBeEqualTo 1
            }
        } finally {
            pool.close().coAwait()
        }
    }
}
