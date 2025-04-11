package exposed.examples.transactions

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.inProperCase
import exposed.shared.tests.withTables
import io.bluetape4k.support.ifTrue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * `Transaction.exec()` 사용 시,
 * SQL Statements 에 Parameter 를 전달하여 실행하는 방식에 대한 테스트 코드입니다.
 *
 * ```kotlin
 * Transaction.exec(
 *      stmt = "INSERT INTO tmp (foo) VALUES (?)",
 *      args = listOf(VarCharColumnType() to "John \"Johny\" Johnson"),
 *      explicitStatementType = StatementType.INSERT
 * )
 * ```
 */
class Ex03_Parameterization: AbstractExposedTest() {

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS tmp (
     *      username VARCHAR(50) NULL
     * )
     * ```
     */
    object TempTable: Table("param_table") {
        val name = varchar("username", 50).nullable()
    }

    private val supportMultipleStatements = TestDB.ALL_MYSQL + TestDB.POSTGRESQL

    /**
     * 하나의 SQL문에 하나의 인자를 전달하여 실행하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with quotes and get it back`(testDB: TestDB) {
        withTables(testDB, TempTable) {
            val name = faker.name().fullName()

            exec(
                stmt = "INSERT INTO ${TempTable.tableName} (username) VALUES (?)",
                args = listOf(VarCharColumnType() to name),
                explicitStatementType = StatementType.INSERT
            )

            TempTable.selectAll()
                .single()[TempTable.name] shouldBeEqualTo name
        }
    }

    /**
     * 복수의 SQL문에 하나의 인자를 전달하여 실행하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `single parameters with multiple statements`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportMultipleStatements }

        val db = Database.connect(
            testDB.connection.invoke().plus(urlExtra(testDB)),
            testDB.driver,
            testDB.user,
            testDB.pass,
        )

        transaction(db) {
            try {
                SchemaUtils.create(TempTable)

                val table = TempTable.tableName.inProperCase()
                val column = TempTable.name.name.inProperCase()

                /**
                 * 복합적인 SQL 문을 파라미터를 사용하여 실행합니다.
                 */
                val result = exec(
                    """
                        INSERT INTO $table ($column) VALUES (?);
                        INSERT INTO $table ($column) VALUES (?);
                        INSERT INTO $table ($column) VALUES (?);
                        DELETE FROM $table WHERE $table.$column LIKE ?;
                        SELECT COUNT(*) FROM $table;
                    """.trimIndent(),
                    listOf(
                        VarCharColumnType() to "Anne",
                        VarCharColumnType() to "Anya",
                        VarCharColumnType() to "Anna",
                        VarCharColumnType() to "Ann%"
                    ),
                    StatementType.MULTI
                ) { resultSet ->
                    resultSet.next().ifTrue { resultSet.getInt(1) }
                }
                result shouldBeEqualTo 1

                TempTable.selectAll().single()[TempTable.name] shouldBeEqualTo "Anya"
            } finally {
                SchemaUtils.drop(TempTable)
            }
        }

        TransactionManager.closeAndUnregister(db)
    }

    /**
     * 복수 SQL문에 다수의 인자를 전달하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multiple parameters with multiple statements`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportMultipleStatements }

        val tester = object: Table("tester") {
            val name = varchar("username", 50)
            val age = integer("age")
            val active = bool("active")
        }

        val db = Database.connect(
            testDB.connection.invoke().plus(urlExtra(testDB)),
            testDB.driver,
            testDB.user,
            testDB.pass,
        )

        transaction(db) {
            try {
                SchemaUtils.create(tester)

                val table = tester.tableName.inProperCase()
                val (name, age, active) = tester.columns.map { it.name.inProperCase() }

                val result = exec(
                    """
                        INSERT INTO $table ($active, $age, $name) VALUES (?, ?, ?);
                        INSERT INTO $table ($active, $age, $name) VALUES (?, ?, ?);
                        UPDATE $table SET $age=? WHERE ($table.$name LIKE ?) AND ($table.$active = ?);
                        SELECT COUNT(*) FROM $table WHERE ($table.$name LIKE ?) AND ($table.$age = ?);
                    """.trimIndent(),
                    args = listOf(
                        BooleanColumnType() to true, IntegerColumnType() to 1, VarCharColumnType() to "Anna",
                        BooleanColumnType() to false, IntegerColumnType() to 1, VarCharColumnType() to "Anya",
                        IntegerColumnType() to 2, VarCharColumnType() to "A%", BooleanColumnType() to true,
                        VarCharColumnType() to "A%", IntegerColumnType() to 2
                    ),
                    explicitStatementType = StatementType.MULTI
                ) { resultSet ->
                    resultSet.next().ifTrue { resultSet.getInt(1) }
                }
                result shouldBeEqualTo 1

                tester.selectAll().count().toInt() shouldBeEqualTo 2

            } finally {
                SchemaUtils.drop(tester)
            }
        }

        TransactionManager.closeAndUnregister(db)
    }

    /**
     * null 인자를 전달하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `null parameter with logger`(testDB: TestDB) {
        withTables(testDB, TempTable) {
            // the logger is left in to test that it does not throw IllegalStateException with null parameter arg
            // 이 것보다 logback 등 표준 logger 를 사용하는 것을 추천합니다.
            // ex) <logger name="Exposed" level="DEBUG"/>
            // or addLogger(Slf4jSqlDebugLogger)
            addLogger(StdOutSqlLogger)

            /**
             * ```sql
             * INSERT INTO tmp (foo) VALUES (NULL)
             * ```
             */
            exec(
                stmt = "INSERT INTO ${TempTable.tableName} (${TempTable.name.name}) VALUES (?)",
                args = listOf(VarCharColumnType() to null),
                explicitStatementType = StatementType.INSERT,
            )

            TempTable.selectAll().single()[TempTable.name].shouldBeNull()
        }
    }

    /**
     * MySQL jdbcUrl에 allowMultiQueries=true 를 추가해야 합니다.
     */
    private fun urlExtra(testDB: TestDB): String {
        return when (testDB) {
            in TestDB.ALL_MYSQL -> "&allowMultiQueries=true"
            else -> ""
        }
    }
}
