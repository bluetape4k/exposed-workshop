package exposed.examples.transactions

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.inProperCase
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.ifTrue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.autoIncColumnType
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.utility.Base58
import java.sql.ResultSet

class Ex02_TransactionExec: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE SEQUENCE IF NOT EXISTS exec_id_seq
     *      START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807;
     *
     * CREATE TABLE IF NOT EXISTS exec_table (
     *      id INT PRIMARY KEY,
     *      amount INT NOT NULL
     * );
     * ```
     */
    object ExecTable: Table("exec_table") {
        val id = integer("id").autoIncrement("exec_id_seq")
        val amount = integer("amount")

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * Exposed [Transaction.exec] method에 대한 한 줄의 SQL을 전달하여 실행하는 방법에 대한 테스트 코드입니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exec with single statement query`(testDB: TestDB) {
        withTables(testDB, ExecTable) {
            val amounts = (90..99).toList()

            ExecTable.batchInsert(amounts, shouldReturnGeneratedValues = false) { amount ->
                this[ExecTable.id] = (amount % 10 + 1)  // autoIncrement 이지만, custom 으로 설정 
                this[ExecTable.amount] = amount
            }

            /**
             * ```sql
             * SELECT * FROM exec_table;
             * ```
             */
            val results: MutableList<Int> = exec(
                """SELECT * FROM ${ExecTable.tableName.inProperCase()}""",
                explicitStatementType = StatementType.SELECT
            ) { resultSet: ResultSet ->
                val allAmounts = mutableListOf<Int>()
                while (resultSet.next()) {
                    val id = resultSet.getInt("id")
                    val loadedAmount = resultSet.getInt("amount")
                    log.debug { "Loaded id=$id, amount: $loadedAmount" }
                    allAmounts.add(loadedAmount)
                }
                allAmounts
            }!!.shouldNotBeEmpty()

            results shouldBeEqualTo amounts
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exec with multi statement query`(testDB: TestDB) {
        // PGjdbc-NG 드라이버는 단일 PreparedStatement에서 여러 명령을 허용하지 않습니다.
        // SQLite 및 H2 드라이버는 여러 개를 허용하지만 첫 번째 문장의 결과만 반환합니다.
        // SQLite issue tracker: https://github.com/xerial/sqlite-jdbc/issues/277
        // H2 issue tracker: https://github.com/h2database/h2database/issues/3704
        val toExclude = TestDB.ALL_H2 + TestDB.ALL_MYSQL_LIKE + listOf(TestDB.POSTGRESQLNG)

        Assumptions.assumeTrue { testDB !in toExclude }
        withTables(testDB, ExecTable) {
            testInsertAndSelectInSingleExec(testDB)
        }
    }

    /**
     * MySQL/MariaDB에서 `allowMultiQueries=true` 설정을 추가하여 여러 개의 SQL 문을 실행하는 방법에 대한 테스트 코드입니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exec with multi statement query using MySQL`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        val extra = if (testDB in TestDB.ALL_MARIADB) "?" else ""
        val db = Database.connect(
            testDB.connection().plus("$extra&allowMultiQueries=true"),
            testDB.driver,
            testDB.user,
            testDB.pass
        )

        transaction(db) {
            try {
                SchemaUtils.create(ExecTable)
                commit()
                testInsertAndSelectInSingleExec(testDB)
                commit()
            } finally {
                SchemaUtils.drop(ExecTable)
            }
        }

        TransactionManager.closeAndUnregister(db)
    }

    private fun Transaction.testInsertAndSelectInSingleExec(testDB: TestDB) {
        ExecTable.insert {
            it[amount] = 99
        }

        val insertStatement = "INSERT INTO ${ExecTable.tableName.inProperCase()} " +
                "(${ExecTable.amount.name.inProperCase()}, ${ExecTable.id.name.inProperCase()}) " +
                "VALUES (100, ${ExecTable.id.autoIncColumnType?.nextValExpression})"

        val columnAlias = "last_inserted_id"
        val selectLastIdStatement = when (testDB) {
            TestDB.POSTGRESQL -> "SELECT lastval() AS $columnAlias;"
            TestDB.MARIADB -> "SELECT LASTVAL(${ExecTable.id.autoIncColumnType?.autoincSeq}) AS $columnAlias"
            else -> "SELECT LAST_INSERT_ID() AS $columnAlias"
        }

        val insertAndSelectStatements =
            """
            $insertStatement;
            $selectLastIdStatement;
            """.trimIndent()

        val result = exec(
            insertAndSelectStatements,
            explicitStatementType = StatementType.MULTI
        ) { resultSet ->
            resultSet.next()
            resultSet.getInt(columnAlias)
        }
        result.shouldNotBeNull() shouldBeEqualTo 2
    }

    /**
     * [Transaction.exec] 메서드 실행의 결과인 [ResultSet]에서 결과물을 가져오는 방법에 대한 테스트 코드입니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exec with nullable and empty resultSets`(testDB: TestDB) {
        val tester = object: Table("tester_${Base58.randomString(4)}") {
            val id = integer("id")
            val title = varchar("title", 32)
        }

        withTables(testDB, tester) {
            tester.insert {
                it[id] = 1
                it[title] = "Exposed"
            }

            val (table, id, title) =
                listOf(tester.tableName, tester.id.name, tester.title.name)

            val stringResult = exec(
                """SELECT $title FROM $table WHERE $id = 1;"""
            ) { rs: ResultSet ->
                rs.next().ifTrue { rs.getString(title) }
            }
            stringResult shouldBeEqualTo "Exposed"

            // no record exists for id = 999, but result set returns single nullable value due to subquery alias
            val nullColumnResult = exec(
                """SELECT (SELECT $title FROM $table WHERE $id = 999) AS sub;"""
            ) { rs: ResultSet ->
                rs.next().ifTrue { rs.getString("sub") }
            }
            nullColumnResult.shouldBeNull()

            // no record exists for id = 999, so result set is empty and rs.next() is false
            val nullTransformResult = exec(
                """SELECT $title FROM $table WHERE $id = 999;"""
            ) { rs: ResultSet ->
                rs.next().ifTrue { rs.getString(title) }
            }
            nullTransformResult.shouldBeNull()
        }
    }
}
