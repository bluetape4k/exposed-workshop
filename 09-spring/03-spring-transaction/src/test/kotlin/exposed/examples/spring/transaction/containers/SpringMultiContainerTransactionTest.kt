package exposed.examples.spring.transaction.containers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.jdbc.JdbcDrivers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

/**
 * 서로 다른 2개의 Database 에 대한 Transaction 을 테스트한다.
 */
class SpringMultiContainerTransactionTest {

    companion object: KLogging()

    val orderContainer = AnnotationConfigApplicationContext(OrderConfig::class.java)
    val paymentContainer = AnnotationConfigApplicationContext(PaymentConfig::class.java)

    val orders: OrderService = orderContainer.getBean(OrderService::class.java)
    val payments: PaymentService = paymentContainer.getBean(PaymentService::class.java)

    @BeforeEach
    fun beforeEach() {
        orders.init()
        payments.init()
    }

    @Test
    fun `find all orders and payments`() {
        orders.findAll().shouldBeEmpty()
        payments.findAll().shouldBeEmpty()
    }

    @Test
    fun `create order and payment`() {
        orders.create()
        orders.findAll().size shouldBeEqualTo 1

        payments.create()
        payments.findAll().size shouldBeEqualTo 1
    }

    @Test
    fun `create order and payment with orders transaction`() {
        orders.transaction {
            payments.create()
            orders.create()
            payments.create()
        }
        orders.findAll().size shouldBeEqualTo 1
        payments.findAll().size shouldBeEqualTo 2
    }

    @Test
    fun `rollback transaction when transaction exception occurs`() {
        runCatching {
            orders.transaction {
                orders.create()
                payments.create()
                throw SpringTransactionTestException()
            }
        }
        orders.findAll().shouldBeEmpty()
        payments.findAll().size shouldBeEqualTo 1       // payment 는 PaymentService의 독립적인 transaction에서 수행된다.
    }

    @Test
    fun `rollback nested transaction when nested transaction exception occurs`() {
        runCatching {
            orders.transaction {
                orders.create()
                payments.databaseTemplate {
                    payments.create()
                    throw SpringTransactionTestException()
                }
            }
        }
        orders.findAll().shouldBeEmpty()
        payments.findAll().shouldBeEmpty()
    }

    @Test
    fun `rollback nested transaction when transaction exception occurs`() {
        runCatching {
            orders.transaction {
                orders.create()
                payments.databaseTemplate {
                    payments.create()
                }
                throw SpringTransactionTestException()
            }
        }
        orders.findAll().shouldBeEmpty()
        payments.findAll().size shouldBeEqualTo 1
    }

    @Test
    fun `read with exposed transaction block`() {
        orders.findAllWithExposedTrxBlock().shouldBeEmpty()
        payments.findAllWithExposedTxBlock().shouldBeEmpty()
    }

    @Test
    fun `create with exposed transaction block`() {
        orders.createWithExposedTxBlock()
        orders.findAllWithExposedTrxBlock().size shouldBeEqualTo 1

        payments.createWithExposedTxBlock()
        payments.findAllWithExposedTxBlock().size shouldBeEqualTo 1
    }

    @Test
    fun `create with exposed transaction block with nested transaction`() {
        orders.transaction {
            payments.createWithExposedTxBlock()
            orders.createWithExposedTxBlock()
            payments.createWithExposedTxBlock()
        }
        orders.findAllWithExposedTrxBlock().size shouldBeEqualTo 1
        payments.findAllWithExposedTxBlock().size shouldBeEqualTo 2
    }

    @Test
    fun `rollback transaction when order transaction exception`() {
        runCatching {
            orders.transaction {
                orders.createWithExposedTxBlock()
                payments.createWithExposedTxBlock()
                throw SpringTransactionTestException()
            }
        }
        orders.findAllWithExposedTrxBlock().shouldBeEmpty()
        payments.findAllWithExposedTxBlock().size shouldBeEqualTo 1  // Order Tx 만 rollback 된다.
    }

    @Test
    fun `rollback transaction when nested transaction exception occurs with exposed transaction block`() {
        runCatching {
            orders.transaction {
                orders.createWithExposedTxBlock()
                payments.databaseTemplate {
                    payments.createWithExposedTxBlock()
                    throw SpringTransactionTestException()
                }
            }
        }
        orders.findAllWithExposedTrxBlock().shouldBeEmpty()
        payments.findAllWithExposedTxBlock().shouldBeEmpty()
    }

    @Test
    fun `rollback transaction when transaction exception occurs with exposed transaction block`() {
        runCatching {
            orders.transaction {
                orders.createWithExposedTxBlock()
                payments.databaseTemplate {
                    payments.createWithExposedTxBlock()
                }
                throw SpringTransactionTestException()
            }
        }
        orders.findAllWithExposedTrxBlock().shouldBeEmpty()
        payments.findAllWithExposedTxBlock().size shouldBeEqualTo 1
    }


    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    class OrderConfig {

        companion object: KLogging()

        @Bean
        fun dataSource(): DataSource {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:order;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL"
                driverClassName = JdbcDrivers.DRIVER_CLASS_H2
                username = "sa"
                password = ""
            }
            return HikariDataSource(config)
        }

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
            return SpringTransactionManager(dataSource, DatabaseConfig { useNestedTransactions = true })
        }

        @Bean
        fun orderService(): OrderService = OrderService()
    }

    @Transactional
    class OrderService {

        fun findAll(): List<ResultRow> = Order.selectAll().toList()

        fun findAllWithExposedTrxBlock(): List<ResultRow> =
            org.jetbrains.exposed.sql.transactions.transaction {
                findAll()
            }

        fun create(buyerId: Long = 123L): Long =
            Order.insertAndGetId {
                it[buyer] = buyerId
            }.value

        fun createWithExposedTxBlock(buyerId: Long = 123L): Long =
            org.jetbrains.exposed.sql.transactions.transaction {
                create(buyerId)
            }

        fun init() {
            SchemaUtils.create(Order)
            Order.deleteAll()
        }

        fun transaction(block: () -> Unit) {
            block()
        }

    }

    object Order: LongIdTable("orders") {
        val buyer = long("buyer_id")
    }


    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    class PaymentConfig {

        companion object: KLogging()

        @Bean
        fun dataSource(): DataSource {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:payment;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL"
                driverClassName = JdbcDrivers.DRIVER_CLASS_H2
                username = "sa"
                password = ""
            }
            return HikariDataSource(config)
        }

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
            return SpringTransactionManager(dataSource, DatabaseConfig { useNestedTransactions = true })
        }

        @Bean
        fun paymentService(): PaymentService = PaymentService()
    }

    @Transactional
    class PaymentService {

        fun findAll(): List<ResultRow> = Payment.selectAll().toList()

        fun findAllWithExposedTxBlock(): List<ResultRow> =
            org.jetbrains.exposed.sql.transactions.transaction {
                findAll()
            }

        fun create(state: String = "created"): Long =
            Payment.insertAndGetId {
                it[this.state] = state
            }.value

        fun createWithExposedTxBlock(state: String = "created"): Long =
            org.jetbrains.exposed.sql.transactions.transaction {
                create(state)
            }

        fun init() {
            SchemaUtils.create(Payment)
            Payment.deleteAll()
        }

        fun databaseTemplate(block: () -> Unit) {
            block()
        }
    }

    object Payment: LongIdTable("payments") {
        val state = varchar("state", 50)
    }

    private class SpringTransactionTestException: Error()
}
