package exposed.examples.connection

import exposed.shared.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainIgnoringCase
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.SQLTransientException

class Ex02_ConnectionException {

    companion object: KLoggingChannel()

    abstract class ConnectionSpy(private val connection: Connection): Connection by connection {
        var commitCalled = false
        var rollbackCalled = false
        var closeCalled = false

        override fun commit() {
            commitCalled = true
            throw CommitException("성공했습니다. ConnectionSpy에서 CommitException을 발생시켰습니다.")
        }

        override fun rollback() {
            rollbackCalled = true
        }

        override fun close() {
            closeCalled = true
        }
    }

    private class WrappingDataSource<T: Connection>(
        private val testDB: TestDB,
        private val connectionDecorator: (Connection) -> T,
    ): DataSourceStub() {
        val connections = mutableListOf<T>()

        override fun getConnection(): Connection {
            val connection = DriverManager.getConnection(testDB.connection(), testDB.user, testDB.pass)
            val wrapped = connectionDecorator(connection)
            connections.add(wrapped)
            return wrapped
        }
    }

    private class RollbackException: SQLTransientException()
    private class ExceptionOnRollbackConnection(connection: Connection): ConnectionSpy(connection) {
        override fun rollback() {
            super.rollback()
            throw RollbackException()
        }
    }

    private class CommitException: SQLTransientException {
        constructor(): super()
        constructor(message: String): super(message)
    }

    private class ExceptionOnCommitConnection(connection: Connection): ConnectionSpy(connection) {
        override fun commit() {
            super.commit()
            throw CommitException()
        }
    }

    @AfterEach
    fun afterEach() {
        TransactionManager.resetCurrent(null)
    }

    @Test
    fun `transaction repetition works even if rollback throws exception`() {
        `_transaction repetition works even if rollback throws exception`(::ExceptionOnRollbackConnection)
    }

    private fun `_transaction repetition works even if rollback throws exception`(
        connectionDecorator: (Connection) -> ConnectionSpy,
    ) {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }
        Class.forName(TestDB.H2.driver).getDeclaredConstructor().newInstance()

        val wrappingDataSource = WrappingDataSource(TestDB.H2, connectionDecorator)
        val db = Database.connect(datasource = wrappingDataSource)
        try {
            transaction(Connection.TRANSACTION_SERIALIZABLE, db = db) {
                maxAttempts = 5
                this.exec("BROKEN_SQL_THAT_CAUSES_EXCEPTION()")
            }
            fail("위의 Tx에서 예외를 발생 시켜야 합니다.")
        } catch (e: SQLException) {
            e.toString() shouldContainIgnoringCase "BROKEN_SQL_THAT_CAUSES_EXCEPTION"
            wrappingDataSource.connections shouldHaveSize 5

            wrappingDataSource.connections.forEach { connection ->
                connection.commitCalled.shouldBeFalse()
                connection.rollbackCalled.shouldBeTrue()
                connection.closeCalled.shouldBeTrue()
            }
        }
    }

    @Test
    fun `transaction repetition works when commit throws exception`() {
        `_transaction repetition works when commit throws exception`(::ExceptionOnCommitConnection)
    }

    private fun `_transaction repetition works when commit throws exception`(
        connectionDecorator: (Connection) -> ConnectionSpy,
    ) {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }
        Class.forName(TestDB.H2.driver).getDeclaredConstructor().newInstance()

        val wrappingDataSource = WrappingDataSource(TestDB.H2, connectionDecorator)
        val db = Database.connect(wrappingDataSource)

        try {
            transaction(Connection.TRANSACTION_SERIALIZABLE, db = db) {
                maxAttempts = 5
                this.exec("SELECT 1;")
            }
            fail("위의 Tx에서 예외를 발생 시켜야 합니다.")
        } catch (_: CommitException) {
            wrappingDataSource.connections shouldHaveSize 5

            wrappingDataSource.connections.forEach { connection ->
                connection.commitCalled.shouldBeTrue()
                connection.closeCalled.shouldBeTrue()
            }
        }
    }

    @Test
    fun `transaction throws exception if all commits throws exception`() {
        `_transaction throws exception if all commits throws exception`(::ExceptionOnCommitConnection)
    }

    private fun `_transaction throws exception if all commits throws exception`(
        connectionDecorator: (Connection) -> ConnectionSpy,
    ) {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }
        Class.forName(TestDB.H2.driver).getDeclaredConstructor().newInstance()

        val wrappingDataSource = WrappingDataSource(TestDB.H2, connectionDecorator)
        val db = Database.connect(wrappingDataSource)

        try {
            transaction(Connection.TRANSACTION_SERIALIZABLE, db = db) {
                maxAttempts = 5
                this.exec("SELECT 1;")
            }
            fail("위의 Tx에서 예외를 발생 시켜야 합니다.")
        } catch (_: CommitException) {
            // Nothing to do
        }
    }

    private class CloseException: SQLTransientException()
    private class ExceptionOnRollbackCloseConnection(connection: Connection): ConnectionSpy(connection) {
        override fun rollback() {
            super.rollback()
            throw CloseException()
        }

        override fun close() {
            super.close()
            throw CloseException()
        }
    }

    @Test
    fun `transaction repetition works even if rollback and close throws exception`() {
        `_transaction repetition works even if rollback throws exception`(::ExceptionOnRollbackCloseConnection)
    }

    @Test
    fun `transaction repetition works when commit and close throws exception`() {
        `_transaction repetition works when commit throws exception`(::ExceptionOnCommitConnection)
    }

    private class ExceptionOnCommitCloseConnection(connection: Connection): ConnectionSpy(connection) {
        override fun commit() {
            super.commit()
            throw CommitException()
        }

        override fun close() {
            super.close()
            throw CloseException()
        }
    }

    @Test
    fun `transaction throws exception if all commits and close throws exception`() {
        `_transaction throws exception if all commits throws exception`(::ExceptionOnCommitCloseConnection)
    }
}
