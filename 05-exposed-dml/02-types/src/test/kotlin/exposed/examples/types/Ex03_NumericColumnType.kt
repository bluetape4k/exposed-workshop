package exposed.examples.types

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.assertFailAndRollback
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEndWithIgnoringCase
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.byteParam
import org.jetbrains.exposed.v1.core.decimalParam
import org.jetbrains.exposed.v1.core.doubleParam
import org.jetbrains.exposed.v1.core.floatParam
import org.jetbrains.exposed.v1.core.functions.math.RoundFunction
import org.jetbrains.exposed.v1.core.intParam
import org.jetbrains.exposed.v1.core.longParam
import org.jetbrains.exposed.v1.core.shortParam
import org.jetbrains.exposed.v1.core.ubyteParam
import org.jetbrains.exposed.v1.core.uintParam
import org.jetbrains.exposed.v1.core.ulongParam
import org.jetbrains.exposed.v1.core.ushortParam
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

class Ex03_NumericColumnType: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * [Short] 수형의 허용 범위에서만 작업이 가능합니다. (-32768 ~ 32767)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (short SMALLINT NOT NULL);
     *
     * INSERT INTO tester (short) VALUES (-32768);
     * INSERT INTO tester (short) VALUES (32767);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `short accepts only allowed range`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val short = short("short")
        }
        withTables(testDB, tester) {
            val columnName = tester.short.nameInDatabaseCase()
            val ddlEnding = "($columnName ${tester.short.columnType} NOT NULL)"

            tester.ddl.single().shouldEndWithIgnoringCase(ddlEnding)

            tester.insert { it[short] = Short.MIN_VALUE }
            tester.insert { it[short] = Short.MAX_VALUE }
            tester.select(tester.short).count().toInt() shouldBeEqualTo 2

            // short 수형의 허용범위를 벗어난 값을 지정하면 예외가 발생합니다.
            val tableName = tester.nameInDatabaseCase()
            assertFailAndRollback("Out-of-range error") {
                val outOfRangeValue = Short.MIN_VALUE - 1
                exec("INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)")
            }

            assertFailAndRollback("Out-of-range error") {
                val outOfRangeValue = Short.MAX_VALUE + 1
                exec("INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)")
            }
        }
    }

    /**
     * [byte] 수형의 허용 범위에서만 작업이 가능합니다. (-128 ~ 127)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `byte accepts only allowed range`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      byte SMALLINT NOT NULL,
         *
         *      CONSTRAINT chk_tester_signed_byte_byte
         *          CHECK (byte BETWEEN -128 AND 127)
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val byte = byte("byte")
        }
        withTables(testDB, tester) {
            val columnName = tester.byte.nameInDatabaseCase()
            val ddlEnding = when (testDB) {
                in TestDB.ALL_POSTGRES_LIKE ->
                    "CHECK ($columnName BETWEEN ${Byte.MIN_VALUE} AND ${Byte.MAX_VALUE}))"

                else ->
                    "($columnName ${tester.byte.columnType} NOT NULL)"
            }

            tester.ddl.single().shouldEndWithIgnoringCase(ddlEnding)

            tester.insert { it[byte] = Byte.MIN_VALUE }
            tester.insert { it[byte] = Byte.MAX_VALUE }
            tester.select(tester.byte).count().toInt() shouldBeEqualTo 2

            // byte 수형의 허용범위를 벗어난 값을 지정하면 예외가 발생합니다.
            val tableName = tester.nameInDatabaseCase()
            assertFailAndRollback("Out-of-range error") {
                val outOfRangeValue = Byte.MIN_VALUE - 1
                exec("INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)")
            }

            assertFailAndRollback("Out-of-range error") {
                val outOfRangeValue = Byte.MAX_VALUE + 1
                exec("INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)")
            }
        }
    }

    /**
     * [Int] 수형의 허용 범위에서만 작업이 가능합니다. (-2147483648 ~ 2147483647)
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      integer_column INT NOT NULL
     * );
     *
     * INSERT INTO tester (integer_column) VALUES (-2147483648);
     * INSERT INTO tester (integer_column) VALUES (2147483647);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `integer accepts only allowed range`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val integer = integer("integer_column")
        }
        withTables(testDB, tester) {
            val columnName = tester.integer.nameInDatabaseCase()
            val ddlEnding = "($columnName ${tester.integer.columnType} NOT NULL)"

            tester.ddl.single().shouldEndWithIgnoringCase(ddlEnding)

            tester.insert { it[integer] = Int.MIN_VALUE }
            tester.insert { it[integer] = Int.MAX_VALUE }
            tester.select(tester.integer).count() shouldBeEqualTo 2L

            // int 수형의 허용범위를 넘어가므로, 예외가 발생합니다.
            val tableName = tester.nameInDatabaseCase()
            assertFailAndRollback("Out-of-range error") {
                val outOfRangeValue = Int.MIN_VALUE.toLong() - 1
                exec("INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)")
            }

            assertFailAndRollback("Out-of-range error") {
                val outOfRangeValue = Int.MAX_VALUE.toLong() + 1
                exec("INSERT INTO $tableName ($columnName) VALUES ($outOfRangeValue)")
            }
        }
    }

    /**
     * Numeric 수형에 대해 Parameter 를 사용하여 할당 및 검색할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `numeric params`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      byte_column SMALLINT NOT NULL,
         *      ubyte_column SMALLINT NOT NULL,
         *      short_column SMALLINT NOT NULL,
         *      ushort_column INT NOT NULL,
         *      integer_column INT NOT NULL,
         *      uinteger_column BIGINT NOT NULL,
         *      long_column BIGINT NOT NULL,
         *      ulong_column BIGINT NOT NULL,
         *      float_column REAL NOT NULL,
         *      double_column DOUBLE PRECISION NOT NULL,
         *      decimal_column DECIMAL(6, 3) NOT NULL,
         *
         *      CONSTRAINT chk_tester_signed_byte_byte_column CHECK (byte_column BETWEEN -128 AND 127),
         *      CONSTRAINT chk_tester_unsigned_byte_ubyte_column CHECK (ubyte_column BETWEEN 0 AND 255),
         *      CONSTRAINT chk_tester_unsigned_ushort_column CHECK (ushort_column BETWEEN 0 AND 65535),
         *      CONSTRAINT chk_tester_unsigned_uinteger_column CHECK (uinteger_column BETWEEN 0 AND 4294967295)
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val byte: Column<Byte> = byte("byte_column")
            val ubyte: Column<UByte> = ubyte("ubyte_column")
            val short: Column<Short> = short("short_column")
            val ushort: Column<UShort> = ushort("ushort_column")
            val integer: Column<Int> = integer("integer_column")
            val uinteger: Column<UInt> = uinteger("uinteger_column")
            val long: Column<Long> = long("long_column")
            val ulong: Column<ULong> = ulong("ulong_column")
            val float: Column<Float> = float("float_column")
            val double: Column<Double> = double("double_column")
            val decimal: Column<BigDecimal> = decimal("decimal_column", 6, 3)
        }

        withTables(testDB, tester) {
            tester.insert {
                it[byte] = byteParam(Byte.MIN_VALUE)
                it[ubyte] = ubyteParam(UByte.MAX_VALUE)
                it[short] = shortParam(Short.MIN_VALUE)
                it[ushort] = ushortParam(UShort.MAX_VALUE)
                it[integer] = intParam(Int.MIN_VALUE)
                it[uinteger] = uintParam(UInt.MAX_VALUE)
                it[long] = longParam(Long.MIN_VALUE)
                it[ulong] = ulongParam(Long.MAX_VALUE.toULong())  // ULong.MAX_VALUE is not supported in Postgres
                it[float] = floatParam(3.14159F)
                it[double] = doubleParam(3.1415925435)
                it[decimal] = decimalParam(123.456.toBigDecimal())
            }

            tester.selectAll().where { tester.byte eq byteParam(Byte.MIN_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.ubyte eq ubyteParam(UByte.MAX_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.short eq shortParam(Short.MIN_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.ushort eq ushortParam(UShort.MAX_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.integer eq intParam(Int.MIN_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.uinteger eq uintParam(UInt.MAX_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.long eq longParam(Long.MIN_VALUE) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.ulong eq ulongParam(Long.MAX_VALUE.toULong()) }.count() shouldBeEqualTo 1L


            tester.selectAll().where { tester.double eq doubleParam(3.1415925435) }.count() shouldBeEqualTo 1L
            tester.selectAll().where { tester.decimal eq decimalParam(123.456.toBigDecimal()) }
                .count() shouldBeEqualTo 1L

            // Float 처리 - MySQL은 정확한 값이 아닌 근사치로 비교
            tester.selectAll()
                .where {
                    if (currentDialectTest is MysqlDialect) {
                        RoundFunction(tester.float, 5).eq<Number, BigDecimal, Float>(floatParam(3.14159F))
                    } else {
                        tester.float eq floatParam(3.14159F)
                    }
                }
                .count() shouldBeEqualTo 1L

        }
    }

    /**
     * Numeric 수형 별로 허용 가능한 범위를 알 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testCustomCheckConstraintName(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      byte_column SMALLINT NOT NULL,
         *      ubyte_column SMALLINT NOT NULL,
         *      short_column SMALLINT NOT NULL,
         *      ushort_column INT NOT NULL,
         *      integer_column INT NOT NULL,
         *      uinteger_column BIGINT NOT NULL,
         *
         *      CONSTRAINT custom_byte_check CHECK (byte_column BETWEEN -128 AND 127),
         *      CONSTRAINT custom_ubyte_check CHECK (ubyte_column BETWEEN 0 AND 255),
         *      CONSTRAINT custom_short_check CHECK (short_column BETWEEN -32768 AND 32767),
         *      CONSTRAINT custom_ushort_check CHECK (ushort_column BETWEEN 0 AND 65535),
         *      CONSTRAINT custom_integer_check CHECK (integer_column BETWEEN -2147483648 AND 2147483647),
         *      CONSTRAINT custom_uinteger_check CHECK (uinteger_column BETWEEN 0 AND 4294967295)
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val byte = byte("byte_column", checkConstraintName = "custom_byte_check")
            val ubyte = ubyte("ubyte_column", checkConstraintName = "custom_ubyte_check")
            val short = short("short_column", checkConstraintName = "custom_short_check")
            val ushort = ushort("ushort_column", checkConstraintName = "custom_ushort_check")
            val integer = integer("integer_column", checkConstraintName = "custom_integer_check")
            val uinteger = uinteger("uinteger_column", checkConstraintName = "custom_uinteger_check")
        }

        withTables(testDB, tester) {
            tester.ddl.joinToString() shouldBeEqualTo
                    "CREATE TABLE ${addIfNotExistsIfSupported()}${tester.nameInDatabaseCase()} (" +
                    "${tester.byte.nameInDatabaseCase()} ${tester.byte.columnType} NOT NULL, " +
                    "${tester.ubyte.nameInDatabaseCase()} ${tester.ubyte.columnType} NOT NULL, " +
                    "${tester.short.nameInDatabaseCase()} ${tester.short.columnType} NOT NULL, " +
                    "${tester.ushort.nameInDatabaseCase()} ${tester.ushort.columnType} NOT NULL, " +
                    "${tester.integer.nameInDatabaseCase()} ${tester.integer.columnType} NOT NULL, " +
                    "${tester.uinteger.nameInDatabaseCase()} ${tester.uinteger.columnType} NOT NULL, " +
                    "CONSTRAINT custom_byte_check CHECK (${tester.byte.nameInDatabaseCase()} BETWEEN ${Byte.MIN_VALUE} AND ${Byte.MAX_VALUE}), " +
                    "CONSTRAINT custom_ubyte_check CHECK (${tester.ubyte.nameInDatabaseCase()} BETWEEN 0 AND ${UByte.MAX_VALUE}), " +
                    "CONSTRAINT custom_short_check CHECK (${tester.short.nameInDatabaseCase()} BETWEEN ${Short.MIN_VALUE} AND ${Short.MAX_VALUE}), " +
                    "CONSTRAINT custom_ushort_check CHECK (${tester.ushort.nameInDatabaseCase()} BETWEEN 0 AND ${UShort.MAX_VALUE}), " +
                    "CONSTRAINT custom_integer_check CHECK (${tester.integer.nameInDatabaseCase()} BETWEEN ${Int.MIN_VALUE} AND ${Int.MAX_VALUE}), " +
                    "CONSTRAINT custom_uinteger_check CHECK (${tester.uinteger.nameInDatabaseCase()} BETWEEN 0 AND ${UInt.MAX_VALUE}))"
        }
    }
}
