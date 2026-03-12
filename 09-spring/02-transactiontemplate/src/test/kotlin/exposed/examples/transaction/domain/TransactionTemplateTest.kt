package exposed.examples.transaction.domain

import exposed.examples.transaction.AbstractTransactionApplicationTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Spring [TransactionOperations] 을 사용한 트랜잭션 경계 설정 방법을 테스트합니다.
 * Spring 트랜잭션, Exposed 트랜잭션, 혼합 방식의 동작 차이를 비교합니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TransactionTemplateTest(
    @param:Autowired private val bookService: BookService,
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
): AbstractTransactionApplicationTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Order(1)
    @RepeatedTest(REPEAT_SIZE)
    fun `without spring transaction`() {
        bookService.execWithoutSpringTransaction()
    }

    @Order(2)
    @RepeatedTest(REPEAT_SIZE)
    fun `with spring transaction`() {
        bookService.executeSpringTransaction()
    }

    @Order(3)
    @RepeatedTest(REPEAT_SIZE)
    fun `with exposed transaction`() {
        bookService.execWithExposedTransaction()
    }

    @Order(4)
    @RepeatedTest(REPEAT_SIZE)
    fun `with spring and exposed transaction`() {
        bookService.execWithSpringAndExposedTransactions()
    }

    @Order(5)
    @RepeatedTest(REPEAT_SIZE)
    fun `with Transactional annotation`() {
        bookService.execTransactionalAnnotation()
    }

    @Order(6)
    @Test
    fun `각 실행 방식은 author를 1건 생성해야 한다`() {
        fun authorCount(): Long =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUTHORS", Long::class.java) ?: 0L

        val beforeWithoutTx = authorCount()
        bookService.execWithoutSpringTransaction()
        authorCount() shouldBeEqualTo beforeWithoutTx + 1L

        val beforeSpringTx = authorCount()
        bookService.executeSpringTransaction()
        authorCount() shouldBeEqualTo beforeSpringTx + 1L
    }
}
