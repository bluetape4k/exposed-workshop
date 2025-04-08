package exposed.examples.types

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.CharColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
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
         *      "char_bool" CHAR(1) NOT NULL,
         *      "char_bool_default" CHAR(1) DEFAULT 'N' NOT NULL,
         *      "char_bool_nullable" CHAR(1) NULL
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val charBoolean: Column<Boolean> = charBool("char_bool")
            val charBooleanWithDefault: Column<Boolean> = charBool("char_bool_default").default(false)
            val charBooleanNullable: Column<Boolean?> = charBool("char_bool_nullable").nullable()
        }

        withTables(testDB, tester) {
            // INSERT INTO tester (char_bool, char_bool_nullable) VALUES ('Y', NULL)
            tester.insert {
                it[charBoolean] = true
                it[charBooleanNullable] = null
            }

            val row = tester.selectAll().single()
            row[tester.charBoolean].shouldBeTrue()
            row[tester.charBooleanWithDefault].shouldBeFalse()
            row[tester.charBooleanNullable].shouldBeNull()

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE (tester.char_bool = 'Y')
             *    AND (tester.char_bool_default = 'N')
             *    AND (tester.char_bool_nullable IS NULL);
             * ```
             */
            tester
                .select(tester.charBoolean)
                .where { tester.charBoolean eq true }
                .andWhere { tester.charBooleanWithDefault eq false }
                .andWhere { tester.charBooleanNullable.isNull() }
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
        private val characterColumnType: CharColumnType = CharColumnType(1),
    ): ColumnType<Boolean>() {

        override fun sqlType(): String = characterColumnType.sqlType()

        // DB 컬럼 값을 Kotlin Boolean 값으로 변환
        override fun valueFromDB(value: Any): Boolean? {
            return when (characterColumnType.valueFromDB(value).uppercase()) {
                "Y" -> true
                "N" -> false
                else -> null
            }
        }

        // Kotlin Boolean 값을 DB 컬럼 값으로 변환
        override fun valueToDB(value: Boolean?): Any? =
            characterColumnType.valueToDB(value.asChar()?.toString())

        override fun nonNullValueToString(value: Boolean): String =
            characterColumnType.nonNullValueToString(value.asChar()?.toString() ?: "")

        private fun Boolean?.asChar(): Char? = when (this) {
            true -> 'Y'
            false -> 'N'
            else -> null
        }
    }

    /**
     * DB CHAR(1) 컬럼 타입을 `Column<Boolean>` 수형으로 등록하는 함수
     */
    fun Table.charBool(name: String): Column<Boolean> =
        registerColumn(name, CharBooleanColumnType())
}
