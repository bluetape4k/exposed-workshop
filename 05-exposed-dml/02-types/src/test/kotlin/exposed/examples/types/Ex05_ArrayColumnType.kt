package exposed.examples.types

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.allFrom
import org.jetbrains.exposed.v1.core.anyFrom
import org.jetbrains.exposed.v1.core.arrayParam
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.get
import org.jetbrains.exposed.v1.core.slice
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex05_ArrayColumnType: JdbcExposedTestBase() {
    companion object: KLogging()

    /**
     * ARRAY 수형은 Postgres, H2 에서만 지원된다.
     */
    private val arrayTypeSupportedDB = TestDB.ALL_POSTGRES_LIKE + TestDB.H2

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS array_test_table (
     *      id SERIAL PRIMARY KEY,
     *      numbers INT[] DEFAULT ARRAY[5] NOT NULL,
     *      strings TEXT[] DEFAULT ARRAY[]::text[] NOT NULL,
     *      doubles DOUBLE PRECISION[] NULL,
     *      byte_array bytea[] NULL
     * )
     * ```
     */
    object ArrayTestTable: IntIdTable("array_test_table") {
        val numbers: Column<List<Int>> = array<Int>("numbers").default(listOf(5))
        val strings = array<String?>("strings", TextColumnType()).default(emptyList())
        val floats: Column<List<Float>?> = array<Float>("floats").nullable()
        val doubles: Column<List<Double>?> = array<Double>("doubles").nullable()
        val byteArray: Column<List<ByteArray>?> = array("byte_array", BinaryColumnType(32)).nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create and drop array columns`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in arrayTypeSupportedDB }

        withDb(testDB) {
            try {
                SchemaUtils.create(ArrayTestTable)
                ArrayTestTable.exists().shouldBeTrue()
            } finally {
                SchemaUtils.drop(ArrayTestTable)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create missing columns with array defaults`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            try {
                SchemaUtils.createMissingTablesAndColumns(ArrayTestTable)
                SchemaUtils.statementsRequiredToActualizeScheme(ArrayTestTable).shouldBeEmpty()
            } finally {
                SchemaUtils.drop(ArrayTestTable)
            }
        }
    }

    @Disabled("array columns 을 logging 하면 예외가 발생한다. 실제 작동에는 문제가 없다")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array column insert and select`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            val numInput = listOf(1, 2, 3)
            val stringInput = listOf<String?>("hi", "hey", "hello")
            val doubleInput = listOf(1.0, 2.0, 3.0)

            val id1 = ArrayTestTable.insertAndGetId {
                it[ArrayTestTable.numbers] = numInput
                it[ArrayTestTable.strings] = stringInput
                it[ArrayTestTable.doubles] = doubleInput
            }

            val result1 = ArrayTestTable.selectAll().where { ArrayTestTable.id eq id1 }.single()
            result1[ArrayTestTable.numbers] shouldBeEqualTo numInput
            result1[ArrayTestTable.strings] shouldBeEqualTo stringInput
            result1[ArrayTestTable.doubles] shouldBeEqualTo doubleInput

            val id2 = ArrayTestTable.insertAndGetId {
                it[ArrayTestTable.numbers] = emptyList()
                it[ArrayTestTable.strings] = emptyList()
                it[ArrayTestTable.doubles] = emptyList()
            }

            val result2 = ArrayTestTable.selectAll().where { ArrayTestTable.id eq id2 }.single()
            result2[ArrayTestTable.numbers].shouldBeEmpty()
            result2[ArrayTestTable.strings].shouldBeEmpty()
            result2[ArrayTestTable.doubles]?.shouldBeEmpty()

            val id3 = ArrayTestTable.insertAndGetId {
                it[ArrayTestTable.numbers] = emptyList()
                it[ArrayTestTable.strings] = listOf(null, null, null, "null")
                it[ArrayTestTable.doubles] = null
            }

            val result3 = ArrayTestTable.selectAll().where { ArrayTestTable.id eq id3 }.single()
            result3[ArrayTestTable.numbers].single() shouldBeEqualTo 5
            result3[ArrayTestTable.strings].take(3).all { it == null }.shouldBeTrue()
            result3[ArrayTestTable.strings].last() shouldBeEqualTo "null"
            result3[ArrayTestTable.doubles].shouldBeNull()
        }
    }

    /**
     * PostgreSQL 은 array column 의 max cardinality 를 무시한다.
     *
     * ```sql
     * -- Postgres
     * INSERT INTO sized_test_table (numbers) VALUES (ARRAY[1,2,3,4,5,6]);
     *
     * SELECT sized_test_table.numbers
     *   FROM sized_test_table;
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array max size`(testDB: TestDB) {
        val maxArraySize = 5

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS sized_test_table (
         *      numbers INT[5] DEFAULT ARRAY[]::int[] NOT NULL
         * )
         * ```
         */
        val sizedTester = object: Table("sized_test_table") {
            val numbers: Column<List<Int>> = array("numbers", IntegerColumnType(), maxArraySize).default(emptyList())
        }

        withArrayTestTable(testDB, sizedTester) {
            val tooLongList = List(maxArraySize + 1) { it + 1 }
            if (currentDialect is PostgreSQLDialect) {
                // PostgreSQL ignores any max cardinality value
                sizedTester.insert {
                    it[numbers] = tooLongList
                }

                val result = sizedTester.selectAll().single()[sizedTester.numbers]
                result shouldBeEqualTo tooLongList
            } else {
                expectException<ExposedSQLException> {
                    sizedTester.insert {
                        it[numbers] = tooLongList
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select using array get`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            val numInput = listOf(1, 2, 3)
            ArrayTestTable.insert {
                it[numbers] = numInput
                it[strings] = listOf<String?>("hi", "hello")
                it[doubles] = null
            }

            /**
             * SQL array indexes are one-based
             *
             * ```sql
             * SELECT array_test_table.numbers[2] FROM array_test_table
             * ```
             */
            val secondNumber = ArrayTestTable.numbers[2]
            val result1 = ArrayTestTable.select(secondNumber).single()[secondNumber]
            result1 shouldBeEqualTo numInput[1]

            /**
             * ```sql
             * SELECT array_test_table.id,
             *        array_test_table.numbers,
             *        array_test_table.strings,
             *        array_test_table.doubles,
             *        array_test_table.byte_array
             *   FROM array_test_table
             *  WHERE array_test_table.strings[2] = 'hello'
             * ```
             */
            val result2 = ArrayTestTable
                .selectAll()
                .where { ArrayTestTable.strings[2] eq "hello" }
                .single()
            result2[ArrayTestTable.strings] shouldBeEqualTo listOf("hi", "hello")
            result2[ArrayTestTable.doubles].shouldBeNull()

            /**
             * ```sql
             * SELECT array_test_table.id,
             *        array_test_table.numbers,
             *        array_test_table.strings,
             *        array_test_table.doubles,
             *        array_test_table.byte_array
             *   FROM array_test_table
             *  WHERE array_test_table.numbers[1] >= array_test_table.numbers[3]
             * ```
             */
            val result3 = ArrayTestTable
                .selectAll()
                .where { ArrayTestTable.numbers[1] greaterEq ArrayTestTable.numbers[3] }
                .toList()
            result3.shouldBeEmpty()

            /**
             * ```sql
             * SELECT array_test_table.doubles[2]
             *   FROM array_test_table
             * ```
             */
            val nullArray = ArrayTestTable.doubles[2]
            val result4 = ArrayTestTable.select(nullArray).single()[nullArray]
            result4.shouldBeNull()
        }
    }

    @Disabled("array columns 을 logging 하면 예외가 발생한다. 실제 작동에는 문제가 없다")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select using array slice`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            val numInput = listOf(1, 2, 3)
            ArrayTestTable.insert {
                it[numbers] = numInput
                it[strings] = listOf<String?>(null, null, null, "hello")
                it[doubles] = null
            }

            val lastTwoNumbers = ArrayTestTable.numbers.slice(2, 3)  // numbers[2:3]
            val result1 = ArrayTestTable.select(lastTwoNumbers).single()[lastTwoNumbers]
            result1 shouldBeEqualTo numInput.takeLast(2)

            val firstThreeStrings = ArrayTestTable.strings.slice(upper = 3) // strings[:3]
            val result2 = ArrayTestTable.select(firstThreeStrings).single()[firstThreeStrings]
            if (currentDialect is H2Dialect) {
                result2.shouldBeNull()
            } else {
                result2.filterNotNull().shouldBeEmpty()
            }

            val allNumbers = ArrayTestTable.numbers.slice()  // numbers[:]
            val result3 = ArrayTestTable.select(allNumbers).single()[allNumbers]
            if (currentDialect is H2Dialect) {
                result3.shouldBeNull()
            } else {
                result3 shouldBeEqualTo numInput
            }

            val nullArray = ArrayTestTable.doubles.slice(1, 3)
            val result4 = ArrayTestTable.select(nullArray).single()[nullArray]
            result4.shouldBeNull()
        }
    }

    /**
     * [arrayParam] 을 사용하여 array column 을 비교 할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array literal and array param`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            /**
             * ```sql
             * -- Postgres
             * INSERT INTO array_test_table (numbers, strings, doubles)
             * VALUES (ARRAY[1,2,3], ARRAY['','','','hello'], ARRAY[1.0,2.0,3.0,4.0,5.0])
             * ```
             */
            val numInput = listOf(1, 2, 3)
            val doublesInput = List(5) { (it + 1).toDouble() }
            val id1 = ArrayTestTable.insertAndGetId {
                it[numbers] = numInput
                it[strings] = listOf("", "", "", "hello") // listOf<String?>(null, null, null, "hello")
                it[doubles] = doublesInput
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT array_test_table.id,
             *        array_test_table.numbers,
             *        array_test_table.strings,
             *        array_test_table.doubles,
             *        array_test_table.byte_array
             *   FROM array_test_table
             *  WHERE (array_test_table.numbers = ARRAY[1,2,3])
             *    AND (array_test_table.strings <> ARRAY[])
             * ```
             */
            val result1 = ArrayTestTable.selectAll()
                .andWhere { ArrayTestTable.numbers eq numInput }
                .andWhere { ArrayTestTable.strings neq emptyList() }
            result1.single()[ArrayTestTable.id] shouldBeEqualTo id1

            /**
             * ```sql
             * SELECT array_test_table.id
             *   FROM array_test_table
             *  WHERE array_test_table.doubles = ARRAY[1.0,2.0,3.0,4.0,5.0]
             * ```
             */
            val result2 = ArrayTestTable
                .select(ArrayTestTable.id)
                .where { ArrayTestTable.doubles eq arrayParam(doublesInput) }

            result2.single()[ArrayTestTable.id] shouldBeEqualTo id1

            if (currentDialectTest is PostgreSQLDialect) {
                /**
                 * ```sql
                 * -- Postgres
                 * SELECT array_test_table.id
                 *   FROM array_test_table
                 *  WHERE array_test_table.strings[4:] = ARRAY['hello']
                 * ```
                 */
                val lastStrings = ArrayTestTable.strings.slice(lower = 4) // strings[4:]
                val result3 = ArrayTestTable
                    .select(ArrayTestTable.id)
                    .where { lastStrings eq arrayParam(listOf("hello")) }
                result3.single()[ArrayTestTable.id] shouldBeEqualTo id1
            }
        }
    }

    /**
     * [update] for array columns
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array column update`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            val id1 = ArrayTestTable.insertAndGetId {
                it[doubles] = null
            }
            ArrayTestTable.selectAll().single()[ArrayTestTable.doubles].shouldBeNull()

            /**
             * ```sql
             * -- Postgres
             * UPDATE array_test_table
             *    SET doubles=ARRAY[9.0]
             *  WHERE array_test_table.id = 1
             * ```
             */
            val updatedDoubles = listOf(9.0)
            ArrayTestTable.update({ ArrayTestTable.id eq id1 }) {
                it[doubles] = updatedDoubles
            }
            ArrayTestTable.selectAll().single()[ArrayTestTable.doubles] shouldBeEqualTo updatedDoubles
        }
    }

    /**
     * [upsert] for array columns
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array column upsert`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            val numbers = listOf(1, 2, 3)
            val strings = listOf("A", "B")

            /**
             * ```sql
             * INSERT INTO array_test_table (numbers, strings)
             * VALUES (ARRAY[1,2,3], ARRAY['A','B'])
             * ```
             */
            val id1 = ArrayTestTable.insertAndGetId {
                it[ArrayTestTable.numbers] = numbers
                it[ArrayTestTable.strings] = strings
            }

            val result = ArrayTestTable.selectAll().single()
            result[ArrayTestTable.id] shouldBeEqualTo id1
            result[ArrayTestTable.numbers] shouldBeEqualTo numbers
            result[ArrayTestTable.strings] shouldBeEqualTo strings

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO array_test_table (id, numbers, strings)
             * VALUES (1, ARRAY[1,2,3], ARRAY['A','B'])
             * ON CONFLICT (id) DO
             *      UPDATE SET strings=ARRAY['C','D','E']
             * ```
             */
            val updatedString = listOf("C", "D", "E")
            ArrayTestTable.upsert(
                onUpdate = { it[ArrayTestTable.strings] = updatedString },
            ) {
                it[id] = id1
                it[ArrayTestTable.numbers] = numbers
                it[ArrayTestTable.strings] = strings
            }

            val result2 = ArrayTestTable.selectAll().single()
            result2[ArrayTestTable.id] shouldBeEqualTo id1
            result2[ArrayTestTable.numbers] shouldBeEqualTo numbers
            result2[ArrayTestTable.strings] shouldBeEqualTo updatedString
        }
    }

    class ArrayTestEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<ArrayTestEntity>(ArrayTestTable)

        var numbers: List<Int> by ArrayTestTable.numbers
        var strings: List<String?> by ArrayTestTable.strings
        var doubles: List<Double>? by ArrayTestTable.doubles
        var byteArray: List<ByteArray>? by ArrayTestTable.byteArray

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("numbers", numbers)
            .add("strings", strings)
            .add("doubles", doubles)
            .add("byteArray", byteArray)
            .toString()
    }

    /**
     * array column 을 가진 엔티티 [ArrayTestEntity] 를 사용하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array column with DAO functions`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            /**
             * ```sql
             * -- Postgres
             * INSERT INTO array_test_table (numbers, strings)
             * VALUES (ARRAY[1,2,3], ARRAY[])
             * ```
             */
            val numInput = listOf(1, 2, 3)
            val entity1 = ArrayTestEntity.new {
                numbers = numInput
                doubles = null
            }

            entityCache.clear()

            entity1.numbers shouldBeEqualTo numInput
            entity1.strings.shouldBeEmpty()

            /**
             * ```sql
             * -- Postgres
             * UPDATE array_test_table
             *    SET doubles=ARRAY[9.0]
             *  WHERE id = 1
             * ```
             */
            val doublesInput = listOf(9.0)
            entity1.doubles = doublesInput

            entityCache.clear()

            ArrayTestEntity.findById(entity1.id)?.doubles shouldBeEqualTo doublesInput
        }
    }

    /**
     * [anyFrom], [allFrom] 함수를 이용하여 array column 을 비교할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `array column with all any ops`(testDB: TestDB) {
        withArrayTestTable(testDB) {
            /**
             * ```sql
             * INSERT INTO array_test_table (numbers, doubles) VALUES (ARRAY[1,2,3], NULL)
             * ```
             */
            val numInput = listOf(1, 2, 3)
            val id1 = ArrayTestTable.insertAndGetId {
                it[numbers] = numInput
                it[doubles] = null
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT array_test_table.id
             *   FROM array_test_table
             *  WHERE array_test_table.id = ANY (array_test_table.numbers)
             * ```
             */
            val result1 = ArrayTestTable.select(ArrayTestTable.id)
                .where { ArrayTestTable.id eq anyFrom(ArrayTestTable.numbers) }
            result1.single()[ArrayTestTable.id] shouldBeEqualTo id1

            /**
             * ```sql
             * -- Postgres
             * SELECT array_test_table.id
             *   FROM array_test_table
             *  WHERE array_test_table.id = ANY (array_test_table.numbers[2:3])
             * ```
             */
            val result2 = ArrayTestTable.select(ArrayTestTable.id)
                .where { ArrayTestTable.id eq anyFrom(ArrayTestTable.numbers.slice(2, 3)) }
            result2.toList().shouldBeEmpty()

            /**
             * ```sql
             * SELECT array_test_table.id
             *   FROM array_test_table
             *  WHERE array_test_table.id <= ALL (array_test_table.numbers)
             * ```
             */
            val result3 = ArrayTestTable.select(ArrayTestTable.id)
                .where { ArrayTestTable.id lessEq allFrom(ArrayTestTable.numbers) }
            result3.single()[ArrayTestTable.id] shouldBeEqualTo id1

            /**
             * ```sql
             * SELECT array_test_table.id
             *   FROM array_test_table
             *  WHERE array_test_table.id > ALL (array_test_table.numbers)
             * ```
             */
            val result4 = ArrayTestTable.select(ArrayTestTable.id)
                .where { ArrayTestTable.id greater allFrom(ArrayTestTable.numbers) }
            result4.toList().shouldBeEmpty()
        }
    }

    /**
     * [ByteArray] array column 을 가진 테이블에 [ByteArray] array 를 insert 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert array of byte arrays`(testDB: TestDB) {
        // POSTGRESQLNG is excluded because the problem may be on their side.
        // Related issue: https://github.com/impossibl/pgjdbc-ng/issues/600
        // Recheck on our side when the issue is resolved.
        Assumptions.assumeTrue { testDB in (arrayTypeSupportedDB - TestDB.POSTGRESQLNG) }
        withArrayTestTable(testDB) {

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO array_test_table (byte_array) VALUES (ARRAY[ ,])
             * ```
             */
            val testByteArrayList = listOf(byteArrayOf(0), byteArrayOf(1, 2, 3))
            ArrayTestTable.insert {
                it[byteArray] = testByteArrayList
            }

            val result = ArrayTestTable.selectAll().single()[ArrayTestTable.byteArray]
            result.shouldNotBeNull()
            result[0][0] shouldBeEqualTo testByteArrayList[0][0]
            result[1].toUByteString() shouldBeEqualTo testByteArrayList[1].toUByteString()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `aliased array`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS test_aliased_array (
         *      id SERIAL PRIMARY KEY,
         *      "value" INT[] NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("test_aliased_array") {
            val value = array<Int>("value")
        }

        val inputInts = listOf(1, 2, 3)

        withArrayTestTable(testDB, tester) {
            /**
             * ```sql
             * -- Postgres
             * INSERT INTO test_aliased_array ("value") VALUES (ARRAY[1,2,3])
             * ```
             */
            tester.insert {
                it[tester.value] = inputInts
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT test_aliased_array."value" aliased_value
             *   FROM test_aliased_array
             * ```
             */
            val alias = tester.value.alias("aliased_value")
            tester.select(alias).first()[alias] shouldBeEqualTo inputInts
        }
    }

    private fun withArrayTestTable(
        testDB: TestDB,
        vararg tables: Table = arrayOf(ArrayTestTable),
        statement: JdbcTransaction.(TestDB) -> Unit,
    ) {
        Assumptions.assumeTrue { testDB in arrayTypeSupportedDB }

        withTables(testDB, *tables) {
            statement(testDB)
        }
    }
}

private fun ByteArray.toUByteString(): String = joinToString { it.toUByte().toString() }
