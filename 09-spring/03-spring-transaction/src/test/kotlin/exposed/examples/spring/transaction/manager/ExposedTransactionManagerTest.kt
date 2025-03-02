package exposed.examples.spring.transaction.manager

import exposed.examples.spring.transaction.AbstractSpringTransactionTest
import exposed.examples.spring.transaction.utils.execute
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional

/**
 * Exposed 의 TransactionManager 를 사용한 트랜잭션 테스트
 */
class ExposedTransactionManagerTest: AbstractSpringTransactionTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    object T1: Table("t1") {
        val c1 = varchar("c1", Int.MIN_VALUE.toString().length)
    }

    private fun T1.insertRandom() {
        insert {
            it[c1] = kotlin.random.Random.nextInt().toString()
        }
    }

    @BeforeEach
    fun beforeEach() {
        transactionManager.execute {
            SchemaUtils.create(T1)
        }
    }

    @AfterEach
    fun afterEach() {
        transactionManager.execute {
            SchemaUtils.drop(T1)
        }
    }

    @Transactional
    @Commit
    @RepeatedTest(REPEAT_SIZE)
    fun `insert record and read record count`() {
        T1.insertRandom()
        T1.selectAll().count() shouldBeEqualTo 1L
    }

    @Transactional
    @Commit
    @RepeatedTest(REPEAT_SIZE)
    fun `insert record and read all records`() {
        val c1 = kotlin.random.Random.nextInt().toString()
        T1.insert {
            it[T1.c1] = c1
        }
        T1.selectAll().single()[T1.c1] shouldBeEqualTo c1
    }
}
