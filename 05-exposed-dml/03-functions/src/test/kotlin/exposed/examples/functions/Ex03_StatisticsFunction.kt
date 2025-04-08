package exposed.examples.functions

import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.stdDevPop
import org.jetbrains.exposed.sql.stdDevSamp
import org.jetbrains.exposed.sql.varPop
import org.jetbrains.exposed.sql.varSamp
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * SQL 통계 함수 예제
 */
class Ex03_StatisticsFunction: Ex00_FunctionBase() {

    companion object: KLogging()

    /**
     * STDDEV_POP: non-null 값만을 사용하여 모집단의 표준 편차(StdDev)를 계산, non-null 값이 없으면 null 반환
     *
     * ```sql
     * SELECT STDDEV_POP(SAMPLE_TABLE."number") FROM SAMPLE_TABLE
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StdDev Population`(testDB: TestDB) {
        withSampleTable(testDB) {
            val expectedStdDevPop = calculateStandardDeviation(isPopulation = true)
            SampleTestTable.number.stdDevPop(scale) shouldExpressionEqualTo expectedStdDevPop
        }
    }

    /**
     * STDDEV_SAMP : non-null 값만을 사용하여 샘플 표준편차 (Sample StdDev)를 계산
     *
     * ```sql
     * SELECT STDDEV_SAMP(sample_table."number") FROM sample_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StdDev Sample`(testDB: TestDB) {
        withSampleTable(testDB) {
            val expectedStdDevPop = calculateStandardDeviation(isPopulation = false)
            SampleTestTable.number.stdDevSamp(scale) shouldExpressionEqualTo expectedStdDevPop
        }
    }

    /**
     * VAR_POP : non-null 값만 사용하여 모집단의 분산(Variance) 를 계산, non-null 값이 없으면 null 반환
     *
     * ```sql
     * SELECT VAR_POP(sample_table."number") FROM sample_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `VarPop function`(testDB: TestDB) {
        withSampleTable(testDB) {
            val expectedVarPop = calculateVariance(isPopulation = true)
            SampleTestTable.number.varPop(scale) shouldExpressionEqualTo expectedVarPop
        }
    }

    /**
     * VAR_SAMP : non-null 값만 사용하여 샘플 분산(Sample Variance) 를 계산, non-null 값이 없으면 null 반환
     *
     * ```sql
     * SELECT VAR_SAMP(sample_table."number") FROM sample_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `VarSamp function`(testDB: TestDB) {
        withSampleTable(testDB) {
            val expectedVarSamp = calculateVariance(isPopulation = false)
            SampleTestTable.number.varSamp(scale) shouldExpressionEqualTo expectedVarSamp
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS sample_table (
     *      "number" INT NULL
     * );
     * ```
     */
    private object SampleTestTable: Table("sample_table") {
        val number = integer("number").nullable()
    }

    private val data: List<Int?> = listOf(4, null, 5, null, 6)
    private val scale = 4

    private fun withSampleTable(testDB: TestDB, body: Transaction.(TestDB) -> Unit) {
        // SQLite does not have any built-in statistics-specific aggregate functions
        withTables(testDB, SampleTestTable) {
            SampleTestTable.batchInsert(data) { num ->
                this[SampleTestTable.number] = num
            }
            body(testDB)
        }
    }

    private infix fun org.jetbrains.exposed.sql.Function<BigDecimal?>.shouldExpressionEqualTo(expected: BigDecimal) {
        val result = SampleTestTable.select(this).first()[this]
        result?.setScale(expected.scale(), RoundingMode.HALF_EVEN) shouldBeEqualTo expected
    }

    private fun calculateStandardDeviation(isPopulation: Boolean): BigDecimal {
        return calculateVariance(isPopulation).simpleSqrt()
    }

    private fun calculateVariance(isPopulation: Boolean): BigDecimal {
        val nonNullData = data.filterNotNull()
        val mean = nonNullData.average()
        val squaredSum = nonNullData.sumOf { n ->
            val deviation = n - mean
            deviation * deviation
        }
        val size = if (isPopulation) nonNullData.size else nonNullData.lastIndex
        return (squaredSum / size).toBigDecimal(MathContext(scale))
    }

    fun BigDecimal.simpleSqrt(): BigDecimal {
        if (this < BigDecimal.ZERO) {
            throw ArithmeticException("Square root of negative number")
        }
        if (this == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        val two = 2.toBigDecimal()
        val epsilon = 0.1.toBigDecimal().pow(scale)

        var low = BigDecimal.ZERO
        var high = this.max(BigDecimal.ONE)
        var result = (low + high).divide(two)

        while (true) {
            val square = result.multiply(result)
            val diff = square.subtract(this).abs()
            if (diff < epsilon) {
                break
            }

            if (result.multiply(result) < this) {
                low = result
            } else {
                high = result
            }
            result = (low + high).divide(two)
        }

        result = result.round(MathContext(scale, RoundingMode.HALF_EVEN))
        result = result.setScale(scale)
        return result
    }
}
