package exposed.ddl.example

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.sql.selectImplicitAll
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.stringLiteral
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.SQLException
import kotlin.test.assertFails

class Ex04_ColumnDefinition: AbstractExposedTest() {

    // 컬럼 주석을 지원하는 DB - H2, MySQL 8, NOT Postgres
    val columnCommentSupportedDB = TestDB.ALL_H2 + TestDB.MYSQL_V8

    /**
     * 컬럼에 주석을 달 수 있다
     * ```sql
     * -- H2
     * CREATE TABLE IF NOT EXISTS TESTER (
     *      AMOUNT INT COMMENT 'Amount of testers' NOT NULL
     * )
     * ```
     *
     * ```sql
     * -- MySQL 8
     * CREATE TABLE IF NOT EXISTS tester (
     *      amount INT COMMENT 'Amount of testers' NOT NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼에 주석 달기`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in columnCommentSupportedDB }

        val comment = "Amount of testers"
        val tester = object: Table("tester") {
            val amount = integer("amount")
                .withDefinition("COMMENT", stringLiteral(comment))  // 컬럼에 주석 추가
        }

        withTables(testDB, tester) {
            val ddl = tester.ddl.single()
            log.info { "tester ddl: $ddl" }

            tester.insert {
                it[amount] = 9
            }
            tester.selectAll().single()[tester.amount] shouldBeEqualTo 9
        }
    }

    /**
     * 묵시적으로 컬럼 전체를 뜻하는 `*` 를 사용하면, INVISIBLE 컬럼은 반환되지 않습니다.
     *
     * ```sql
     * -- MySQL 8
     * CREATE TABLE IF NOT EXISTS tester (
     *      amount INT NOT NULL,
     *      active BOOLEAN INVISIBLE NULL   -- INVISIBLE 컬럼
     * )
     * ```
     *
     * ```sql
     * -- MySQL 8
     * SELECT *                    -- active 는 INVISIBLE 컬럼이므로 반환되지 않음
     *   FROM TESTER
     *  WHERE TESTER.AMOUNT > 100
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼의 visibility를 변경할 수 있다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in columnCommentSupportedDB }

        val tester = object: Table("tester") {
            val amount = integer("amount")
            val active = bool("active")
                .nullable()
                .withDefinition("INVISIBLE")  // Implicit 조회 시 (select * from tester), 컬럼을 숨김
        }

        withTables(testDB, tester) {
            val ddl = tester.ddl.single()
            log.info { "tester ddl: $ddl" }

            tester.insert {
                it[amount] = 999
                it[active] = true
            }

            /**
             * 묵시적으로 컬럼 전체를 뜻하는 `*` 를 사용하면, INVISIBLE 컬럼은 반환되지 않습니다.
             *
             * ```sql
             * SELECT *         -- active 는 INVISIBLE 컬럼이므로 반환되지 않음
             *   FROM TESTER
             *  WHERE TESTER.AMOUNT > 100
             * ```
             */
            tester.selectImplicitAll()
                .where { tester.amount greater 100 }
                .execute(this)  // HINT: 이렇게 Statement.execute(transaction) 을 수행하면 java.sql.ResultSet 를 반환합니다.
                .use { rs ->
                    rs!!.next()
                    rs.getInt(tester.amount.name) shouldBeEqualTo 999

                    expectException<SQLException> {
                        rs.getBoolean(tester.active.name)
                    }
                }

            assertFails {
                val row = tester.selectImplicitAll()
                    .where { tester.amount greater 100 }
                    .single()

                row[tester.amount] shouldBeEqualTo 999
                row.getOrNull(tester.active).shouldBeNull()   // INVISIBLE 컬럼은 반환되지 않음
            }

            /**
             * 명시적으로 INVISIBLE 컬럼을 지정해야만 반환됩니다.
             *
             * ```sql
             * SELECT TESTER.AMOUNT,
             *        TESTER.ACTIVE
             *   FROM TESTER
             *  WHERE TESTER.AMOUNT > 100
             * ```
             */
            tester.selectAll()
                .where { tester.amount greater 100 }
                .execute(this)   // HINT: 이렇게 Statement.execute(transaction) 을 수행하면 java.sql.ResultSet 를 반환합니다.
                .use { rs ->
                    rs.shouldNotBeNull()
                    rs.next()
                    rs.getInt(tester.amount.name) shouldBeEqualTo 999
                    rs.getBoolean(tester.active.name).shouldBeTrue()
                }
        }
    }
}
