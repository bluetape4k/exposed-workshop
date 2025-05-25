package exposed.examples.functions

import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.decimalLiteral
import org.jetbrains.exposed.v1.core.doubleLiteral
import org.jetbrains.exposed.v1.core.functions.math.ACosFunction
import org.jetbrains.exposed.v1.core.functions.math.ASinFunction
import org.jetbrains.exposed.v1.core.functions.math.ATanFunction
import org.jetbrains.exposed.v1.core.functions.math.CosFunction
import org.jetbrains.exposed.v1.core.functions.math.CotFunction
import org.jetbrains.exposed.v1.core.functions.math.DegreesFunction
import org.jetbrains.exposed.v1.core.functions.math.PiFunction
import org.jetbrains.exposed.v1.core.functions.math.RadiansFunction
import org.jetbrains.exposed.v1.core.functions.math.SinFunction
import org.jetbrains.exposed.v1.core.functions.math.TanFunction
import org.jetbrains.exposed.v1.core.intLiteral
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

/**
 * SQL 삼각함수 사용 예
 */
class Ex04_TrigonometricalFunction: Ex00_FunctionBase() {

    companion object: KLogging()

    /**
     * ```sql
     * SELECT ACOS(0)
     * SELECT ACOS(1)
     * SELECT ACOS(0.25)
     * SELECT ACOS(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ACosFunction test`(testDB: TestDB) {
        withTable(testDB) {
            ACosFunction(intLiteral(0)) shouldExpressionEqualTo "1.5707963".toBigDecimal()
            ACosFunction(intLiteral(1)) shouldExpressionEqualTo "0".toBigDecimal()
            ACosFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "1.3181161".toBigDecimal()
            ACosFunction(decimalLiteral("0.25".toBigDecimal())) shouldExpressionEqualTo "1.3181161".toBigDecimal()
        }
    }

    /**
     * [ASinFunction]
     *
     * ```sql
     * SELECT ASIN(0)
     * SELECT ASIN(1)
     * SELECT ASIN(0.25)
     * SELECT ASIN(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ASinFunction test`(testDB: TestDB) {
        withTable(testDB) {
            ASinFunction(intLiteral(0)) shouldExpressionEqualTo "0".toBigDecimal()
            ASinFunction(intLiteral(1)) shouldExpressionEqualTo "1.5707963".toBigDecimal()
            ASinFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "0.252680255".toBigDecimal()
            ASinFunction(decimalLiteral("0.25".toBigDecimal())) shouldExpressionEqualTo "0.252680255".toBigDecimal()
        }
    }

    /**
     * [ATanFunction]
     *
     * ```sql
     * SELECT ATAN(0)
     * SELECT ATAN(1)
     * SELECT ATAN(0.25)
     * SELECT ATAN(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ATanFunction test`(testDB: TestDB) {
        withTable(testDB) {
            ATanFunction(intLiteral(0)) shouldExpressionEqualTo "0".toBigDecimal()
            ATanFunction(intLiteral(1)) shouldExpressionEqualTo "0.785398163".toBigDecimal()
            ATanFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "0.244978663".toBigDecimal()
            ATanFunction(decimalLiteral("0.25".toBigDecimal())) shouldExpressionEqualTo "0.244978663".toBigDecimal()
        }
    }

    /**
     * [CosFunction]
     *
     * ```sql
     * SELECT COS(0)
     * SELECT COS(1)
     * SELECT COS(0.26)
     * SELECT COS(0.26)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `CosFunction test`(testDB: TestDB) {
        withTable(testDB) {
            CosFunction(intLiteral(0)) shouldExpressionEqualTo "1".toBigDecimal()
            CosFunction(intLiteral(1)) shouldExpressionEqualTo "0.5403023".toBigDecimal()
            CosFunction(doubleLiteral(0.26)) shouldExpressionEqualTo "0.96638998".toBigDecimal()
            CosFunction(decimalLiteral("0.26".toBigDecimal())) shouldExpressionEqualTo "0.96638998".toBigDecimal()
        }
    }

    /**
     * [CotFunction]
     *
     * ```sql
     * SELECT COT(1)
     * SELECT COT(0.25)
     * SELECT COT(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `CotFunction test`(testDB: TestDB) {
        withTable(testDB) {
            CotFunction(intLiteral(1)) shouldExpressionEqualTo "0.642092616".toBigDecimal()
            CotFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "3.916317365".toBigDecimal()
            CotFunction(decimalLiteral(BigDecimal("0.25"))) shouldExpressionEqualTo "3.916317365".toBigDecimal()
        }
    }

    /**
     * [DegreesFunction]
     *
     * ```sql
     *  SELECT DEGREES(0)
     *  SELECT DEGREES(1)
     *  SELECT DEGREES(0.25)
     *  SELECT DEGREES(0.25)
     *  ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DegreesFunction test`(testDB: TestDB) {
        withTable(testDB) {
            DegreesFunction(intLiteral(0)) shouldExpressionEqualTo "0".toBigDecimal()

            DegreesFunction(intLiteral(1)) shouldExpressionEqualTo "57.29577951308232".toBigDecimal()
            DegreesFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "14.32394487827058".toBigDecimal()
            DegreesFunction(decimalLiteral(BigDecimal("0.25"))) shouldExpressionEqualTo "14.32394487827058".toBigDecimal()
        }
    }

    /**
     * [PiFunction]
     *
     * ```sql
     * SELECT PI()
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `PiFunction test`(testDB: TestDB) {
        withTable(testDB) {
            when (testDB) {
                in TestDB.ALL_MYSQL_MARIADB -> PiFunction shouldExpressionEqualTo "3.141593".toBigDecimal()

                else -> PiFunction shouldExpressionEqualTo "3.141592653589793".toBigDecimal()
            }
        }
    }

    /**
     * [RadiansFunction]
     *
     * ```sql
     * SELECT RADIANS(0)
     * SELECT RADIANS(180)
     * SELECT RADIANS(0.25)
     * SELECT RADIANS(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `RadiansFunction test`(testDB: TestDB) {
        withTable(testDB) {
            RadiansFunction(intLiteral(0)) shouldExpressionEqualTo "0".toBigDecimal()


            RadiansFunction(intLiteral(180)) shouldExpressionEqualTo "3.141592653589793".toBigDecimal()
            RadiansFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "0.004363323129985824".toBigDecimal()
            RadiansFunction(decimalLiteral(BigDecimal("0.25"))) shouldExpressionEqualTo "0.004363323129985824".toBigDecimal()
        }
    }

    /**
     * [SinFunction]
     *
     * ```sql
     * SELECT SIN(0)
     * SELECT SIN(1)
     * SELECT SIN(0.25)
     * SELECT SIN(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SinFunction test`(testDB: TestDB) {
        withTable(testDB) {
            SinFunction(intLiteral(0)) shouldExpressionEqualTo "0".toBigDecimal()
            SinFunction(intLiteral(1)) shouldExpressionEqualTo "0.841470985".toBigDecimal()
            SinFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "0.2474039593".toBigDecimal()
            SinFunction(decimalLiteral(BigDecimal("0.25"))) shouldExpressionEqualTo "0.2474039593".toBigDecimal()
        }
    }

    /**
     * [TanFunction]
     *
     * ```sql
     * SELECT TAN(0)
     * SELECT TAN(1)
     * SELECT TAN(0.25)
     * SELECT TAN(0.25)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TanFunction test`(testDB: TestDB) {
        withTable(testDB) {
            TanFunction(intLiteral(0)) shouldExpressionEqualTo "0".toBigDecimal()
            TanFunction(intLiteral(1)) shouldExpressionEqualTo "1.557407725".toBigDecimal()
            TanFunction(doubleLiteral(0.25)) shouldExpressionEqualTo "0.2553419212".toBigDecimal()
            TanFunction(decimalLiteral(BigDecimal("0.25"))) shouldExpressionEqualTo "0.2553419212".toBigDecimal()
        }
    }
}
