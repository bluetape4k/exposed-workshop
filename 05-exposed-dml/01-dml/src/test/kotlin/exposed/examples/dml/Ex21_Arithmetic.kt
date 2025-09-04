package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.DivideOp
import org.jetbrains.exposed.v1.core.DivideOp.Companion.withScale
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.decimalLiteral
import org.jetbrains.exposed.v1.core.div
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.times
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

class Ex21_Arithmetic: JdbcExposedTestBase() {

    companion object: KLogging()


    /**
     * 컬럼 값을 산술 연산자를 사용하여 계산합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT userdata."value",
     *        (((userdata."value" - 5) * 2) / 2)
     *   FROM userdata
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `operator precedence of minus, plus, div times`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, _, userData ->

            val calculatedColumn: DivideOp<Int, Int> = ((userData.value - 5) * 2) / 2

            userData
                .select(userData.value, calculatedColumn)
                .forEach {
                    val value = it[userData.value]
                    val actualResult = it[calculatedColumn]
                    val expectedResult = ((value - 5) * 2) / 2
                    actualResult shouldBeEqualTo expectedResult
                }
        }
    }

    /**
     * `Expression.build { ten / three }`
     *
     * ```sql
     * SELECT (10 / 3)
     *
     * ```
     *
     * `withScale(2)`
     *
     * ```sql
     * SELECT (10.0 / 3)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `big decimal division with scale and without`(testDB: TestDB) {
        withDb(testDB) {
            val ten = decimalLiteral(10.toBigDecimal())
            val three = decimalLiteral(3.toBigDecimal())

            // SELECT (10 / 3)
            val divTenToThreeWithoutScale: DivideOp<BigDecimal, BigDecimal> = ten / three
            val resultWithoutScale = Table.Dual
                .select(divTenToThreeWithoutScale)
                .single()[divTenToThreeWithoutScale]

            resultWithoutScale shouldBeEqualTo 3.toBigDecimal()

            // SELECT (10.0 / 3)
            val divTenToThreeWithScale: DivideOp<BigDecimal, BigDecimal> = divTenToThreeWithoutScale.withScale(2)
            val resultWithScale = Table.Dual
                .select(divTenToThreeWithScale)
                .single()[divTenToThreeWithScale]

            resultWithScale shouldBeEqualTo 3.33.toBigDecimal()
        }
    }
}
