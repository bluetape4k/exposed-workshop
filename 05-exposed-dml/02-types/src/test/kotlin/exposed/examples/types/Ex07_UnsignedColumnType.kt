package exposed.examples.types

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.assertFailAndRollback
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Unsigned 수형을 가진 컬럼에 대한 예제
 */
class Ex07_UnsignedColumnType: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS ubyte_table (
     *      ubyte SMALLINT NOT NULL,
     *      CONSTRAINT chk_ubyte_table_unsigned_byte_ubyte
     *          CHECK (ubyte BETWEEN 0 AND 255)
     * )
     * ```
     */
    object UByteTable: Table("ubyte_table") {
        val unsignedByte = ubyte("ubyte")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS ushort_table (
     *      ushort INT NOT NULL,
     *
     *      CONSTRAINT chk_ushort_table_unsigned_ushort
     *          CHECK (ushort BETWEEN 0 AND 65535)
     * )
     * ```
     */
    object UShortTable: Table("ushort_table") {
        val unsignedShort = ushort("ushort")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS uint_table (
     *      uint BIGINT NOT NULL,
     *
     *      CONSTRAINT chk_uint_table_unsigned_uint
     *          CHECK (uint BETWEEN 0 AND 4294967295)
     * )
     * ```
     */
    object UIntTable: Table("uint_table") {
        val unsignedInt = uinteger("uint")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS ulong_table (
     *      ulong BIGINT NOT NULL
     * )
     * ```
     */
    object ULongTable: Table("ulong_table") {
        val unsignedLong = ulong("ulong")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `test UByteColumnType`(testDB: TestDB) {
        withTables(testDB, UByteTable) {
            UByteTable.insert {
                it[unsignedByte] = 123u
            }

            val result = UByteTable.selectAll().toList()
            result shouldHaveSize 1
            result.single()[UByteTable.unsignedByte] shouldBeEqualTo 123u
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testUByteWithCheckConstraint(testDB: TestDB) {
        withTables(testDB, UByteTable) {
            val ddlEnding = when (currentDialectTest) {
                is MysqlDialect -> "(ubyte TINYINT UNSIGNED NOT NULL)"
                is SQLServerDialect -> "(ubyte TINYINT NOT NULL)"
                else -> "CHECK (ubyte BETWEEN 0 and ${UByte.MAX_VALUE}))"
            }
            UByteTable.ddl.single().endsWith(ddlEnding, ignoreCase = true).shouldBeTrue()

            val number = 191.toUByte()
            number.shouldBeInRange(Byte.MAX_VALUE.toUByte(), UByte.MAX_VALUE)

            /**
             * ```sql
             * INSERT INTO ubyte_table (ubyte) VALUES (191)
             * ```
             */
            UByteTable.insert { it[unsignedByte] = number }

            val result = UByteTable.selectAll()
            result.single()[UByteTable.unsignedByte] shouldBeEqualTo number

            // test that column itself blocks same out-of-range value that compiler blocks
            assertFailAndRollback("Check constraint violation (or out-of-range error in MySQL/MariaDB/SQL Server)") {
                val tableName = UByteTable.nameInDatabaseCase()
                val columnName = UByteTable.unsignedByte.nameInDatabaseCase()
                val outOfRangeValue = UByte.MAX_VALUE + 1u
                exec("""INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)""")
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testPreviousUByteColumnTypeWorksWithNewSmallIntType(testDB: TestDB) {
        // MySQL and MariaDB type hasn't changed, and PostgreSQL and Oracle never supported TINYINT
        Assumptions.assumeTrue { testDB in (TestDB.ALL_H2 - TestDB.H2_PSQL) }

        withDb(testDB) {
            try {
                val tableName = UByteTable.nameInDatabaseCase()
                val columnName = UByteTable.unsignedByte.nameInDatabaseCase()
                // create table using previous column type TINYINT
                exec("""CREATE TABLE ${addIfNotExistsIfSupported()}$tableName ($columnName TINYINT NOT NULL)""")

                val number1 = Byte.MAX_VALUE.toUByte()
                UByteTable.insert { it[unsignedByte] = number1 }

                val result1 = UByteTable.selectAll().where { UByteTable.unsignedByte eq number1 }.count()
                result1 shouldBeEqualTo 1

                // TINYINT maps to INTEGER in SQLite, so it will not throw OoR error

                val number2 = (Byte.MAX_VALUE + 1).toUByte()
                assertFailAndRollback("Out-of-range (OoR) error") {
                    UByteTable.insert { it[unsignedByte] = number2 }
                    UByteTable.selectAll().where { UByteTable.unsignedByte less 0u }.count().toInt() shouldBeEqualTo 0
                }

                // modify column to now have SMALLINT type
                exec(UByteTable.unsignedByte.modifyStatement().first())
                UByteTable.insert { it[unsignedByte] = number2 }

                val result2 = UByteTable.selectAll().map { it[UByteTable.unsignedByte] }
                result2 shouldContainSame listOf(number1, number2)
            } finally {
                SchemaUtils.drop(UByteTable)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testUShortColumnType(testDB: TestDB) {
        withTables(testDB, UShortTable) {
            UShortTable.insert {
                it[unsignedShort] = 123u
            }

            val result = UShortTable.selectAll().toList()
            result shouldHaveSize 1
            result.single()[UShortTable.unsignedShort] shouldBeEqualTo 123u
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testUShortWithCheckConstraint(testDB: TestDB) {
        withTables(testDB, UShortTable) {
            val ddlEnding = if (currentDialectTest is MysqlDialect) {
                "(ushort SMALLINT UNSIGNED NOT NULL)"
            } else {
                "CHECK (ushort BETWEEN 0 and ${UShort.MAX_VALUE}))"
            }
            UShortTable.ddl.single().endsWith(ddlEnding, ignoreCase = true).shouldBeTrue()

            val number = 49151.toUShort()
            number.shouldBeInRange(Short.MAX_VALUE.toUShort(), UShort.MAX_VALUE)

            UShortTable.insert { it[unsignedShort] = number }

            val result = UShortTable.selectAll()
            result.single()[UShortTable.unsignedShort] shouldBeEqualTo number

            // test that column itself blocks same out-of-range value that compiler blocks
            assertFailAndRollback("Check constraint violation (or out-of-range error in MySQL/MariaDB)") {
                val tableName = UShortTable.nameInDatabaseCase()
                val columnName = UShortTable.unsignedShort.nameInDatabaseCase()
                val outOfRangeValue = UShort.MAX_VALUE + 1u
                exec("""INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)""")
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testPreviousUShortColumnTypeWorksWithNewIntType(testDB: TestDB) {
        withDb(testDB) {
            try {
                val tableName = UShortTable.nameInDatabaseCase()
                val columnName = UShortTable.unsignedShort.nameInDatabaseCase()
                // create table using previous column type SMALLINT
                exec("""CREATE TABLE ${addIfNotExistsIfSupported()}$tableName ($columnName SMALLINT NOT NULL)""")

                val number1 = Short.MAX_VALUE.toUShort()
                UShortTable.insert { it[unsignedShort] = number1 }

                val result1 = UShortTable.selectAll().where { UShortTable.unsignedShort eq number1 }.count()
                result1 shouldBeEqualTo 1

                // SMALLINT maps to INTEGER in SQLite and NUMBER(38) in Oracle, so they will not throw OoR error

                val number2 = (Short.MAX_VALUE + 1).toUShort()
                assertFailAndRollback("Out-of-range (OoR) error") {
                    UShortTable.insert { it[unsignedShort] = number2 }
                    UShortTable.selectAll().where { UShortTable.unsignedShort less 0u }.count()
                        .toInt() shouldBeEqualTo 0
                }

                // modify column to now have INT type
                /**
                 * ```sql
                 * ALTER TABLE ushort_table
                 *      ALTER COLUMN ushort TYPE INT,
                 *      ALTER COLUMN ushort SET NOT NULL,
                 *      ALTER COLUMN ushort DROP DEFAULT
                 * ```
                 */
                exec(UShortTable.unsignedShort.modifyStatement().first())
                UShortTable.insert { it[unsignedShort] = number2 }

                val result2 = UShortTable.selectAll().map { it[UShortTable.unsignedShort] }
                result2 shouldContainSame listOf(number1, number2)

            } finally {
                SchemaUtils.drop(UShortTable)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testUIntColumnType(testDB: TestDB) {
        withTables(testDB, UIntTable) {
            UIntTable.insert {
                it[unsignedInt] = 123u
            }

            val result = UIntTable.selectAll().toList()
            result shouldHaveSize 1
            result.single()[UIntTable.unsignedInt] shouldBeEqualTo 123u
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testUIntWithCheckConstraint(testDB: TestDB) {
        withTables(testDB, UIntTable) {
            val ddlEnding = if (currentDialectTest is MysqlDialect) {
                "(uint INT UNSIGNED NOT NULL)"
            } else {
                "CHECK (uint BETWEEN 0 and ${UInt.MAX_VALUE}))"
            }
            UIntTable.ddl.single().endsWith(ddlEnding, ignoreCase = true).shouldBeTrue()

            val number = 3_221_225_471u
            number shouldBeInRange Int.MAX_VALUE.toUInt()..UInt.MAX_VALUE

            UIntTable.insert { it[unsignedInt] = number }

            val result = UIntTable.selectAll()
            result.single()[UIntTable.unsignedInt] shouldBeEqualTo number

            // test that column itself blocks same out-of-range value that compiler blocks
            assertFailAndRollback("Check constraint violation (or out-of-range error in MySQL/MariaDB)") {
                val tableName = UIntTable.nameInDatabaseCase()
                val columnName = UIntTable.unsignedInt.nameInDatabaseCase()
                val outOfRangeValue = UInt.MAX_VALUE.toLong() + 1L
                exec("""INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)""")
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testPreviousUIntColumnTypeWorksWithNewBigIntType(testDB: TestDB) {
        // Oracle was already previously constrained to NUMBER(13)
        withDb(testDB) {
            try {
                val tableName = UIntTable.nameInDatabaseCase()
                val columnName = UIntTable.unsignedInt.nameInDatabaseCase()
                // create table using previous column type INT
                exec("""CREATE TABLE ${addIfNotExistsIfSupported()}$tableName ($columnName INT NOT NULL)""")

                val number1 = Int.MAX_VALUE.toUInt()
                UIntTable.insert { it[unsignedInt] = number1 }

                val result1 = UIntTable.selectAll().where { UIntTable.unsignedInt eq number1 }.count()
                result1 shouldBeEqualTo 1

                // INT maps to INTEGER in SQLite, so it will not throw OoR error

                val number2 = Int.MAX_VALUE.toUInt() + 1u
                assertFailAndRollback("Out-of-range (OoR) error") {
                    UIntTable.insert { it[unsignedInt] = number2 }
                    UIntTable.selectAll().where { UIntTable.unsignedInt less 0u }.count().toInt() shouldBeEqualTo 0
                }

                /**
                 * modify column to now have BIGINT type
                 *
                 * ```sql
                 * ALTER TABLE uint_table
                 *      ALTER COLUMN uint TYPE BIGINT,
                 *      ALTER COLUMN uint SET NOT NULL,
                 *      ALTER COLUMN uint DROP DEFAULT
                 * ```
                 */
                exec(UIntTable.unsignedInt.modifyStatement().first())
                UIntTable.insert { it[unsignedInt] = number2 }

                val result2 = UIntTable.selectAll().map { it[UIntTable.unsignedInt] }
                result2 shouldContainSame listOf(number1, number2)

            } finally {
                SchemaUtils.drop(UIntTable)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testULongColumnType(testDB: TestDB) {
        withTables(testDB, ULongTable) {
            ULongTable.insert {
                it[unsignedLong] = 123uL
            }

            val result = ULongTable.selectAll().toList()
            result.size shouldBeEqualTo 1
            result.single()[ULongTable.unsignedLong] shouldBeEqualTo 123uL
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testMaxULongColumnType(testDB: TestDB) {
        val ulongMaxValueUnsupportedDatabases = TestDB.ALL_POSTGRES_LIKE

        withTables(testDB, ULongTable) {
            val maxValue =
                if (testDB in ulongMaxValueUnsupportedDatabases) Long.MAX_VALUE.toULong() else ULong.MAX_VALUE

            ULongTable.insert {
                it[unsignedLong] = maxValue
            }

            val result = ULongTable.selectAll().toList()
            result shouldHaveSize 1
            result.single()[ULongTable.unsignedLong] shouldBeEqualTo maxValue
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testMaxUnsignedTypesInMySql(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_POSTGRES_LIKE }

        withTables(testDB, UByteTable, UShortTable, UIntTable, ULongTable) {
            UByteTable.insert { it[unsignedByte] = UByte.MAX_VALUE }
            UByteTable.selectAll().single()[UByteTable.unsignedByte] shouldBeEqualTo UByte.MAX_VALUE

            UShortTable.insert { it[unsignedShort] = UShort.MAX_VALUE }
            UShortTable.selectAll().single()[UShortTable.unsignedShort] shouldBeEqualTo UShort.MAX_VALUE

            UIntTable.insert { it[unsignedInt] = UInt.MAX_VALUE }
            UIntTable.selectAll().single()[UIntTable.unsignedInt] shouldBeEqualTo UInt.MAX_VALUE

            ULongTable.insert { it[unsignedLong] = ULong.MAX_VALUE }
            ULongTable.selectAll().single()[ULongTable.unsignedLong] shouldBeEqualTo ULong.MAX_VALUE
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testCheckConstraintNameAcrossMultipleTables(testDB: TestDB) {

        val (col1, col2, col3) = listOf("num1", "num2", "num3")

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester_1 (
         *      num1 SMALLINT NOT NULL,
         *      num2 INT NOT NULL,
         *      num3 BIGINT NOT NULL,
         *
         *      CONSTRAINT chk_tester_1_unsigned_byte_num1 CHECK (num1 BETWEEN 0 AND 255),
         *      CONSTRAINT chk_tester_1_unsigned_num2 CHECK (num2 BETWEEN 0 AND 65535),
         *      CONSTRAINT chk_tester_1_unsigned_num3 CHECK (num3 BETWEEN 0 AND 4294967295)
         * )
         * ```
         */
        val tester1 = object: Table("tester_1") {
            val unsigned1 = ubyte(col1)
            val unsigned2 = ushort(col2)
            val unsigned3 = uinteger(col3)
        }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester_2 (
         *      num1 SMALLINT NOT NULL,
         *      num2 INT NOT NULL,
         *      num3 BIGINT NOT NULL,
         *
         *      CONSTRAINT chk_tester_2_unsigned_byte_num1 CHECK (num1 BETWEEN 0 AND 255),
         *      CONSTRAINT chk_tester_2_unsigned_num2 CHECK (num2 BETWEEN 0 AND 65535),
         *      CONSTRAINT chk_tester_2_unsigned_num3 CHECK (num3 BETWEEN 0 AND 4294967295)
         * )
         * ```
         */
        val tester2 = object: Table("tester_2") {
            val unsigned1 = ubyte(col1)
            val unsigned2 = ushort(col2)
            val unsigned3 = uinteger(col3)
        }

        withTables(testDB, tester1, tester2) {
            val (byte, short, integer) = Triple(191.toUByte(), 49151.toUShort(), 3_221_225_471u)

            tester1.insert {
                it[unsigned1] = byte
                it[unsigned2] = short
                it[unsigned3] = integer
            }
            tester2.insert {
                it[unsigned1] = byte
                it[unsigned2] = short
                it[unsigned3] = integer
            }
        }
    }
}
