package exposed.examples.spring.transaction.manager

import exposed.examples.spring.transaction.mock.ConnectionSpy
import exposed.examples.spring.transaction.mock.DataSourceSpy
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.TransactionTemplate
import java.sql.SQLException
import kotlin.test.assertFailsWith

/**
 * Exposed의 [SpringTransactionManager] 를 사용한 트랜잭션 테스트
 */
class SpringTransactionManagerTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    private val ds1 = DataSourceSpy(::ConnectionSpy)
    private val con1 = ds1.con as ConnectionSpy

    private val ds2 = DataSourceSpy(::ConnectionSpy)
    private val con2 = ds2.con as ConnectionSpy

    @BeforeEach
    fun beforeEach() {
        con1.clearMock()
        con2.clearMock()
    }

    @AfterEach
    fun afterEach() {
        while (TransactionManager.currentOrNull() != null) {
            TransactionManager.defaultDatabase?.let { database ->
                TransactionManager.closeAndUnregister(database)
            }
        }
    }

    @Test
    fun `트랜잭션 시작 시 트랜잭션 매니저를 설정한다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert(false)
    }

    @Test
    fun `두 개의 트랜잭션이 동시에 존재해도 올바른 트랜젹션 매니저를 설정한다`() {
        val tm1 = SpringTransactionManager(ds1)
        val tm2 = SpringTransactionManager(ds2)

        tm1.executeAssert(false)
        tm2.executeAssert(false)
    }

    @Test
    fun `두 개의 트랜잭션이 중첩되는 경우에도 올바른 트랜잭션 매니저를 설정한다`() {
        val tm1 = SpringTransactionManager(ds1)
        val tm2 = SpringTransactionManager(ds2)

        tm2.executeAssert(false) {
            tm1.executeAssert(false)

            val database = TransactionManager.current().db
            TransactionManager.managerFor(database) shouldBeEqualTo TransactionManager.manager
        }
    }

    @Test
    fun `트랜잭션이 성공하면 커넥션은 commit 되고 close 된다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert()

        // connection 작업 확인
        con1.verifyCallOrder("setAutoCommit", "commit", "close").shouldBeTrue()
        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `트랜잭션이 실패하면 커넥션은 rollback 되고 close 된다`() {
        val tm = SpringTransactionManager(ds1)
        val ex = RuntimeException("Application exception")

        try {
            tm.executeAssert { throw ex }
        } catch (e: RuntimeException) {
            e shouldBeEqualTo ex
        }

        // connection 작업 확인
        con1.verifyCallOrder("rollback", "close").shouldBeTrue()
        con1.rollbackCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `중첩된 트랜잭션이 성공하면 커넥션은 commit 되고 close 된다`() {
        val tm = SpringTransactionManager(ds1)

        tm.executeAssert {
            tm.executeAssert()
        }

        // connection 작업 확인
        con1.verifyCallOrder("setAutoCommit", "commit", "close").shouldBeTrue()
        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `중첩된 서로 다른 두 트랜잭션 매니저가 성공하면 각각의 커넥션은 commit 되고 close 된다`() {
        val tm1 = SpringTransactionManager(ds1)
        val tm2 = SpringTransactionManager(ds2)

        tm1.executeAssert {
            tm2.executeAssert()

            val database = TransactionManager.current().db
            TransactionManager.managerFor(database) shouldBeEqualTo TransactionManager.manager
        }

        // connection 작업 확인
        con1.verifyCallOrder("setAutoCommit", "commit", "close").shouldBeTrue()
        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1

        con2.verifyCallOrder("setAutoCommit", "commit", "close").shouldBeTrue()
        con2.commitCallCount shouldBeEqualTo 1
        con2.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `중첩된 서로 다른 두 트랜잭션 매니저가 실패하면 각각의 커넥션은 rollback 되고 close 된다`() {
        val tm1 = SpringTransactionManager(ds1)
        val tm2 = SpringTransactionManager(ds2)
        val ex = RuntimeException("Application exception")

        try {
            tm1.executeAssert {
                tm2.executeAssert {
                    throw ex
                }
                val database = TransactionManager.current().db
                TransactionManager.managerFor(database) shouldBeEqualTo TransactionManager.manager
            }
        } catch (e: RuntimeException) {
            e shouldBeEqualTo ex
        }

        // connection 작업 확인
        con2.verifyCallOrder("rollback", "close").shouldBeTrue()
        con2.commitCallCount shouldBeEqualTo 0
        con2.rollbackCallCount shouldBeEqualTo 1
        con2.closeCallCount shouldBeEqualTo 1

        // connection 작업 확인
        con1.verifyCallOrder("rollback", "close").shouldBeTrue()
        con1.commitCallCount shouldBeEqualTo 0
        con1.rollbackCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `LazyConnectionDataSourceProxy 를 이용한 트랜잭션 커밋은 성공한다`() {
        val lazyDs = LazyConnectionDataSourceProxy(ds1)
        val tm = SpringTransactionManager(lazyDs)
        tm.executeAssert()

        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `LazyConnectionDataSourceProxy 를 이용한 트랜잭션 롤백은 성공한다`() {
        val lazyDs = LazyConnectionDataSourceProxy(ds1)
        val tm = SpringTransactionManager(lazyDs)
        val ex = RuntimeException("Application exception")

        try {
            tm.executeAssert {
                throw ex
            }
        } catch (e: RuntimeException) {
            e shouldBeEqualTo ex
        }

        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `TransactionAwareDataSourceProxy 를 이용한 트랜잭션 커밋은 성공한다`() {
        val txAwareDs = TransactionAwareDataSourceProxy(ds1)
        val tm = SpringTransactionManager(txAwareDs)
        tm.executeAssert()

        con1.verifyCallOrder("setAutoCommit", "commit").shouldBeTrue()
        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeGreaterThan 0
    }

    @Test
    fun `TransactionAwareDataSourceProxy 를 이용한 트랜잭션 롤백은 성공한다`() {
        val txAwareDs = TransactionAwareDataSourceProxy(ds1)
        val tm = SpringTransactionManager(txAwareDs)
        val ex = RuntimeException("Application exception")
        try {
            tm.executeAssert {
                throw ex
            }
        } catch (e: RuntimeException) {
            e shouldBeEqualTo ex
        }

        con1.verifyCallOrder("setAutoCommit", "rollback").shouldBeTrue()
        con1.rollbackCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeGreaterThan 0
    }

    @Test
    fun `트랜잭션이 commit 시 예외가 발생하면 rollback 된다`() {
        con1.mockCommit = { throw SQLException("Commit failure") }

        val tm = SpringTransactionManager(ds1)
        tm.isRollbackOnCommitFailure = true
        assertFailsWith<TransactionSystemException> {
            tm.executeAssert()
        }

        con1.verifyCallOrder("setAutoCommit", "commit", "isClosed", "rollback", "close").shouldBeTrue()
        con1.commitCallCount shouldBeEqualTo 1
        con1.rollbackCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `트랜잭션 롤백 시 예외가 발생하면 connection이 close 된다`() {
        con1.mockRollback = { throw SQLException("Rollback failure") }

        val tm = SpringTransactionManager(ds1)
        assertFailsWith<TransactionSystemException> {
            tm.executeAssert {
                it.isRollbackOnly.shouldBeFalse()
                it.setRollbackOnly()
                it.isRollbackOnly.shouldBeTrue()
            }
        }

        con1.verifyCallOrder("setAutoCommit", "isClosed", "rollback", "close").shouldBeTrue()
        con1.rollbackCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `중첩된 트랜잭션에서 commit 하면 성공한다`() {
        val tm = SpringTransactionManager(ds1, DatabaseConfig { useNestedTransactions = true })

        tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_NESTED) {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_NESTED)
            it.isNewTransaction.shouldBeTrue()
        }

        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `중첩된 트랜잭션을 rollback 하면 성공한다`() {
        val tm = SpringTransactionManager(ds1, DatabaseConfig { useNestedTransactions = true })

        tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_NESTED) {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_NESTED) { status ->
                status.setRollbackOnly()
            }
            it.isNewTransaction.shouldBeTrue()
        }

        con1.rollbackCallCount shouldBeEqualTo 1
        con1.releaseSavepointCallCount shouldBeEqualTo 1
        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `requires new 트랜잭션 내부에서 commit 되면 외부도 commit 된다`() {
        val tm = SpringTransactionManager(ds1)

        tm.executeAssert {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
                it.isNewTransaction.shouldBeTrue()
            }
            it.isNewTransaction.shouldBeTrue()
        }

        // 새로운 트랜잭션이 생성되었으므로 commit, close 가 두 번 발생한다.
        con1.commitCallCount shouldBeEqualTo 2
        con1.closeCallCount shouldBeEqualTo 2
    }


    @Test
    fun `requires new 트랜잭션 내부에서 rollback이 발생하면 내부는 rollback되고 외부는 commit 된다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW) { status ->
                status.isNewTransaction.shouldBeTrue()
                status.setRollbackOnly()
            }
            it.isNewTransaction.shouldBeTrue()
        }

        con1.commitCallCount shouldBeEqualTo 1
        con1.rollbackCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 2
    }

    @Test
    fun `not support 옵션인 경우 connection 얻기에 실패한다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(
                initializeConnection = false,
                propagationBehavior = TransactionDefinition.PROPAGATION_NOT_SUPPORTED
            ) {
                assertFailsWith<IllegalStateException> {
                    TransactionManager.current().connection
                }
            }
            it.isNewTransaction.shouldBeTrue()
            TransactionManager.current().connection
        }

        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `mandatory with transaction은 성공한다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_MANDATORY)
            it.isNewTransaction.shouldBeTrue()
            TransactionManager.current().connection
        }

        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `mandatory without transaction 은 예외가 발생한다`() {
        val tm = SpringTransactionManager(ds1)
        assertFailsWith<IllegalTransactionStateException> {
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_MANDATORY)
        }
    }

    @Test
    fun `support with transaction 은 성공한다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert {
            it.isNewTransaction.shouldBeTrue()
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_SUPPORTS)
            it.isNewTransaction.shouldBeTrue()
            TransactionManager.current().connection
        }

        con1.commitCallCount shouldBeEqualTo 1
        con1.closeCallCount shouldBeEqualTo 1
    }

    @Test
    fun `support without transaction 은 예외가 발생한다`() {
        val tm = SpringTransactionManager(ds1)
        assertFailsWith<IllegalStateException> {
            tm.executeAssert(propagationBehavior = TransactionDefinition.PROPAGATION_SUPPORTS)
        }
        tm.executeAssert(initializeConnection = false, propagationBehavior = TransactionDefinition.PROPAGATION_SUPPORTS)
        con1.commitCallCount shouldBeEqualTo 0
        con1.rollbackCallCount shouldBeEqualTo 0
        con1.closeCallCount shouldBeEqualTo 0
    }

    @Test
    fun `transaction timeout 설정하기`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert(initializeConnection = true, timeout = 1) {
            TransactionManager.current().queryTimeout shouldBeEqualTo 1
        }
    }

    @Test
    fun `transaction timeout propagation 은 새로운 트랜잭션이 아니라면 전파되지 않는다`() {
        val tm = SpringTransactionManager(ds1)
        tm.executeAssert(initializeConnection = true, timeout = 3) {
            tm.executeAssert(initializeConnection = true, timeout = 5) {
                TransactionManager.current().queryTimeout shouldBeEqualTo 3
            }
            TransactionManager.current().queryTimeout shouldBeEqualTo 3
        }
    }

    fun SpringTransactionManager.executeAssert(
        initializeConnection: Boolean = true,
        propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRED,
        timeout: Int? = null,
        body: (TransactionStatus) -> Unit = {},
    ) {
        val txTemplate = TransactionTemplate(this).apply {
            this.propagationBehavior = propagationBehavior
            timeout?.let { this.timeout = it }
        }

        txTemplate.executeWithoutResult { txStatus ->
            TransactionManager.currentOrNull()?.db?.let { db ->
                log.debug { "db = $db" }
                TransactionManager.current().transactionManager shouldBeEqualTo TransactionManager.managerFor(db)
            }

            if (initializeConnection) {
                TransactionManager.current().connection
            }

            body(txStatus)
        }
    }
}
