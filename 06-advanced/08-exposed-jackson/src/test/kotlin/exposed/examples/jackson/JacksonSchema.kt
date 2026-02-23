package exposed.examples.jackson

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.core.jackson.jackson
import io.bluetape4k.exposed.core.jackson.jacksonb
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assumptions

@Suppress("UnusedReceiverParameter")
/**
 * Jackson/JacksonB 컬럼 예제에서 공통으로 사용하는 스키마와 보조 모델을 제공한다.
 */
object JacksonSchema {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS jackson_table (
     *      id SERIAL PRIMARY KEY,
     *      jackson_column JSON NOT NULL
     * )
     * ```
     */
    object JacksonTable: IntIdTable("jackson_table") {
        val jacksonColumn = jackson<DataHolder>("jackson_column")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS jackson_b_table (
     *      id SERIAL PRIMARY KEY,
     *      jackson_b_column JSONB NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS jackson_b_table (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      jackson_b_column JSON NOT NULL
     * )
     * ```
     */
    object JacksonBTable: IntIdTable("jackson_b_table") {
        val jacksonBColumn = jacksonb<DataHolder>("jackson_b_column")
    }

    /**
     * [JacksonTable] 레코드를 다루는 DAO 엔티티.
     */
    class JacksonEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<JacksonEntity>(JacksonTable)

        var jacksonColumn by JacksonTable.jacksonColumn
    }

    /**
     * [JacksonBTable] 레코드를 다루는 DAO 엔티티.
     */
    class JacksonBEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<JacksonBEntity>(JacksonBTable)

        var jacksonBColumn by JacksonBTable.jacksonBColumn
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS jackson_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSON NOT NULL,
     *      numbers JSON NOT NULL
     * );
     * ```
     */
    object JacksonArrayTable: IntIdTable("jackson_arrays") {
        val groups = jackson<UserGroup>("groups")
        val numbers = jackson<IntArray>("numbers")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS jackson_b_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSONB NOT NULL,
     *      numbers JSONB NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS jackson_b_arrays (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `groups` JSON NOT NULL,
     *      numbers JSON NOT NULL
     * )
     * ```
     */
    object JacksonBArrayTable: IntIdTable("jackson_b_arrays") {
        val groups = jacksonb<UserGroup>("groups")
        val numbers = jacksonb<IntArray>("numbers")
    }


    /**
     * 사용자 정보와 로그인 상태를 JSON 문서로 저장하기 위한 모델.
     */
    data class DataHolder(val user: User, val logins: Int, val active: Boolean, val team: String?)

    /**
     * JSON 문서 내부의 사용자 정보를 표현하는 모델.
     */
    data class User(val name: String, val team: String?)

    /**
     * 사용자 목록(JSON 배열)을 표현하는 모델.
     */
    data class UserGroup(val users: List<User>)

    /**
     * [JacksonTable]에 기본 샘플 데이터를 준비한 뒤 테스트 코드를 실행한다.
     */
    fun AbstractExposedTest.withJacksonTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: JacksonSchema.JacksonTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = JacksonSchema.JacksonTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.jacksonColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    /**
     * [JacksonBTable]에 기본 샘플 데이터를 준비한 뒤 테스트 코드를 실행한다.
     */
    fun AbstractExposedTest.withJacksonBTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: JacksonSchema.JacksonBTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = JacksonSchema.JacksonBTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.jacksonBColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    /**
     * JSON 배열 컬럼 예제용 샘플 데이터를 준비한 뒤 테스트 코드를 실행한다.
     */
    fun AbstractExposedTest.withJacksonArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: JacksonSchema.JacksonArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = JacksonSchema.JacksonArrayTable

        withTables(testDB, tester) {
            val singleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(listOf(User("A", "Team A")))
                it[tester.numbers] = intArrayOf(100)
            }
            val tripleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(List(3) { i -> User("${'B' + i}", "Team ${'B' + i}") })
                it[tester.numbers] = intArrayOf(3, 4, 5)
            }

            statement(tester, singleId, tripleId)
        }
    }

    /**
     * JSONB 배열 컬럼 예제용 샘플 데이터를 준비한 뒤 테스트 코드를 실행한다.
     */
    fun AbstractExposedTest.withJacksonBArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: JacksonSchema.JacksonBArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = JacksonSchema.JacksonBArrayTable

        withTables(testDB, tester) {
            val singleId = tester.insertAndGetId {
                it[tester.groups] = UserGroup(listOf(User("A", "Team A")))
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
