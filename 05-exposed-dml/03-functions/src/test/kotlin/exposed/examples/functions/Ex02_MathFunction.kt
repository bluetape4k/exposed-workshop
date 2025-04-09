package exposed.examples.functions

import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.DecimalColumnType
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.div
import org.jetbrains.exposed.sql.decimalLiteral
import org.jetbrains.exposed.sql.doubleLiteral
import org.jetbrains.exposed.sql.functions.math.AbsFunction
import org.jetbrains.exposed.sql.functions.math.CeilingFunction
import org.jetbrains.exposed.sql.functions.math.ExpFunction
import org.jetbrains.exposed.sql.functions.math.FloorFunction
import org.jetbrains.exposed.sql.functions.math.PowerFunction
import org.jetbrains.exposed.sql.functions.math.RoundFunction
import org.jetbrains.exposed.sql.functions.math.SignFunction
import org.jetbrains.exposed.sql.functions.math.SqrtFunction
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.MathContext
import java.sql.SQLException

class Ex02_MathFunction: Ex00_FunctionBase() {

    companion object: KLogging()

    /**
     * [AbsFunction] 함수
     *
     * ```sql
     * SELECT ABS(0) FROM faketable
     * SELECT ABS(100) FROM faketable
     * SELECT ABS(-100) FROM faketable
     * SELECT ABS(100.0) FROM faketable
     * SELECT ABS(-100.0) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `abs function`(testDB: TestDB) {
        withTable(testDB) {
            AbsFunction(intLiteral(0)) shouldExpressionEqualTo 0
            AbsFunction(intLiteral(100)) shouldExpressionEqualTo 100
            AbsFunction(intLiteral(-100)) shouldExpressionEqualTo 100
            AbsFunction(doubleLiteral(100.0)) shouldExpressionEqualTo 100.0
            AbsFunction(doubleLiteral(-100.0)) shouldExpressionEqualTo 100.0
        }
    }

    /**
     * [SignFunction] 함수
     *
     * ```sql
     * SELECT SIGN(0) FROM faketable
     * SELECT SIGN(100) FROM faketable
     * SELECT SIGN(-100) FROM faketable
     * SELECT SIGN(100.0) FROM faketable
     * SELECT SIGN(-100.0) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `sign function`(testDB: TestDB) {
        withTable(testDB) {
            SignFunction(intLiteral(0)) shouldExpressionEqualTo 0
            SignFunction(intLiteral(100)) shouldExpressionEqualTo 1
            SignFunction(intLiteral(-100)) shouldExpressionEqualTo -1
            SignFunction(doubleLiteral(100.0)) shouldExpressionEqualTo 1
            SignFunction(doubleLiteral(-100.0)) shouldExpressionEqualTo -1
        }
    }

    /**
     * [FloorFunction] 함수
     *
     * ```sql
     * SELECT FLOOR(100) FROM faketable
     * SELECT FLOOR(-100) FROM faketable
     * SELECT FLOOR(100.0) FROM faketable
     * SELECT FLOOR(100.3) FROM faketable
     * SELECT FLOOR(100.7) FROM faketable
     * SELECT FLOOR(-100.0) FROM faketable
     * SELECT FLOOR(-100.3) FROM faketable
     * SELECT FLOOR(-100.7) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `floor function`(testDB: TestDB) {
        withTable(testDB) {
            FloorFunction(intLiteral(100)) shouldExpressionEqualTo 100
            FloorFunction(intLiteral(-100)) shouldExpressionEqualTo -100
            FloorFunction(doubleLiteral(100.0)) shouldExpressionEqualTo 100
            FloorFunction(doubleLiteral(100.30)) shouldExpressionEqualTo 100
            FloorFunction(doubleLiteral(100.70)) shouldExpressionEqualTo 100
            FloorFunction(doubleLiteral(-100.0)) shouldExpressionEqualTo -100
            FloorFunction(doubleLiteral(-100.30)) shouldExpressionEqualTo -101
            FloorFunction(doubleLiteral(-100.70)) shouldExpressionEqualTo -101
        }
    }

    /**
     * [CeilingFunction] 함수
     *
     * ```sql
     * SELECT CEILING(100) FROM faketable
     * SELECT CEILING(-100) FROM faketable
     * SELECT CEILING(100.0) FROM faketable
     * SELECT CEILING(100.3) FROM faketable
     * SELECT CEILING(100.7) FROM faketable
     * SELECT CEILING(-100.0) FROM faketable
     * SELECT CEILING(-100.3) FROM faketable
     * SELECT CEILING(-100.7) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ceil function`(testDB: TestDB) {
        withTable(testDB) {
            CeilingFunction(intLiteral(100)) shouldExpressionEqualTo 100
            CeilingFunction(intLiteral(-100)) shouldExpressionEqualTo -100
            CeilingFunction(doubleLiteral(100.0)) shouldExpressionEqualTo 100
            CeilingFunction(doubleLiteral(100.30)) shouldExpressionEqualTo 101
            CeilingFunction(doubleLiteral(100.70)) shouldExpressionEqualTo 101
            CeilingFunction(doubleLiteral(-100.0)) shouldExpressionEqualTo -100
            CeilingFunction(doubleLiteral(-100.30)) shouldExpressionEqualTo -100
            CeilingFunction(doubleLiteral(-100.70)) shouldExpressionEqualTo -100
        }
    }

    /**
     * [PowerFunction] 함수
     *
     * ```sql
     * SELECT POWER(10, 2) FROM faketable
     * SELECT POWER(10, 2.0) FROM faketable
     * SELECT POWER(10.1, 2) FROM faketable
     * SELECT POWER(10.1, 2) FROM faketable
     * SELECT POWER(10.1, 2.0) FROM faketable
     * SELECT POWER(10.1, 2.0) FROM faketable
     * SELECT POWER(10.1, 2.5) FROM faketable
     * SELECT POWER(10.1, 2.5) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `power function`(testDB: TestDB) {
        withTable(testDB) {
            PowerFunction(intLiteral(10), intLiteral(2)) shouldExpressionEqualTo 100.toBigDecimal()
            PowerFunction(intLiteral(10), doubleLiteral(2.0)) shouldExpressionEqualTo 100.toBigDecimal()

            PowerFunction(doubleLiteral(10.1), intLiteral(2)) shouldExpressionEqualTo 102.01.toBigDecimal()
            PowerFunction(decimalLiteral(10.1.toBigDecimal()), intLiteral(2)) shouldExpressionEqualTo
                    102.01.toBigDecimal()

            PowerFunction(doubleLiteral(10.1), doubleLiteral(2.0)) shouldExpressionEqualTo
                    102.01.toBigDecimal()
            PowerFunction(decimalLiteral(BigDecimal("10.1")), doubleLiteral(2.0)) shouldExpressionEqualTo
                    102.01.toBigDecimal()

            PowerFunction(doubleLiteral(10.1), doubleLiteral(2.5)) shouldExpressionEqualTo
                    324.1928515714.toBigDecimal()
            PowerFunction(decimalLiteral(BigDecimal("10.1")), doubleLiteral(2.5)) shouldExpressionEqualTo
                    324.1928515714.toBigDecimal()
        }
    }

    /**
     * [RoundFunction] 함수
     *
     * ```sql
     * SELECT ROUND(10, 0) FROM faketable
     * SELECT ROUND(10, 2) FROM faketable
     * SELECT ROUND(10.455, 0) FROM faketable
     * SELECT ROUND(10.555, 0) FROM faketable
     * SELECT ROUND(10.555, 2) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `round function`(testDB: TestDB) {
        withTable(testDB) {
            RoundFunction(intLiteral(10), 0) shouldExpressionEqualTo 10.toBigDecimal()
            RoundFunction(intLiteral(10), 2) shouldExpressionEqualTo "10.00".toBigDecimal()

            RoundFunction(doubleLiteral(10.455), 0) shouldExpressionEqualTo 10.toBigDecimal()
            RoundFunction(doubleLiteral(10.555), 0) shouldExpressionEqualTo 11.toBigDecimal()

            RoundFunction(doubleLiteral(10.555), 2) shouldExpressionEqualTo "10.56".toBigDecimal()
        }
    }

    /**
     * [SqrtFunction] 함수
     *
     * ```sql
     * SELECT SQRT(100) FROM faketable
     * SELECT SQRT(100.0) FROM faketable
     * SELECT SQRT(125.44) FROM faketable
     * SELECT SQRT(100) FROM faketable
     * SELECT SQRT(125.44) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `sqrt function`(testDB: TestDB) {
        withTable(testDB) {
            SqrtFunction(intLiteral(100)) shouldExpressionEqualTo 10.toBigDecimal()
            SqrtFunction(doubleLiteral(100.0)) shouldExpressionEqualTo 10.toBigDecimal()

            SqrtFunction(doubleLiteral(125.44)) shouldExpressionEqualTo "11.2".toBigDecimal()
            SqrtFunction(decimalLiteral(100.toBigDecimal())) shouldExpressionEqualTo 10.toBigDecimal()

            SqrtFunction(decimalLiteral(BigDecimal("125.44"))) shouldExpressionEqualTo "11.2".toBigDecimal()

            when (testDB) {
                in TestDB.ALL_MYSQL_MARIADB ->
                    SqrtFunction(intLiteral(-100)) shouldExpressionEqualTo null

                else ->
                    expectException<SQLException> {
                        SqrtFunction(intLiteral(-100)) shouldExpressionEqualTo null
                    }
            }
        }
    }

    /**
     * [ExpFunction] 함수
     *
     * ```sql
     * SELECT EXP(1) FROM faketable
     * SELECT EXP(2.5) FROM faketable
     * SELECT EXP(2.5) FROM faketable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exp function`(testDB: TestDB) {
        withTable(testDB) {
            ExpFunction(intLiteral(1)) shouldExpressionEqualTo "2.7182818284590".toBigDecimal()
            ExpFunction(doubleLiteral(2.5)) shouldExpressionEqualTo "12.182493960703473".toBigDecimal()
            ExpFunction(decimalLiteral("2.5".toBigDecimal())) shouldExpressionEqualTo "12.182493960703473".toBigDecimal()
        }
    }

    /**
     * MySQL V8, MariaDB 에서는 컬럼 기본 값에 다른 컬럼을 참조할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Column Reference In Default Expression in MySQL`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB - TestDB.MYSQL_V5 }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS foo (
         *      id INT AUTO_INCREMENT PRIMARY KEY,
         *      `integer` INT NOT NULL,
         *      `double` DOUBLE PRECISION NOT NULL,
         *      `long` BIGINT NOT NULL,
         *      defaultInt INT DEFAULT (ABS(foo.`integer`)) NOT NULL,
         *      defaultInt2 INT DEFAULT ((foo.defaultInt / 100)) NOT NULL,
         *      defaultDecimal DECIMAL(14, 12) DEFAULT (EXP(foo.defaultInt2)) NULL,
         *      defaultLong BIGINT DEFAULT (FLOOR(foo.`double`)) NULL,
         *      defaultDecimal2 DECIMAL(3, 0) DEFAULT (POWER(foo.`long`, 2)) NULL,
         *      defaultDecimal3 DECIMAL(3, 0) DEFAULT (ROUND(foo.`double`, 0)) NULL,
         *      defaultInt3 INT DEFAULT (SIGN(foo.`integer`)) NULL,
         *      defaultDecimal4 DECIMAL(3, 0) DEFAULT (SQRT(foo.defaultDecimal2)) NULL
         * );
         * ```
         */
        val foo = object: IntIdTable("foo") {
            val integer = integer("integer")
            val double = double("double")
            val long = long("long")
            val defaultInt = integer("defaultInt").defaultExpression(AbsFunction(integer))
            val defaultInt2 = integer("defaultInt2").defaultExpression(defaultInt.div(100))
            val defaultDecimal =
                decimal("defaultDecimal", 14, 12).nullable().defaultExpression(ExpFunction(defaultInt2))
            val defaultLong = long("defaultLong").nullable().defaultExpression(FloorFunction(double))
            val defaultDecimal2 =
                decimal("defaultDecimal2", 3, 0).nullable().defaultExpression(PowerFunction(long, intLiteral(2)))
            val defaultDecimal3 =
                decimal("defaultDecimal3", 3, 0).nullable().defaultExpression(RoundFunction(double, 0))
            val defaultInt3 = integer("defaultInt3").nullable().defaultExpression(SignFunction(integer))
            val defaultDecimal4 =
                decimal("defaultDecimal4", 3, 0).nullable().defaultExpression(SqrtFunction(defaultDecimal2))
        }

        // MySQL and MariaDB are the only supported databases that allow referencing another column in a default expression
        // MySQL 5 does not support functions in default values.
        withTables(testDB, foo) {
            val id = foo.insertAndGetId {
                it[foo.integer] = -100
                it[foo.double] = 100.70
                it[foo.long] = 10L
            }
            val result = foo.selectAll().where { foo.id eq id }.single()

            result[foo.defaultInt] shouldBeEqualTo 100
            result[foo.defaultDecimal] shouldBeEqualTo "2.718281828459".toBigDecimal()
            result[foo.defaultLong] shouldBeEqualTo 100L
            result[foo.defaultDecimal2] shouldBeEqualTo 100.toBigDecimal()
            result[foo.defaultDecimal3] shouldBeEqualTo 101.toBigDecimal()
            result[foo.defaultInt3] shouldBeEqualTo -1
            result[foo.defaultDecimal4] shouldBeEqualTo 10.toBigDecimal()
        }
    }

    /**
     * 	Returns e raised to the ln of a specified number
     */
    open class LnFunction<T: Number?>(expression: ExpressionWithColumnType<T>): CustomFunction<BigDecimal?>(
        functionName = "LN",
        columnType = DefaultColumnType,
        expr = arrayOf(expression)
    ) {
        companion object {
            internal val DefaultColumnType = DecimalColumnType(MathContext.DECIMAL64.precision, 20)
        }
    }

    fun <T: Number?> ExpressionWithColumnType<T>.ln(): LnFunction<T> =
        LnFunction(this)

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ln function`(testDB: TestDB) {
        withTable(testDB) {
            LnFunction(ExpFunction(intLiteral(100))) shouldExpressionEqualTo 100.toBigDecimal()
            LnFunction(ExpFunction(doubleLiteral(100.0))) shouldExpressionEqualTo 100.0.toBigDecimal()
            LnFunction(ExpFunction(decimalLiteral(100.0.toBigDecimal()))) shouldExpressionEqualTo 100.0.toBigDecimal()
        }
    }

    /**
     * "LOG" 함수를 표현합니다.
     */
    open class LogFunction<B: Number, T: Number?>(
        base: Expression<B>,
        expression: Expression<T>,
    ): CustomFunction<BigDecimal?>(
        functionName = "LOG",
        columnType = DefaultColumnType,
        expr = arrayOf(base, expression)
    ) {
        companion object {
            internal val DefaultColumnType = DecimalColumnType(MathContext.DECIMAL64.precision, 20)
        }
    }

    fun <B: Number, T: Number?> ExpressionWithColumnType<T>.log(base: Expression<B>): LogFunction<B, T> =
        LogFunction(base, this)

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `log function`(testDB: TestDB) {
        withTable(testDB) {
            LogFunction(intLiteral(10), intLiteral(10)) shouldExpressionEqualTo 1.toBigDecimal()
            LogFunction(intLiteral(10), intLiteral(100)) shouldExpressionEqualTo 2.toBigDecimal()
            LogFunction(doubleLiteral(10.0), doubleLiteral(100.0)) shouldExpressionEqualTo 2.toBigDecimal()
            LogFunction(
                decimalLiteral(10.0.toBigDecimal()),
                decimalLiteral(100.0.toBigDecimal())
            ) shouldExpressionEqualTo 2.toBigDecimal()

            LogFunction(
                decimalLiteral(10.0.toBigDecimal()),
                PowerFunction(doubleLiteral(10.0), doubleLiteral(2.0))
            ) shouldExpressionEqualTo 2.toBigDecimal()
            LogFunction(
                decimalLiteral(10.0.toBigDecimal()),
                PowerFunction(decimalLiteral(10.0.toBigDecimal()), decimalLiteral(2.0.toBigDecimal()))
            ) shouldExpressionEqualTo 2.toBigDecimal()
        }
    }

    /**
     * "LOG10" 함수를 표현합니다. (밑이 10인 로그)
     */
    open class Log10Function<T: Number?>(
        expression: ExpressionWithColumnType<T>,
    ): LogFunction<BigDecimal, T>(
        base = decimalLiteral(10.toBigDecimal()),
        expression = expression
    )

    fun <T: Number?> ExpressionWithColumnType<T>.log10(): Log10Function<T> =
        Log10Function(this)

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `log10 function`(testDB: TestDB) {
        withTable(testDB) {
            Log10Function(intLiteral(10)) shouldExpressionEqualTo 1.toBigDecimal()
            Log10Function(intLiteral(100)) shouldExpressionEqualTo 2.toBigDecimal()
            Log10Function(doubleLiteral(100.0)) shouldExpressionEqualTo 2.toBigDecimal()
            Log10Function(decimalLiteral(100.0.toBigDecimal())) shouldExpressionEqualTo 2.toBigDecimal()

            Log10Function(
                PowerFunction(
                    doubleLiteral(10.0),
                    doubleLiteral(2.0)
                )
            ) shouldExpressionEqualTo 2.toBigDecimal()
            Log10Function(
                PowerFunction(
                    decimalLiteral(10.0.toBigDecimal()),
                    decimalLiteral(2.0.toBigDecimal())
                )
            ) shouldExpressionEqualTo 2.toBigDecimal()
        }
    }
}
