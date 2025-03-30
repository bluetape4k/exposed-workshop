package exposed.examples.connection.h2

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.shared.tests.TestDB
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

class Ex01_H2_ConnectionPool {

    companion object: KLogging()

    private val hikariDataSource1 by lazy {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:hikariDB1"
                maximumPoolSize = 10
            }
        )
    }

    private val hikariDB1 by lazy {
        Database.connect(hikariDataSource1)
    }

    @Test
    fun `suspend transactions exceeding pool size`() {
        Assumptions.assumeTrue(TestDB.H2 in TestDB.enabledDialects())

        transaction(db = hikariDB1) {
            SchemaUtils.create(TestTable)
        }

        // DataSource의 maximumPoolSize를 초과하는 트랜잭션을 실행합니다.
        val exceedsPoolSize = (hikariDataSource1.maximumPoolSize * 2 + 1).coerceAtMost(100)
        log.debug { "Exceeds pool size: $exceedsPoolSize" }

        runBlocking {
            val tasks = List(exceedsPoolSize) {
                suspendedTransactionAsync(db = hikariDB1) {
                    val entity = TestEntity.new { testValue = "test$it" }
                    log.debug { "Created test entity: $entity" }
                }
            }
            tasks.awaitAll()
        }

        // 실제 생성된 엔티티 수는 exceedsPoolSize 만큼이다. (즉 Connection 이 재활용되었다는 뜻)
        transaction(db = hikariDB1) {
            TestEntity.all().count() shouldBeEqualTo exceedsPoolSize.toLong()
            SchemaUtils.drop(TestTable)
        }
    }

    object TestTable: IntIdTable("HIKARI_TESTER") {
        val testValue = varchar("test_value", 32)
    }

    class TestEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TestEntity>(TestTable)

        var testValue: String by TestTable.testValue

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("testValue", testValue)
            .toString()
    }
}
