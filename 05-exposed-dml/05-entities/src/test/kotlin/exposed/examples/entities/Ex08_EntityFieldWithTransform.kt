package exposed.examples.entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldStartWith
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import kotlin.random.Random

/**
 * 엔티티 필드의 수형이나 값을 변환하여 DB 컬럼 수형에 맞게 변환하는 작업에 대한 예
 */
class Ex08_EntityFieldWithTransform: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS trns (
     *      id SERIAL PRIMARY KEY,
     *      "value" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object TransTable: IntIdTable("TRNS") {
        val value = varchar("value", 50)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS null_trns (
     *      id SERIAL PRIMARY KEY,
     *      "value" VARCHAR(50) NULL
     * )
     * ```
     */
    object NullableTransTable: IntIdTable("NULL_TRNS") {
        val value = varchar("value", 50).nullable()
    }

    /**
     * Not Null 컬럼에 대해 `transform` 을 적용한다.
     */
    class TransEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TransEntity>(TransTable)

        var value: String by TransTable.value.transform(
            unwrap = { "transformed-$it" },                               // entity -> db
            wrap = { it.replace("transformed-", "") }   // db -> entity
        )

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    /**
     * Nullable 컬럼에 대해 `transform` 을 적용한다.
     */
    class NullableTransEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<NullableTransEntity>(NullableTransTable)

        var value by NullableTransTable.value.transform(
            unwrap = { it?.run { "transformed-$it" } },                    // entity -> db
            wrap = { it?.replace("transformed-", "") }   // db -> entity
        )

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    /**
     * 엔티티 필드를 DB에 저장 시 `transform` 함수의 `unwrap` 함수를 적용하고,
     * 엔티티 필드를 읽을 때 `transform` 함수의 `wrap` 함수를 적용한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `set and get value`(testDB: TestDB) {
        withTables(testDB, TransTable) {
            // 저장 시 unwrap 함수가 적용된 값이 저장된다.
            // INSERT INTO trns ("value") VALUES ('transformed-stuff')
            val entity = TransEntity.new {
                value = "stuff"
            }
            entity.value shouldBeEqualTo "stuff"

            // 엔티티 로드 시 wrap 함수가 적용된 값이 로드된다.
            val loaded = TransEntity.all().first()
            loaded.value shouldBeEqualTo "stuff"

            // DSL로 DB 컬럼 값 확인
            //  SELECT trns.id, trns."value" FROM trns WHERE TRU
            val row = TransTable.selectAll().first()
            row[TransTable.value] shouldBeEqualTo "transformed-stuff"
        }
    }

    /**
     * 엔티티 필드를 DB에 저장 시 `transform` 함수의 `unwrap` 함수를 적용하고,
     * 엔티티 필드를 읽을 때 `transform` 함수의 `wrap` 함수를 적용한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `set and get nullable value while present`(testDB: TestDB) {
        withTables(testDB, NullableTransTable) {
            // 저장 시 unwrap 함수가 적용된 값이 저장된다.
            // INSERT INTO null_trns ("value") VALUES ('transformed-stuff')
            val entity = NullableTransEntity.new {
                value = "stuff"
            }
            entity.value shouldBeEqualTo "stuff"

            entityCache.clear()

            // 엔티티 로드 시 wrap 함수가 적용된 값이 로드된다.
            // SELECT null_trns.id, null_trns."value" FROM null_trns
            val loaded = NullableTransEntity.all().first()
            loaded.value shouldBeEqualTo "stuff"

            // DSL로 DB 컬럼 값 확인
            // SELECT null_trns.id, null_trns."value" FROM null_trns
            val row = NullableTransTable.selectAll().first()
            row[NullableTransTable.value] shouldBeEqualTo "transformed-stuff"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `set and get nullable value while absent`(testDB: TestDB) {
        withTables(testDB, NullableTransTable) {
            val entity = NullableTransEntity.new {
                value = null
            }
            entity.value.shouldBeNull()

            entityCache.clear()

            val loaded = NullableTransEntity.all().first()
            loaded.value.shouldBeNull()

            val row = NullableTransTable.selectAll().first()
            row[NullableTransTable.value].shouldBeNull()
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tablewithtransformss (
     *      id SERIAL PRIMARY KEY,
     *      "value" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object TableWithTransformss: IntIdTable() {
        // `transform` 함수를 사용하여 DB 컬럼과 엔티티 필드의 수형을 맞춘다.
        val value: Column<BigDecimal> = varchar("value", 50)
            .transform(
                wrap = { it.toBigDecimal() },               // colum -> field
                unwrap = { it.toString() },                 // field -> column
            )
    }

    class TableWithTransform(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TableWithTransform>(TableWithTransformss)

        // `transform` 함수를 사용하여 DB 컬럼 (string) -> DSL 속성 (BigDecimal) -> 엔티티 필드 (Int) 의 수형을 변환한다
        var value: Int by TableWithTransformss.value
            .transform(
                wrap = { it.toInt() },
                unwrap = { it.toBigDecimal() },
            )
    }

    /**
     * SQL DSL 방식에서도 `transform` 함수를 정의하고, 엔티티 필드의 수형을 변환한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Dao transfrom with DSL transform`(testDB: TestDB) {
        withTables(testDB, TableWithTransformss) {

            // INSERT INTO tablewithtransformss ("value") VALUES (10)
            TableWithTransform.new {
                value = 10
            }

            // Correct DAO value (DAO 에서는 Int 이다)
            TableWithTransform.all().first().value shouldBeEqualTo 10

            // Correct DSL value (DSL 에서는 BigDecimal 이다)
            TableWithTransformss.selectAll()
                .first()[TableWithTransformss.value] shouldBeEqualTo 10.toBigDecimal()
        }
    }

    class ChainedTrans(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<ChainedTrans>(TransTable)

        // `transform` 함수를 연쇄적으로 적용한다.
        var value by TransTable.value
            .transform(
                unwrap = { "transformed-$it" },
                wrap = { it.replace("transformed-", "") }
            )
            .transform(
                unwrap = { if (it.length > 5) it.slice(0..4) else it },
                wrap = { it }
            )

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `chained transformation`(testDB: TestDB) {
        withTables(testDB, TransTable) {
            // INSERT INTO trns ("value") VALUES ('transformed-qwert')
            ChainedTrans.new {
                value = "qwertyuiop"
            }

            ChainedTrans.all().first().value shouldBeEqualTo "qwert"

            // DB에 저장된 컬럼 값은 변환된 값이다.
            TransTable.selectAll().first()[TransTable.value] shouldBeEqualTo "transformed-qwert"
        }
    }

    /**
     * `memoizedTransform` 은 한 번 변환된 값을 캐싱하여 재사용한다.
     */
    class MemoizedChainedTrans(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<MemoizedChainedTrans>(TransTable)

        var value by TransTable.value
            .transform(
                unwrap = { "transformed-$it" },                                              // 2 - INSERT
                wrap = { it.replace("transformed-", "") }                   // 3 - SELECT
            )
            .memoizedTransform(
                unwrap = { it + Random.nextInt(0, 100) },                         // 1 - INSERT
                wrap = { it }                                                                // 4 - SELECT
            )

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    /**
     * `memoizedTransform` 은 한 번 변환된 값을 캐싱하여 재사용한다.
     * 이 경우, MemoizedChainedTrans 는 Memoized Unwrapping 을 한 번만 출력해야 한다.
     *
     * ```sql
     * INSERT INTO TRNS ("value") VALUES ('transformed-value#36')
     * ```
     *
     * ```sql
     * SELECT TRNS.ID, TRNS."value" FROM TRNS
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `memoized chained transformation`(testDB: TestDB) {
        withTables(testDB, TransTable) {
            // INSERT INTO trns ("value") VALUES ('transformed-value#75')
            MemoizedChainedTrans.new {
                value = "value#"
            }
            entityCache.clear()

            val entity = MemoizedChainedTrans.all().first()

            val firstRead = entity.value
            log.debug { "entity.value: $firstRead" }  // entity.value: value#75

            firstRead.shouldStartWith("value#")

            // 캐시된된 값을 사용한다.
            entity.value shouldBeEqualTo firstRead

            // 캐시된된 값을 사용한다.
            MemoizedChainedTrans.all().first().value shouldBeEqualTo firstRead
        }
    }
}
