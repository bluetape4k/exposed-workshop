package exposed.examples.types

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.booleanParam
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Boolean column type 사용 예
 */
class Ex01_BooleanColumnType: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS booleantable (
     *      id SERIAL PRIMARY KEY,
     *      "boolColumn" BOOLEAN NOT NULL
     * )
     * ```
     */
    object BooleanTable: IntIdTable("booleanTable") {
        val boolColumn = bool("boolColumn")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `true value`(testDB: TestDB) {
        withTables(testDB, BooleanTable) {
            val id = BooleanTable.insertAndGetId {
                it[boolColumn] = true
            }

            val result = BooleanTable.selectAll().where { BooleanTable.id eq id }.singleOrNull()
            result?.get(BooleanTable.boolColumn).shouldNotBeNull().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `false value`(testDB: TestDB) {
        withTables(testDB, BooleanTable) {
            val id = BooleanTable.insertAndGetId {
                it[boolColumn] = false
            }

            val result = BooleanTable.selectAll().where { BooleanTable.id eq id }.singleOrNull()
            result?.get(BooleanTable.boolColumn).shouldNotBeNull().shouldBeFalse()
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT booleantable.id, booleantable."boolColumn"
     *   FROM booleantable
     *  WHERE booleantable."boolColumn" = TRUE;
     *
     * SELECT booleantable.id, booleantable."boolColumn"
     *   FROM booleantable
     *  WHERE booleantable."boolColumn" = TRUE;
     *
     * SELECT booleantable.id, booleantable."boolColumn"
     *   FROM booleantable
     *  WHERE booleantable."boolColumn" = FALSE;
     *
     * SELECT booleantable.id, booleantable."boolColumn"
     *   FROM booleantable
     *  WHERE booleantable."boolColumn" = FALSE;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `bool in a condition`(testDB: TestDB) {
        withTables(testDB, BooleanTable) {
            val idTrue = BooleanTable.insertAndGetId {
                it[boolColumn] = true
            }
            val idFalse = BooleanTable.insertAndGetId {
                it[boolColumn] = booleanParam(false)
            }

            BooleanTable
                .selectAll()
                .where { BooleanTable.boolColumn eq true }
                .single()[BooleanTable.id] shouldBeEqualTo idTrue

            BooleanTable
                .selectAll()
                .where { BooleanTable.boolColumn eq booleanParam(true) }
                .single()[BooleanTable.id] shouldBeEqualTo idTrue


            BooleanTable
                .selectAll()
                .where { BooleanTable.boolColumn eq false }
                .single()[BooleanTable.id] shouldBeEqualTo idFalse

            BooleanTable
                .selectAll()
                .where { BooleanTable.boolColumn eq booleanParam(false) }
                .single()[BooleanTable.id] shouldBeEqualTo idFalse
        }
    }

    /**
     * DB CHAR(1) 을 Boolean 수형으로 표현하기 위한 예제 ([CharBooleanColumnType])
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `custom Char Boolean Column Type`(testDB: TestDB) {

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      "charBooleanColumn" VARCHAR(1) NOT NULL,
         *      "charBooleanColumnWithDefault" VARCHAR(1) DEFAULT 'N' NOT NULL
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val charBooleanColumn = charBoolean("charBooleanColumn")
            val charBooleanColumnWithDefault = charBoolean("charBooleanColumnWithDefault")
                .default(false)
        }

        withTables(testDB, tester) {
            // INSERT INTO tester ("charBooleanColumn") VALUES ('Y')
            tester.insert {
                it[charBooleanColumn] = true
            }

            tester.selectAll().single()[tester.charBooleanColumn].shouldBeTrue()

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE (tester."charBooleanColumn" = 'Y')
             *    AND (tester."charBooleanColumnWithDefault" = 'N')
             * ```
             */
            tester
                .select(tester.charBooleanColumn)
                .where { tester.charBooleanColumn eq true }
                .andWhere { tester.charBooleanColumnWithDefault eq false }
                .count() shouldBeEqualTo 1
        }
    }

    /**
     * Boolean 을 표현하는 DB CHAR(1) 컬럼 타입
     *
     * * 'Y' -> true
     * * else -> false ('N')
     */
    class CharBooleanColumnType(
        private val characterColumnType: VarCharColumnType = VarCharColumnType(1),
    ): ColumnType<Boolean>() {
        override fun sqlType(): String = characterColumnType.preciseType()

        override fun valueFromDB(value: Any): Boolean =
            when (characterColumnType.valueFromDB(value).uppercase()) {
                "Y" -> true
                else -> false
            }

        override fun valueToDB(value: Boolean?): Any? =
            characterColumnType.valueToDB(value.toChar().toString())

        override fun nonNullValueToString(value: Boolean): String =
            characterColumnType.nonNullValueToString(value.toChar().toString())

        private fun Boolean?.toChar() = when (this) {
            true -> 'Y'
            false -> 'N'
            else -> ' '
        }
    }

    /**
     * DB CHAR(1) 컬럼 타입을 `Column<Boolean>` 수형으로 등록하는 함수
     */
    fun Table.charBoolean(name: String): Column<Boolean> =
        registerColumn(name, CharBooleanColumnType())
}
