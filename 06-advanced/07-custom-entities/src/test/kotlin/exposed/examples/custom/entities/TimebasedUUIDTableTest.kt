package exposed.examples.custom.entities

import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

class TimebasedUUIDTableTest: AbstractCustomIdTableTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS t_timebased_uuid (
     *      id uuid PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      age INT NOT NULL
     * )
     * ```
     */
    object T1: TimebasedUUIDTable("t_timebased_uuid") {
        val name = varchar("name", 255)
        val age = integer("age")
    }

    class E1(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<E1>(T1)

        var name by T1.name
        var age by T1.age

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("age", age)
            .toString()
    }

    data class Record(val name: String, val age: Int)

    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource(GET_TESTDB_AND_ENTITY_COUNT)
    fun `TimebasedUUID id를 가진 레코드를 생성한다`(testDB: TestDB, recordCount: Int) {
        withTables(testDB, T1) {
            repeat(recordCount) {
                T1.insert {
                    it[T1.name] = faker.name().fullName()
                    it[T1.age] = Random.nextInt(10, 80)
                }
            }
            entityCache.clear()
            commit()

            T1.selectAll().count() shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource(GET_TESTDB_AND_ENTITY_COUNT)
    fun `TimebasedUUID id를 가진 레코드를 배치로 생성한다`(testDB: TestDB, recordCount: Int) {
        withTables(testDB, T1) {
            val records = List(recordCount) {
                Record(
                    name = faker.name().fullName(),
                    age = Random.nextInt(10, 80)
                )
            }
            records.chunked(100).forEach { chunk ->
                T1.batchInsert(chunk, shouldReturnGeneratedValues = false) {
                    this[T1.name] = it.name
                    this[T1.age] = it.age
                }
                flushCache()
            }
            entityCache.clear()
            commit()

            T1.selectAll().count() shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource(GET_TESTDB_AND_ENTITY_COUNT)
    fun `코루틴 환경에서 레코드를 배치로 생성한다`(testDB: TestDB, recordCount: Int) = runSuspendIO {
        withSuspendedTables(testDB, T1) {
            val records = List(recordCount) {
                Record(
                    name = faker.name().fullName(),
                    age = Random.nextInt(10, 80)
                )
            }

            records.chunked(100).map { chunk ->
                suspendedTransactionAsync(Dispatchers.IO) {
                    T1.batchInsert(chunk, shouldReturnGeneratedValues = false) {
                        this[T1.name] = it.name
                        this[T1.age] = it.age
                    }
                }
            }.awaitAll()

            entityCache.clear()
            commit()

            T1.selectAll().count() shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource(GET_TESTDB_AND_ENTITY_COUNT)
    fun `TimebasedUUID Id를 가진 엔티티를 생성한다`(testDB: TestDB, recordCount: Int) {
        withTables(testDB, T1) {
            List(recordCount) {
                E1.new {
                    name = faker.name().fullName()
                    age = Random.nextInt(10, 80)
                }
            }
            entityCache.clear()
            commit()

            E1.all().count() shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest(name = "{0} - {1}개 레코드")
    @MethodSource(GET_TESTDB_AND_ENTITY_COUNT)
    fun `코루틴 환경에서 엔티티를 생성한다`(testDB: TestDB, recordCount: Int) = runSuspendIO {
        withSuspendedTables(testDB, T1) {
            val tasks: List<Deferred<E1>> = List(recordCount) {
                suspendedTransactionAsync(Dispatchers.IO) {
                    E1.new {
                        name = faker.name().fullName()
                        age = Random.nextInt(10, 80)
                    }
                }
            }
            tasks.awaitAll()
            entityCache.clear()
            commit()

            E1.all().count() shouldBeEqualTo recordCount.toLong()
        }
    }
}
