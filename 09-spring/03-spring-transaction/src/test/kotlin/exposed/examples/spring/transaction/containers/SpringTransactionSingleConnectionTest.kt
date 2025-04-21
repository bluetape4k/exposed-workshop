package exposed.examples.spring.transaction.containers

import exposed.examples.spring.transaction.utils.execute
import io.bluetape4k.codec.Base58
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

class SpringTransactionSingleConnectionTest {

    private object T1: Table() {
        val c1 = varchar("c1", Int.MIN_VALUE.toString().length)
    }

    private val singleConnectionH2TestContainer =
        AnnotationConfigApplicationContext(SingleConnectionH2TestConfig::class.java)
    val dataSource: DataSource = singleConnectionH2TestContainer.getBean(DataSource::class.java)
    val transactionManager = singleConnectionH2TestContainer.getBean(PlatformTransactionManager::class.java)

    @BeforeEach
    fun beforeEach() {
        transactionManager.execute {
            SchemaUtils.create(T1)

            T1.insert {
                it[T1.c1] = "c1"
            }
        }
    }

    @AfterEach
    fun afterEach() {
        transactionManager.execute {
            SchemaUtils.drop(T1)
        }
    }

    @Test
    fun `isolation level 이 ISOLATION_DEFAULT이 아닌 transaction 실행하기`() {
        transactionManager.execute(
            isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE,
        ) {
            T1.selectAll().toList()
        }
    }

    @Test
    fun `isolation level 이 ISOLATION_DEFAULT이 아닌 중첩된 transaction 실행하기`() {
        transactionManager.execute(
            isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE,
        ) {
            T1.selectAll().toList()

            // 중첩된 transaction 이라면 같은 connection 을 사용하기 때문에 부모 transaction 의 isolation level 을 상속받는다.
            // 단, propagation 이 PROPAGATION_REQUIRES_NEW 이라면 기본 isolation level 이 적용된다.
            transactionManager.execute(
                isolationLevel = TransactionDefinition.ISOLATION_READ_UNCOMMITTED,
            ) {
                DataSourceUtils.getConnection(dataSource).use { conn ->
                    conn.transactionIsolation shouldBeEqualTo TransactionDefinition.ISOLATION_SERIALIZABLE
                }
                T1.selectAll().toList()
            }
            T1.selectAll().toList()
        }
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    class SingleConnectionH2TestConfig {

        @Bean
        fun singleConnectionH2DataSource(): DataSource =
            SingleConnectionDataSource(
                "jdbc:h2:mem:${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;",
                "root",
                "",
                true
            )

        @Bean
        fun transactionManager(
            @Qualifier("singleConnectionH2DataSource") dataSource: DataSource,
        ): PlatformTransactionManager {
            // Exposed의 SpringTransactionManager는 기본적으로 Nested Transactions를 지원하지 않는다.
            return SpringTransactionManager(
                dataSource = dataSource,
                DatabaseConfig { useNestedTransactions = true }
            )
        }
    }
}
