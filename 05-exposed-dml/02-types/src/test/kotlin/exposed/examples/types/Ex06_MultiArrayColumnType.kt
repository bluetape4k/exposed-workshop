package exposed.examples.types

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.arrayLiteral
import org.jetbrains.exposed.sql.arrayParam
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.get
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.slice
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private inline fun <reified T: Any> Table.array3(
    name: String,
    maximumCardinality: List<Int>? = null,
): Column<List<List<List<T>>>> {
    return array<T, List<List<List<T>>>>(name, maximumCardinality, dimensions = 3)
}

private inline fun <reified T: Any> Table.array2(
    name: String,
    maximumCardinality: List<Int>? = null,
): Column<List<List<T>>> {
    return array<T, List<List<T>>>(name, maximumCardinality, dimensions = 2)
}

/**
 * 다차원 배열은 Postgres 만 지원합니다.
 */
class Ex06_MultiArrayColumnType: AbstractExposedTest() {

    companion object: KLogging() {
        private val multiArrayTypeSupportedDb = TestDB.ALL_POSTGRES
    }

    /**
     * 2차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,2,3],[4,5,6],[7,8,9]]);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `2x multi array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }
        withTables(testDB, tester) {
            val list = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))

            val statement = tester.insert {
                it[multiArray] = list
            }
            statement[tester.multiArray] shouldBeEqualTo list

            val value = tester.selectAll().first()[tester.multiArray]
            value shouldBeEqualTo list
        }
    }

    /**
     * 3차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[[1,2],[3,4]],[[5,6],[7,8]]]);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `3x multi array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array3<Int>("multi_array")
        }
        withTables(testDB, tester) {
            val list = listOf(
                listOf(listOf(1, 2), listOf(3, 4)),
                listOf(listOf(5, 6), listOf(7, 8))
            )
            val statement = tester.insert {
                it[multiArray] = list
            }

            statement[tester.multiArray] shouldBeEqualTo list
            // statement[tester.multiArray].flatten().flatten() shouldBeEqualTo list.flatten().flatten()

            val value = tester.selectAll().first()[tester.multiArray]
            value shouldBeEqualTo list
            // value.flatten().flatten() shouldBeEqualTo list.flatten().flatten()
        }
    }

    /**
     * 5차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array TEXT[][][][][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[[[['Hallo','MultiDimensional','Array']]]]]);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `5x multi array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray =
                array<String, List<List<List<List<List<String>>>>>>("multi_array", dimensions = 5)
        }
        withTables(testDB, tester) {
            val list = listOf(listOf(listOf(listOf(listOf("Hallo", "MultiDimensional", "Array")))))

            val statement = tester.insert {
                it[multiArray] = list
            }
            statement[tester.multiArray] shouldBeEqualTo list

            val value = tester.selectAll().first()[tester.multiArray]
            value shouldBeEqualTo list
        }
    }

    /**
     * 2차원 배열을 기본값으로 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] DEFAULT ARRAY[[1,2],[3,4]] NOT NULL
     * );
     *
     * INSERT INTO test_table  DEFAULT VALUES;
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array with default`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val default = listOf(listOf(1, 2), listOf(3, 4))
        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array").default(default)
        }

        val testerDatabaseGenerated = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array").databaseGenerated()
        }

        withTables(testDB, tester) {
            val statement = testerDatabaseGenerated.insert {}
            statement[testerDatabaseGenerated.multiArray].flatten() shouldBeEqualTo default.flatten()

            val value = testerDatabaseGenerated.selectAll().first()[testerDatabaseGenerated.multiArray]
            value shouldBeEqualTo default
            value.flatten() shouldBeEqualTo default.flatten()
        }
    }

    /**
     * 2차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[2][3] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,2,3],[4,5,6]]);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array cardinality`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val list = listOf(listOf(1, 2, 3), listOf(4, 5, 6))

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array", maximumCardinality = listOf(2, 3))
        }

        withTables(testDB, tester) {
            tester.insert {
                it[tester.multiArray] = list
            }

            tester.selectAll().first()[tester.multiArray] shouldBeEqualTo list
            tester.selectAll().first()[tester.multiArray].flatten() shouldBeEqualTo list.flatten()
        }
    }

    /**
     * NULLABLE 2차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (NULL);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table;
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array with nullable`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array").nullable()
        }

        withTables(testDB, tester) {
            val statement = tester.insert {
                it[multiArray] = null
            }
            statement[tester.multiArray].shouldBeNull()
            tester.selectAll().first()[tester.multiArray].shouldBeNull()
        }
    }

    /**
     * 2차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,2],[3,4]]);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array literal`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }

        withTables(testDB, tester) {
            val list = listOf(listOf(1, 2), listOf(3, 4))

            tester.insert {
                it[multiArray] = arrayLiteral<Int, List<List<Int>>>(list, dimensions = 2)
            }

            val value = tester.selectAll().first()[tester.multiArray]
            value shouldBeEqualTo list
            value.flatten() shouldBeEqualTo list.flatten()
        }
    }

    /**
     * 2차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,2],[3,4]]);
     *
     * SELECT test_table.id, test_table.multi_array FROM test_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array param`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }

        withTables(testDB, tester) {
            val list = listOf(listOf(1, 2), listOf(3, 4))

            tester.insert {
                it[multiArray] = arrayParam<Int, List<List<Int>>>(list, dimensions = 2)
            }

            val value = tester.selectAll().first()[tester.multiArray]
            value shouldBeEqualTo list
            value.flatten() shouldBeEqualTo list.flatten()
        }
    }

    /**
     * 2차원 배열을 저장하고 Update 한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * )
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,2],[3,4]]);
     * SELECT test_table.id, test_table.multi_array
     *   FROM test_table WHERE test_table.id = 1;
     *
     * UPDATE test_table SET multi_array=ARRAY[[5,6],[7,8]]
     *  WHERE test_table.id = 1;
     *
     * SELECT test_table.id, test_table.multi_array
     *   FROM test_table
     *  WHERE test_table.id = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array update`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }

        withTables(testDB, tester) {
            val initialArray = listOf(listOf(1, 2), listOf(3, 4))

            val insertedId = tester.insertAndGetId {
                it[multiArray] = initialArray
            }

            var value = tester.selectAll().where { tester.id eq insertedId }.first()[tester.multiArray]
            value shouldBeEqualTo initialArray
            value.flatten() shouldBeEqualTo initialArray.flatten()

            val updatedArray = listOf(listOf(5, 6), listOf(7, 8))

            // Perform update
            tester.update({ tester.id eq insertedId }) {
                it[multiArray] = updatedArray
            }

            value = tester.selectAll().where { tester.id eq insertedId }.first()[tester.multiArray]
            value shouldBeEqualTo updatedArray
            value.flatten() shouldBeEqualTo updatedArray.flatten()
        }
    }

    /**
     * 2차원 배열을 저장하고 Upsert 한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,2],[3,4]]);
     *
     * INSERT INTO test_table (id, multi_array) VALUES (1, ARRAY[[5,6],[7,8]])
     *     ON CONFLICT (id) DO UPDATE SET multi_array=ARRAY[[5,6],[7,8]];
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[5,6],[7,8]])
     *     ON CONFLICT (id) DO UPDATE SET multi_array=EXCLUDED.multi_array;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array upsert`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }

        withTables(testDB, tester) {
            val initialArray = listOf(listOf(1, 2), listOf(3, 4))

            val id = tester.insertAndGetId {
                it[multiArray] = initialArray
            }

            var value = tester.selectAll().where { tester.id eq id }.first()[tester.multiArray]
            value shouldBeEqualTo initialArray
            value.flatten() shouldBeEqualTo initialArray.flatten()

            val updatedArray = listOf(listOf(5, 6), listOf(7, 8))

            // Perform upsert - Update
            tester.upsert(tester.id, onUpdate = { it[tester.multiArray] = updatedArray }) {
                it[tester.id] = id
                it[multiArray] = updatedArray
            }

            value = tester.selectAll().where { tester.id eq id }.first()[tester.multiArray]
            value shouldBeEqualTo updatedArray
            value.flatten() shouldBeEqualTo updatedArray.flatten()

            // Insert
            tester.upsert(tester.id) {
                it[multiArray] = updatedArray
            }
            tester.selectAll().count().toInt() shouldBeEqualTo 2
        }
    }

    /**
     * 2차원 배열을 저장하고 배열의 특정 값만을 이용하여 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,1],[1,4]]);
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,1],[2,4]]);
     * INSERT INTO test_table (multi_array) VALUES (ARRAY[[1,1],[1,6]]);
     *
     * SELECT test_table.id, test_table.multi_array
     *   FROM test_table
     *  WHERE test_table.multi_array[2][2] = 4;
     *
     * SELECT test_table.id, test_table.multi_array
     *   FROM test_table
     *  WHERE test_table.multi_array[2][2] > 10;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array get function`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }

        withTables(testDB, tester) {
            tester.batchInsert(
                listOf(
                    listOf(listOf(1, 1), listOf(1, 4)),
                    listOf(listOf(1, 1), listOf(2, 4)),
                    listOf(listOf(1, 1), listOf(1, 6))
                )
            ) { list ->
                this[tester.multiArray] = list
            }

            val values = tester.selectAll()
                .where { tester.multiArray[2][2] eq 4 }
                .map { it[tester.multiArray] }

            values shouldHaveSize 2
            values shouldBeEqualTo listOf(
                listOf(listOf(1, 1), listOf(1, 4)),
                listOf(listOf(1, 1), listOf(2, 4))
            )

            tester.selectAll()
                .where { tester.multiArray[2][2] greater 10 }
                .map { it[tester.multiArray] }
                .shouldBeEmpty()
        }
    }

    /**
     * 2차원 배열을 저장하고 배열의 특정 범위만 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     *
     * INSERT INTO test_table (multi_array)
     * VALUES (ARRAY[[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]]);
     *
     * SELECT test_table.multi_array[1:2][2:3]
     *   FROM test_table;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array slice function`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        val tester = object: IntIdTable("test_table") {
            val multiArray = array2<Int>("multi_array")
        }

        withTables(testDB, tester) {
            tester.insert {
                it[multiArray] = listOf(
                    listOf(1, 2, 3, 4),
                    listOf(5, 6, 7, 8),
                    listOf(9, 10, 11, 12),
                    listOf(13, 14, 15, 16)
                )
            }

            val alias = tester.multiArray.slice(1, 2).slice(2, 3)
            val query = tester.select(alias).first()
            query[alias] shouldBeEqualTo listOf(listOf(2, 3), listOf(6, 7))
            query[alias].flatten() shouldBeEqualTo listOf(2, 3, 6, 7)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS multiarray (
     *      id SERIAL PRIMARY KEY,
     *      multi_array INT[][] NOT NULL
     * );
     * ```
     */
    object MultiArrayTable: IntIdTable() {
        val multiArray: Column<List<List<Int>>> = array2<Int>("multi_array")
    }

    class MultiArrayEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<MultiArrayEntity>(MultiArrayTable)

        var multiArray: List<List<Int>> by MultiArrayTable.multiArray

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("multiArray", multiArray)
            .toString()
    }

    /**
     * 엔티티를 사용하여 2차원 배열을 저장하고 조회한다. (Postgres 만 지원)
     *
     * ```sql
     * INSERT INTO multiarray (multi_array) VALUES (ARRAY[[1,2],[3,4]])
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array entity create`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        withTables(testDB, MultiArrayTable) {
            val initialArray = listOf(listOf(1, 2), listOf(3, 4))

            val entity = MultiArrayEntity.new {
                this.multiArray = initialArray
            }
            entity.multiArray shouldBeEqualTo initialArray

            flushCache()

            val fetchedList = MultiArrayEntity.findById(entity.id)?.multiArray!!
            fetchedList shouldBeEqualTo initialArray
        }
    }

    /**
     * DAO 방식으로 2차원 배열을 저장하고 Update 한다. (Postgres 만 지원)
     *
     * ```sql
     * -- Postgres
     * INSERT INTO multiarray (multi_array) VALUES (ARRAY[[1,2],[3,4]]);
     *
     * UPDATE multiarray SET multi_array=ARRAY[[5,6],[7,8]] WHERE id = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi array entity update`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in multiArrayTypeSupportedDb }

        withTables(testDB, MultiArrayTable) {
            val initialArray = listOf(listOf(1, 2), listOf(3, 4))

            val entity = MultiArrayEntity.new {
                this.multiArray = initialArray
            }
            entityCache.clear()

            entity.multiArray shouldBeEqualTo initialArray
            entity.multiArray.flatten() shouldBeEqualTo initialArray.flatten()

            val updatedArray = listOf(listOf(5, 6), listOf(7, 8))
            entity.multiArray = updatedArray

            entityCache.clear()

            val fetchedEntity = MultiArrayEntity.findById(entity.id)!!
            fetchedEntity shouldBeEqualTo entity // Same reference
            fetchedEntity.multiArray shouldBeEqualTo updatedArray
        }
    }
}
