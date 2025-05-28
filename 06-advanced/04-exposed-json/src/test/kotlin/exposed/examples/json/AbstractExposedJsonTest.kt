package exposed.examples.json

import exposed.examples.json.JsonTestData.JsonArrayTable
import exposed.examples.json.JsonTestData.JsonBArrayTable
import exposed.examples.json.JsonTestData.JsonBTable
import exposed.examples.json.JsonTestData.JsonTable
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assumptions

abstract class AbstractExposedJsonTest: JdbcExposedTestBase() {

    companion object: KLogging()


    protected fun withJsonTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: JsonTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = JsonTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            /**
             * JSON column 에 JSON 데이터를 저장합니다.
             * ```sql
             * -- Postgres
             * INSERT INTO j_table (j_column)
             * VALUES ({"user":{"name":"Admin","team":null},"logins":10,"active":true,"team":null})
             * ```
             */
            tester.insert { it[jsonColumn] = data1 }

            statement(tester, user1, data1)
        }
    }

    protected fun withJsonBTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: JsonBTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = JsonBTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert { it[jsonBColumn] = data1 }

            statement(tester, user1, data1)
        }
    }

    protected fun withJsonArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: JsonArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        val tester = JsonArrayTable

        withTables(testDB, tester) {
            /**
             * ```sql
             * INSERT INTO j_arrays ("groups", numbers)
             * VALUES ({"users":[{"name":"Admin","team":"Team A"}]}, [100])
             * ```
             */
            val singleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(listOf(User("Admin", "Team A")))
                it[tester.numbers] = intArrayOf(100)
            }

            /**
             * ```sql
             * INSERT INTO j_arrays ("groups", numbers)
             * VALUES (
             *      {"users":[{"name":"B","team":"Team B"},{"name":"C","team":"Team C"},{"name":"D","team":"Team D"}]},
             *      [3,4,5]
             * )
             * ```
             */
            val tripleId = tester.insertAndGetId {
                // User name = "B", "C", "D"
                // User team = "Team B", "Team C", "Team D"
                it[tester.groups] = UserGroup(List(3) { i -> User("${'B' + i}", "Team ${'B' + i}") })
                it[numbers] = intArrayOf(3, 4, 5)
            }

            statement(tester, singleId, tripleId)
        }
    }

    protected fun withJsonBArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: JsonBArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        val tester = JsonBArrayTable

        withTables(testDB, tester) {
            val singleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(listOf(User("Admin", "Team A")))
                it[tester.numbers] = intArrayOf(100)
            }
            val tripleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(List(3) { i -> User("${'B' + i}", "Team ${'B' + i}") })
                it[tester.numbers] = intArrayOf(3, 4, 5)
            }

            statement(tester, singleId, tripleId)
        }
    }
}
