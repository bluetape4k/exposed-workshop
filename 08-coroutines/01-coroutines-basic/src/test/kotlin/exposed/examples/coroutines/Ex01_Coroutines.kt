package exposed.examples.coroutines

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import io.bluetape4k.collections.intRangeOf
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedConnection
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith

class Ex01_Coroutines: AbstractExposedTest() {

    companion object: KLogging() {

        private val singleThreadDispatcher: ExecutorCoroutineDispatcher =
            Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS coroutines_tester (id SERIAL PRIMARY KEY)
     * ```
     */
    object Tester: IntIdTable("coroutines_tester")

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS coroutines_tester_unique (id INT PRIMARY KEY);
     * ALTER TABLE coroutines_tester_unique ADD CONSTRAINT coroutines_tester_unique_id_unique UNIQUE (id);
     * ```
     */
    object TesterUnique: Table("coroutines_tester_unique") {
        val id = integer("id").uniqueIndex()
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun Transaction.getTesterById(id: Int): ResultRow? =
        withSuspendTransaction {
            Tester.selectAll()
                .where { Tester.id eq id }
                .singleOrNull()
        }

    /**
     * Coroutines 환경 하에서 여러 작업을 순차적으로 수행합니다.
     *
     * ```console
     * 10:59:10.999 DEBUG [1-thread-1 @coroutine#32] Exposed :37: SELECT coroutines_tester.id FROM coroutines_tester WHERE coroutines_tester.id = 1
     * 10:59:11.014 DEBUG [1-thread-1 @coroutine#34] Exposed :37: SELECT coroutines_tester.id FROM coroutines_tester WHERE coroutines_tester.id = 1
     * 10:59:11.016 DEBUG [r-worker-1 @coroutine#35] Exposed :37: SELECT coroutines_tester.id FROM coroutines_tester WHERE coroutines_tester.id = 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspended transaction으로 시퀀셜 작업 수행하기`(testDB: TestDB) = runSuspendIO {
        withSuspendedTables(testDB, Tester) {
            newSuspendedTransaction(context = singleThreadDispatcher) {
                val id = Tester.insertAndGetId { }
                flushCache()
                entityCache.clear()
                getTesterById(id.value)!![Tester.id].value shouldBeEqualTo id.value
            }

            val result = newSuspendedTransaction(context = singleThreadDispatcher) {
                getTesterById(1)!![Tester.id].value
            }
            result shouldBeEqualTo 1

            getTesterById(1)!![Tester.id].value shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendedTransactionAsync으로 동시 작업 수행하기`(testDB: TestDB) = runSuspendIO {
        withSuspendedTables(testDB, TesterUnique) {
            val originId = 1
            val updatedId = 99

            // 새로운 레코드 생성
            newSuspendedTransaction {
                TesterUnique.insert {
                    it[TesterUnique.id] = originId
                }
                flushCache()
                entityCache.clear()

                TesterUnique.selectAll().single()[TesterUnique.id] shouldBeEqualTo originId
            }

            // 비동기 방식으로 originId 를 가진 레코드를 삽입한다
            val insertJob = suspendedTransactionAsync(Dispatchers.IO) {
                // 기존 레코드가 있기 때문에 Unique Constraint 위배 예외가 발생할 수 있다. (coroutines_tester_unique_pkey)
                // updateJob에 의해 기본 레코드가 update 가 된 후 (id가 1 -> 99) 에 insertJob이 성공한다.
                // 그래서 maxAttempts 를 1 이상의 충분한 값으로 설정한다.
                maxAttempts = 20

                // INSERT INTO coroutines_tester_unique (id) VALUES (1)
                TesterUnique.insert {
                    it[TesterUnique.id] = originId
                }

                flushCache()
                entityCache.clear()
                TesterUnique.selectAll().count() shouldBeEqualTo 2L  // 1, 99
            }

            // 비동기 방식으로 originId를 가진 레코드를 updateId로 업데이트 한다
            val updateJob = suspendedTransactionAsync(Dispatchers.Default) {
                maxAttempts = 20

                // UPDATE coroutines_tester_unique SET id=99 WHERE coroutines_tester_unique.id = 1
                TesterUnique.update({ TesterUnique.id eq originId }) {
                    it[TesterUnique.id] = updatedId
                }

                flushCache()
                entityCache.clear()

                TesterUnique.selectAll().single()[TesterUnique.id] shouldBeEqualTo updatedId
            }

            insertJob.await()
            updateJob.await()

            val recordCount: Long = newSuspendedTransaction(Dispatchers.Default) {
                TesterUnique.selectAll().count()
            }
            recordCount shouldBeEqualTo 2L

            val ids = TesterUnique.selectAll().orderBy(TesterUnique.id).map { it[TesterUnique.id] }
            ids shouldBeEqualTo listOf(originId, updatedId)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspendedTransactionAsync 를 이용하여 여러 작업을 동시에 수행`(testDB: TestDB) = runSuspendIO {
        withSuspendedTables(testDB, TesterUnique) {
            val originId = 1
            val updatedId = 99

            // 새로운 레코드 생성
            newSuspendedTransaction {
                TesterUnique.insert {
                    it[TesterUnique.id] = originId
                }
                flushCache()
                entityCache.clear()

                TesterUnique.selectAll().single()[TesterUnique.id] shouldBeEqualTo originId
            }

            val (insertResult, updateResult) = listOf(
                // 비동기 방식으로 originId 를 가진 레코드를 삽입한다
                suspendedTransactionAsync(Dispatchers.IO) {
                    // 기존 레코드가 있기 때문에 Unique Constraint 위배 예외가 발생할 수 있다. (coroutines_tester_unique_pkey)
                    // updateJob에 의해 기본 레코드가 update 가 된 후 (id가 1 -> 99) 에 insertJob이 성공한다.
                    // 그래서 maxAttempts 를 1 이상의 충분한 값으로 설정한다.
                    maxAttempts = 20

                    // INSERT INTO coroutines_tester_unique (id) VALUES (1)
                    TesterUnique.insert {
                        it[TesterUnique.id] = originId
                    }
                    TesterUnique.selectAll().count()
                },

                // 비동기 방식으로 originId를 가진 레코드를 updateId로 업데이트 한다
                suspendedTransactionAsync(Dispatchers.Default) {
                    maxAttempts = 20

                    // UPDATE coroutines_tester_unique SET id=99 WHERE coroutines_tester_unique.id = 1
                    TesterUnique.update({ TesterUnique.id eq originId }) {
                        it[TesterUnique.id] = updatedId
                    }
                    TesterUnique.selectAll().count()
                }
            ).awaitAll()

            insertResult shouldBeEqualTo 2L
            updateResult shouldBeEqualTo 1L

            val ids = TesterUnique.selectAll().orderBy(TesterUnique.id).map { it[TesterUnique.id] }
            ids shouldBeEqualTo listOf(originId, updatedId)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중첩된 suspend transaction 실행`(testDB: TestDB) = runSuspendIO {
        suspend fun insertTester(db: Database) = newSuspendedTransaction(db = db) {
            Tester.insert { }
        }

        withSuspendedTables(testDB, Tester, context = Dispatchers.IO) {
            newSuspendedTransaction(db = db) {
                connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
                getTesterById(1).shouldBeNull()
                insertTester(db)
                getTesterById(1)?.getOrNull(Tester.id)?.value shouldBeEqualTo 1
            }

            val result = newSuspendedTransaction(Dispatchers.Default, db = db) {
                getTesterById(1)?.getOrNull(Tester.id)?.value
            }
            result shouldBeEqualTo 1

            getTesterById(1)?.getOrNull(Tester.id)?.value shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중첩된 suspend transaction async 실행`(testDB: TestDB) = runSuspendIO {
        val recordCount = 10

        withSuspendedTables(testDB, Tester, context = Dispatchers.IO) {
            newSuspendedTransaction {
                // 하나의 Connection을 이용하여 시퀀스하게 작업한다.
                repeat(recordCount) {
                    Tester.insert { }
                }
                commit()

                // nested transaction 에서 동시에 여러 개의 작업을 수행한다
                val tasks = List(recordCount) {
                    suspendedTransactionAsync(context = Dispatchers.IO) {
                        Tester.selectAll().toList()
                    }
                }
                val rows = tasks.awaitAll().flatten()
                rows.shouldNotBeEmpty()
            }

            val count = newSuspendedTransaction(Dispatchers.IO) {
                Tester.selectAll().count()
            }
            count shouldBeEqualTo recordCount.toLong()
        }
    }

    /**
     * 복수의 INSERT 작업을 동시에 수행하기
     *
     * Postgres 에 대한 작업 수행 로그
     * ```console
     * 12:16:01.574 DEBUG [-worker-17 @coroutine#53] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.574 DEBUG [r-worker-9 @coroutine#52] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.574 DEBUG [-worker-17 @coroutine#53] e.e.coroutines.Ex01_Coroutines : task[2]: inserted
     * 12:16:01.574 DEBUG [r-worker-9 @coroutine#52] e.e.coroutines.Ex01_Coroutines : task[1]: inserted
     * 12:16:01.579 DEBUG [-worker-21 @coroutine#56] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.579 DEBUG [-worker-18 @coroutine#51] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.579 DEBUG [-worker-18 @coroutine#51] e.e.coroutines.Ex01_Coroutines : task[0]: inserted
     * 12:16:01.579 DEBUG [-worker-21 @coroutine#56] e.e.coroutines.Ex01_Coroutines : task[5]: inserted
     * 12:16:01.585 DEBUG [-worker-19 @coroutine#55] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.585 DEBUG [-worker-20 @coroutine#54] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.585 DEBUG [r-worker-7 @coroutine#60] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.585 DEBUG [-worker-16 @coroutine#59] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.585 DEBUG [r-worker-7 @coroutine#60] e.e.coroutines.Ex01_Coroutines : task[9]: inserted
     * 12:16:01.585 DEBUG [-worker-19 @coroutine#55] e.e.coroutines.Ex01_Coroutines : task[4]: inserted
     * 12:16:01.585 DEBUG [-worker-16 @coroutine#59] e.e.coroutines.Ex01_Coroutines : task[8]: inserted
     * 12:16:01.585 DEBUG [-worker-20 @coroutine#54] e.e.coroutines.Ex01_Coroutines : task[3]: inserted
     * 12:16:01.587 DEBUG [-worker-14 @coroutine#58] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.587 DEBUG [-worker-14 @coroutine#58] e.e.coroutines.Ex01_Coroutines : task[7]: inserted
     * 12:16:01.591 DEBUG [r-worker-4 @coroutine#57] Exposed                        :INSERT INTO coroutines_tester  DEFAULT VALUES
     * 12:16:01.591 DEBUG [r-worker-4 @coroutine#57] e.e.coroutines.Ex01_Coroutines : task[6]: inserted
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `다중의 비동기 작업을 수행 후 대기`(testDB: TestDB) = runSuspendIO {
        val recordCount = 10

        withSuspendedTables(testDB, Tester) {
            // 복수의 INSERT 작업을 동시에 수행합니다.
            val results = List(recordCount) { index ->
                suspendedTransactionAsync(Dispatchers.IO, db = db) {
                    maxAttempts = 5
                    Tester.insert { }
                    log.debug { "task[$index]: inserted" }
                    index + 1
                }
            }.awaitAll()

            results shouldBeEqualTo intRangeOf(1, recordCount)
            Tester.selectAll().count() shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspended 와 일반 transaction 혼합 사용하기`(testDB: TestDB) = runSuspendIO {
        withSuspendedTables(testDB, Tester) {
            val db = this.db
            var suspendedOk = true
            var normalOk = true

            newSuspendedTransaction(db = db) {
                try {
                    Tester.selectAll().toList()
                } catch (e: Throwable) {
                    suspendedOk = false
                }
            }

            transaction(db) {
                try {
                    Tester.selectAll().toList()
                } catch (e: Throwable) {
                    normalOk = false
                }
            }

            suspendedOk.shouldBeTrue()
            normalOk.shouldBeTrue()
        }
    }

    class TesterEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TesterEntity>(Tester)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = "TesterEntity(id=$id)"
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `coroutines with exception within`(testDB: TestDB) = runSuspendIO {
        withSuspendedTables(testDB, Tester) {
            val id = TesterEntity.new { }.id
            commit()

            var innerConn: ExposedConnection<*>? = null

            assertFailsWith<ExposedSQLException> {
                newSuspendedTransaction(context = singleThreadDispatcher, db = db) {
                    innerConn = this.connection
                    innerConn!!.isClosed.shouldBeFalse()
                    // 중복된 ID를 삽입하려고 하면 예외가 발생한다.
                    TesterEntity.new(id.value) { }
                }
            }

            // Nested transaction은 예외가 발생하고, 해당 connection은 닫힌다.
            innerConn.shouldNotBeNull()
            innerConn!!.isClosed.shouldBeTrue()

            // Outer transaction은 아무 문제없이 수행된다.
            TesterEntity.count() shouldBeEqualTo 1L
        }
    }
}
