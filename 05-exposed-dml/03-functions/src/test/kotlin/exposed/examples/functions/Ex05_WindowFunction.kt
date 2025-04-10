package exposed.examples.functions

import exposed.shared.dml.DMLTestData
import exposed.shared.dml.DMLTestData.withSales
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.cumeDist
import org.jetbrains.exposed.sql.SqlExpressionBuilder.denseRank
import org.jetbrains.exposed.sql.SqlExpressionBuilder.firstValue
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lag
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lastValue
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lead
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.nthValue
import org.jetbrains.exposed.sql.SqlExpressionBuilder.ntile
import org.jetbrains.exposed.sql.SqlExpressionBuilder.percentRank
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.rank
import org.jetbrains.exposed.sql.SqlExpressionBuilder.rowNumber
import org.jetbrains.exposed.sql.WindowFrameBound
import org.jetbrains.exposed.sql.WindowFunctionDefinition
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.decimalLiteral
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.min
import org.jetbrains.exposed.sql.stdDevPop
import org.jetbrains.exposed.sql.stdDevSamp
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.varPop
import org.jetbrains.exposed.sql.varSamp
import org.jetbrains.exposed.sql.vendors.currentDialect
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.RoundingMode

class Ex05_WindowFunction: Ex00_FunctionBase() {

    companion object: KLogging()

    private val supportsCountDistinctAsWindowFunction = TestDB.ALL_H2
    private val supportsStatisticsAggregateFunctions = TestDB.ALL
    private val supportsNthValueFunction = TestDB.ALL
    private val supportsExpressionsInWindowFunctionArguments = TestDB.ALL - TestDB.ALL_MYSQL
    private val supportsExpressionsInWindowFrameClause = TestDB.ALL - TestDB.ALL_MYSQL_MARIADB
    private val supportsDefaultValueInLeadLagFunctions = TestDB.ALL - TestDB.MARIADB
    private val supportsRangeModeWithOffsetFrameBound = TestDB.ALL

    /**
     * Window functions
     *
     * ```sql
     * -- Postgres
     * SELECT ROW_NUMBER() OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT RANK() OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT DENSE_RANK() OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT PERCENT_RANK() OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT CUME_DIST() OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT NTILE(2) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LAG(sales.amount, 1) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LEAD(sales.amount, 1) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LAG(sales.amount, 1, -1.0) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LEAD(sales.amount, 1, -1.0) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT FIRST_VALUE(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LAST_VALUE(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT NTH_VALUE(sales.amount, 2) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT NTILE((1 + 1)) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LAG(sales.amount, (2 - 1)) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT LEAD(sales.amount, (2 - 1)) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT NTH_VALUE(sales.amount, (1 + 1)) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `윈도우 함수 기본 사용법`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        withSales(testDB) { _, sales ->
            sales.assertWindowFunctionDefinition(
                rowNumber().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOf(1, 1, 2, 1, 1, 1, 2)
            )
            sales.assertWindowFunctionDefinition(
                rank().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOf(1, 1, 2, 1, 1, 1, 2)
            )
            sales.assertWindowFunctionDefinition(
                denseRank().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOf(1, 1, 2, 1, 1, 1, 2)
            )
            sales.assertWindowFunctionDefinition(
                percentRank().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOfBigDecimal("0", "0", "1", "0", "0", "0", "1").filterNotNull()
            )

            sales.assertWindowFunctionDefinition(
                cumeDist().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOfBigDecimal("0.5", "1", "1", "0.5", "1", "1", "1").filterNotNull()
            )
            sales.assertWindowFunctionDefinition(
                ntile(intLiteral(2)).over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOf(1, 1, 2, 1, 1, 1, 2)
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.lag(intLiteral(1)).over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOfBigDecimal(null, null, "550.1", null, null, null, "1620.1")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.lead(intLiteral(1)).over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOfBigDecimal("900.3", null, null, "1870.9", null, null, null)
            )

            if (testDB in supportsDefaultValueInLeadLagFunctions) {
                sales.assertWindowFunctionDefinition(
                    sales.amount.lag(intLiteral(1), decimalLiteral(BigDecimal("-1.0"))).over()
                        .partitionBy(sales.year, sales.product).orderBy(sales.amount),
                    listOfBigDecimal("-1", "-1", "550.1", "-1", "-1", "-1", "1620.1")
                )
                sales.assertWindowFunctionDefinition(
                    sales.amount.lead(intLiteral(1), decimalLiteral(BigDecimal("-1.0"))).over()
                        .partitionBy(sales.year, sales.product).orderBy(sales.amount),
                    listOfBigDecimal("900.3", "-1", "-1", "1870.9", "-1", "-1", "-1")
                )
            }

            sales.assertWindowFunctionDefinition(
                sales.amount.firstValue().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOfBigDecimal("550.1", "1500.25", "550.1", "1620.1", "650.7", "10.2", "1620.1").filterNotNull()
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.lastValue().over().partitionBy(sales.year, sales.product).orderBy(sales.amount),
                listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9").filterNotNull()
            )

            if (testDB in supportsNthValueFunction) {
                sales.assertWindowFunctionDefinition(
                    sales.amount.nthValue(intLiteral(2)).over().partitionBy(sales.year, sales.product)
                        .orderBy(sales.amount),
                    listOfBigDecimal(null, null, "900.3", null, null, null, "1870.9")
                )
            }

            if (testDB in supportsExpressionsInWindowFunctionArguments) {
                sales.assertWindowFunctionDefinition(
                    ntile(intLiteral(1) + intLiteral(1)).over().partitionBy(sales.year, sales.product)
                        .orderBy(sales.amount),
                    listOf(1, 1, 2, 1, 1, 1, 2)
                )
                sales.assertWindowFunctionDefinition(
                    sales.amount.lag(intLiteral(2) - intLiteral(1)).over().partitionBy(sales.year, sales.product)
                        .orderBy(sales.amount),
                    listOfBigDecimal(null, null, "550.1", null, null, null, "1620.1")
                )
                sales.assertWindowFunctionDefinition(
                    sales.amount.lead(intLiteral(2) - intLiteral(1)).over().partitionBy(sales.year, sales.product)
                        .orderBy(sales.amount),
                    listOfBigDecimal("900.3", null, null, "1870.9", null, null, null)
                )

                if (testDB in supportsNthValueFunction) {
                    sales.assertWindowFunctionDefinition(
                        sales.amount.nthValue(intLiteral(1) + intLiteral(1)).over()
                            .partitionBy(sales.year, sales.product).orderBy(sales.amount),
                        listOfBigDecimal(null, null, "900.3", null, null, null, "1870.9")
                    )
                }
            }
        }
    }

    /**
     * 집계 함수를 윈도우 함수로 사용하기
     *
     * ```sql
     * SELECT MIN(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT MAX(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT AVG(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT COUNT(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT STDDEV_POP(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT STDDEV_SAMP(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT VAR_POP(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT VAR_SAMP(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `집계함수를 윈도우 함수로 사용하기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        withSales(testDB) { _, sales ->
            sales.assertWindowFunctionDefinition(
                sales.amount.min().over().partitionBy(sales.year, sales.product),
                listOfBigDecimal("550.1", "1500.25", "550.1", "1620.1", "650.7", "10.2", "1620.1")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.max().over().partitionBy(sales.year, sales.product),
                listOfBigDecimal("900.3", "1500.25", "900.3", "1870.9", "650.7", "10.2", "1870.9")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.avg().over().partitionBy(sales.year, sales.product),
                listOfBigDecimal("725.2", "1500.25", "725.2", "1745.5", "650.7", "10.2", "1745.5")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over().partitionBy(sales.year, sales.product),
                listOfBigDecimal("1450.4", "1500.25", "1450.4", "3491", "650.7", "10.2", "3491")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.count().over().partitionBy(sales.year, sales.product),
                listOf(2, 1, 2, 2, 1, 1, 2)
            )

            if (testDB in supportsStatisticsAggregateFunctions) {
                sales.assertWindowFunctionDefinition(
                    sales.amount.stdDevPop().over().partitionBy(sales.year, sales.product),
                    listOfBigDecimal("175.1", "0", "175.1", "125.4", "0", "0", "125.4")
                )
                sales.assertWindowFunctionDefinition(
                    sales.amount.stdDevSamp().over().partitionBy(sales.year, sales.product),
                    listOfBigDecimal("247.63", null, "247.63", "177.34", null, null, "177.34")
                )
                sales.assertWindowFunctionDefinition(
                    sales.amount.varPop().over().partitionBy(sales.year, sales.product),
                    listOfBigDecimal("30660.01", "0", "30660.01", "15725.16", "0", "0", "15725.16")
                )
                sales.assertWindowFunctionDefinition(
                    sales.amount.varSamp().over().partitionBy(sales.year, sales.product),
                    listOfBigDecimal("61320.02", null, "61320.02", "31450.32", null, null, "31450.32")
                )
            }

            if (testDB in supportsCountDistinctAsWindowFunction) {
                /**
                 * ```sql
                 * -- H2, H2_MYSQL, H2_PSQL
                 * SELECT COUNT(DISTINCT SALES.AMOUNT) OVER(PARTITION BY SALES."year", SALES.PRODUCT)
                 *   FROM SALES
                 *  ORDER BY SALES."year" ASC,
                 *           SALES."month" ASC,
                 *           SALES.PRODUCT ASC NULLS FIRST
                 * ```
                 */
                sales.assertWindowFunctionDefinition(
                    sales.amount.countDistinct().over().partitionBy(sales.year, sales.product),
                    listOf(2, 1, 2, 2, 1, 1, 2)
                )
            }
        }
    }

    /**
     * Partition by clause
     *
     * ```sql
     * SELECT SUM(sales.amount) OVER()
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER() FROM
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year")
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `partitionBy 절 적용에 따른 소계`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        withSales(testDB) { _, sales ->
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over(),
                listOfBigDecimal("7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over().partitionBy(),
                listOfBigDecimal("7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over().partitionBy(sales.year),
                listOfBigDecimal("2950.65", "2950.65", "2950.65", "4151.9", "4151.9", "4151.9", "4151.9")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over().partitionBy(sales.year, sales.product),
                listOfBigDecimal("1450.4", "1500.25", "1450.4", "3491", "650.7", "10.2", "3491")
            )
        }
    }

    /**
     * `sum()` window function with `ORDER BY` clause
     *
     * ```sql
     * SELECT SUM(sales.amount) OVER()
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER()
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER( ORDER BY sales."year" ASC)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER( ORDER BY sales."year" DESC, sales.product ASC NULLS FIRST)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy 적용에 따른 소계`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        withSales(testDB) { _, sales ->
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over(),
                listOfBigDecimal("7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over().orderBy(),
                listOfBigDecimal("7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55", "7102.55")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over().orderBy(sales.year),
                listOfBigDecimal("2950.65", "2950.65", "2950.65", "7102.55", "7102.55", "7102.55", "7102.55")
            )
            sales.assertWindowFunctionDefinition(
                sales.amount.sum().over()
                    .orderBy(sales.year to SortOrder.DESC, sales.product to SortOrder.ASC_NULLS_FIRST),
                listOfBigDecimal("7102.55", "5652.15", "7102.55", "3501.2", "4151.9", "10.2", "3501.2")
            )
        }
    }

    /**
     * Window frame clause
     *
     * ```sql
     * -- Postgres
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS UNBOUNDED PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS 1 PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN 1 FOLLOWING AND 2 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN 2 PRECEDING AND 1 PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN CURRENT ROW AND 2 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN CURRENT ROW AND CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN (1 + 1) PRECEDING AND (1 + 1) FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE UNBOUNDED PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE 1 PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN 1 PRECEDING AND 1 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN 1 FOLLOWING AND 2 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN 2 PRECEDING AND 1 PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN 2 PRECEDING AND CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN CURRENT ROW AND 2 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN (1 + 1) PRECEDING AND (1 + 1) FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC RANGE BETWEEN CURRENT ROW AND CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS UNBOUNDED PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS 1 PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN 1 FOLLOWING AND 2 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN 2 PRECEDING AND 1 PRECEDING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN 2 PRECEDING AND CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN CURRENT ROW AND 2 FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN CURRENT ROW AND CURRENT ROW)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN (1 + 1) PRECEDING AND (1 + 1) FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     *
     * SELECT SUM(sales.amount) OVER(PARTITION BY sales."year", sales.product ORDER BY sales.amount ASC GROUPS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)
     *   FROM sales
     *  ORDER BY sales."year" ASC,
     *           sales."month" ASC,
     *           sales.product ASC NULLS FIRST;
     * ```
     */
    @Suppress("LongMethod")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `WindowFrameBound 종류별 결과값 비교`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        withSales(testDB) { _, sales ->
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales).rows(WindowFrameBound.unboundedPreceding()),
                listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales).rows(WindowFrameBound.offsetPreceding(1)),
                listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales).rows(WindowFrameBound.currentRow()),
                listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.offsetPreceding(1), WindowFrameBound.offsetFollowing(1)),
                listOfBigDecimal("1450.4", "1500.25", "1450.4", "3491", "650.7", "10.2", "3491")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.offsetFollowing(1), WindowFrameBound.offsetFollowing(2)),
                listOfBigDecimal("900.3", null, null, "1870.9", null, null, null)
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.offsetPreceding(2), WindowFrameBound.offsetPreceding(1)),
                listOfBigDecimal(null, null, "550.1", null, null, null, "1620.1")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.offsetPreceding(2), WindowFrameBound.currentRow()),
                listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.currentRow(), WindowFrameBound.offsetFollowing(2)),
                listOfBigDecimal("1450.4", "1500.25", "900.3", "3491", "650.7", "10.2", "1870.9")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.currentRow(), WindowFrameBound.currentRow()),
                listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
            )

            if (testDB in supportsExpressionsInWindowFrameClause) {
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales).rows(
                        WindowFrameBound.offsetPreceding(intLiteral(1) + intLiteral(1)),
                        WindowFrameBound.offsetFollowing(intLiteral(1) + intLiteral(1))
                    ),
                    listOfBigDecimal("1450.4", "1500.25", "1450.4", "3491", "650.7", "10.2", "3491")
                )
            }

            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .rows(WindowFrameBound.currentRow(), WindowFrameBound.unboundedFollowing()),
                listOfBigDecimal("1450.4", "1500.25", "900.3", "3491", "650.7", "10.2", "1870.9")
            )

            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales).range(WindowFrameBound.unboundedPreceding()),
                listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales).range(WindowFrameBound.currentRow()),
                listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
            )

            if (testDB in supportsRangeModeWithOffsetFrameBound) {
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales).range(WindowFrameBound.offsetPreceding(1)),
                    listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .range(WindowFrameBound.offsetPreceding(1), WindowFrameBound.offsetFollowing(1)),
                    listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .range(WindowFrameBound.offsetFollowing(1), WindowFrameBound.offsetFollowing(2)),
                    listOfBigDecimal(null, null, null, null, null, null, null)
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .range(WindowFrameBound.offsetPreceding(2), WindowFrameBound.offsetPreceding(1)),
                    listOfBigDecimal(null, null, null, null, null, null, null)
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .range(WindowFrameBound.offsetPreceding(2), WindowFrameBound.currentRow()),
                    listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .range(WindowFrameBound.currentRow(), WindowFrameBound.offsetFollowing(2)),
                    listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                )

                if (testDB in supportsExpressionsInWindowFrameClause) {
                    sales.assertWindowFunctionDefinition(
                        sumAmountPartitionByYearProductOrderByAmount(sales).range(
                            WindowFrameBound.offsetPreceding(intLiteral(1) + intLiteral(1)),
                            WindowFrameBound.offsetFollowing(intLiteral(1) + intLiteral(1))
                        ),
                        listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                    )
                }
            }
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .range(WindowFrameBound.currentRow(), WindowFrameBound.unboundedFollowing()),
                listOfBigDecimal("1450.4", "1500.25", "900.3", "3491", "650.7", "10.2", "1870.9")
            )
            sales.assertWindowFunctionDefinition(
                sumAmountPartitionByYearProductOrderByAmount(sales)
                    .range(WindowFrameBound.currentRow(), WindowFrameBound.currentRow()),
                listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
            )

            if (currentDialect.supportsWindowFrameGroupsMode) {
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales).groups(WindowFrameBound.unboundedPreceding()),
                    listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales).groups(WindowFrameBound.offsetPreceding(1)),
                    listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales).groups(WindowFrameBound.currentRow()),
                    listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.offsetPreceding(1), WindowFrameBound.offsetFollowing(1)),
                    listOfBigDecimal("1450.4", "1500.25", "1450.4", "3491", "650.7", "10.2", "3491")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.offsetFollowing(1), WindowFrameBound.offsetFollowing(2)),
                    listOfBigDecimal("900.3", null, null, "1870.9", null, null, null)
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.offsetPreceding(2), WindowFrameBound.offsetPreceding(1)),
                    listOfBigDecimal(null, null, "550.1", null, null, null, "1620.1")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.offsetPreceding(2), WindowFrameBound.currentRow()),
                    listOfBigDecimal("550.1", "1500.25", "1450.4", "1620.1", "650.7", "10.2", "3491")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.currentRow(), WindowFrameBound.offsetFollowing(2)),
                    listOfBigDecimal("1450.4", "1500.25", "900.3", "3491", "650.7", "10.2", "1870.9")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.currentRow(), WindowFrameBound.currentRow()),
                    listOfBigDecimal("550.1", "1500.25", "900.3", "1620.1", "650.7", "10.2", "1870.9")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales).groups(
                        WindowFrameBound.offsetPreceding(intLiteral(1) + intLiteral(1)),
                        WindowFrameBound.offsetFollowing(intLiteral(1) + intLiteral(1))
                    ),
                    listOfBigDecimal("1450.4", "1500.25", "1450.4", "3491", "650.7", "10.2", "3491")
                )
                sales.assertWindowFunctionDefinition(
                    sumAmountPartitionByYearProductOrderByAmount(sales)
                        .groups(WindowFrameBound.currentRow(), WindowFrameBound.unboundedFollowing()),
                    listOfBigDecimal("1450.4", "1500.25", "900.3", "3491", "650.7", "10.2", "1870.9")
                )
            }
        }
    }

    /**
     * [definition] 결과값과 [expectedResult] 결과값이 같음을 검증합니다.
     */
    private fun <T> DMLTestData.Sales.assertWindowFunctionDefinition(
        definition: WindowFunctionDefinition<T>,
        expectedResult: List<T>,
    ) {
        val result = select(year, month, product, amount, definition)
            .orderBy(
                year to SortOrder.ASC,
                month to SortOrder.ASC,
                product to SortOrder.ASC_NULLS_FIRST
            )
            .onEach { log.debug { "row=${it[year]},${it[month]},${it[product]},${it[amount]},${it[definition]}" } }
            .map { it[definition] }

        result shouldBeEqualTo expectedResult
    }

    private fun sumAmountPartitionByYearProductOrderByAmount(sales: DMLTestData.Sales) =
        sales.amount.sum()
            .over().partitionBy(sales.year, sales.product)
            .orderBy(sales.amount)

    private fun listOfBigDecimal(vararg numbers: String?): List<BigDecimal?> {
        return numbers.map { it?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP) }
    }
}
