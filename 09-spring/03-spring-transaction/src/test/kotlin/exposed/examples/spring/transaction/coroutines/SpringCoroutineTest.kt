package exposed.examples.spring.transaction.coroutines

import exposed.examples.spring.transaction.AbstractSpringTransactionTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.debug
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.debug.junit5.CoroutinesTimeout
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.api.RepeatedTest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional

/**
 * Coroutines 환경에서 `@Transactional` 을 지원하는 방법에 대한 예제입니다.
 */
@Suppress("DEPRECATION")
class SpringCoroutineTest: AbstractSpringTransactionTest() {

    companion object {
        private val timeout = CoroutinesTimeout(60_000)
        private const val REPEAT_SIZE = 5
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS COROUTINES_TESTER (
     *      ID SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object Tester: IntIdTable("coroutines_tester") {
        val name = varchar("name", 255)
    }

    @Transactional
    @Commit
    @RepeatedTest(REPEAT_SIZE)
    fun `중첩된 Coroutine Transaction 테스트`() = runSuspendIO {
        try {
            newSuspendedTransaction {
                log.debug { "Create schema ..." }
                SchemaUtils.create(Tester)
            }

            /**
             * recordCount 만큼 동시에 insert 를 수행한다. (transaction/connection이 recordCount 수만큼 동시에 생성된다.)
             *
             * ```sql
             * -- 동시에 실행되므로, 로그 상으로는 순서가 보장되지 않는다.
             * INSERT INTO COROUTINES_TESTER ("name") VALUES ('Tester 1');
             * INSERT INTO COROUTINES_TESTER ("name") VALUES ('Tester 2');
             * INSERT INTO COROUTINES_TESTER ("name") VALUES ('Tester 4');
             * INSERT INTO COROUTINES_TESTER ("name") VALUES ('Tester 0');
             * INSERT INTO COROUTINES_TESTER ("name") VALUES ('Tester 3');
             * ```
             */
            val recordCount = 5
            List(recordCount) { index ->
                suspendedTransactionAsync {
                    Tester.insert {
                        it[name] = "Tester $index"
                    }
                    index
                }
            }.awaitAll()

            newSuspendedTransaction(readOnly = true) {
                log.debug { "Load Tester records ..." }
                Tester.selectAll().count() shouldBeEqualTo recordCount.toLong()
            }
        } finally {
            newSuspendedTransaction {
                log.debug { "Drop schema ..." }
                SchemaUtils.drop(Tester)
            }
        }
    }
}
