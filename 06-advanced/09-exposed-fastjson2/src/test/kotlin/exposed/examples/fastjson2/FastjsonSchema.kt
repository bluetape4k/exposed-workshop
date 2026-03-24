package exposed.examples.fastjson2

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.core.fastjson2.fastjson
import io.bluetape4k.exposed.core.fastjson2.fastjsonb
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assumptions

/**
 * Fastjson2 JSON/JSONB 컬럼 테스트에서 공통으로 사용하는 스키마/엔티티/픽스처를 제공합니다.
 */
@Suppress("UnusedReceiverParameter")
object FastjsonSchema {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS fastjson_table (
     *      id SERIAL PRIMARY KEY,
     *      fastjson_column JSON NOT NULL
     * )
     * ```
     */
    object FastjsonTable: IntIdTable("fastjson_table") {
        val fastjsonColumn = fastjson<DataHolder>("fastjson_column")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS fastjson_b_table (
     *      id SERIAL PRIMARY KEY,
     *      fastjson_b_column JSONB NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS fastjson_b_table (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      fastjson_b_column JSON NOT NULL
     * );
     * ```
     */
    object FastjsonBTable: IntIdTable("fastjson_b_table") {
        val fastjsonBColumn = fastjsonb<DataHolder>("fastjson_b_column")
    }

    /**
     * Fastjson `json` 컬럼 테스트용 DAO 엔티티입니다.
     */
    class FastjsonEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<FastjsonEntity>(FastjsonTable)

        var fastjsonColumn by FastjsonTable.fastjsonColumn

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
    }

    /**
     * Fastjson `jsonb` 컬럼 테스트용 DAO 엔티티입니다.
     */
    class FastjsonBEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<FastjsonBEntity>(FastjsonBTable)

        var fastjsonBColumn by FastjsonBTable.fastjsonBColumn

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS fastjson_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSON NOT NULL,
     *      numbers JSON NOT NULL
     * );
     * ```
     */
    object FastjsonArrayTable: IntIdTable("fastjson_arrays") {
        val groups = fastjson<UserGroup>("groups")
        val numbers = fastjson<IntArray>("numbers")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS fastjson_b_arrays (
     *      id SERIAL PRIMARY KEY,
     *      "groups" JSONB NOT NULL,
     *      numbers JSONB NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS fastjson_b_arrays (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `groups` JSON NOT NULL,
     *      numbers JSON NOT NULL
     * );
     * ```
     */
    object FastjsonBArrayTable: IntIdTable("fastjson_b_arrays") {
        val groups = fastjsonb<UserGroup>("groups")
        val numbers = fastjsonb<IntArray>("numbers")
    }

    /**
     * JSON 컬럼 입출력 검증에 사용하는 샘플 데이터입니다.
     */
    data class DataHolder(val user: User, val logins: Int, val active: Boolean, val team: String?)

    /**
     * 사용자 정보를 표현하는 JSON 하위 객체입니다.
     */
    data class User(val name: String, val team: String?)

    /**
     * 사용자 목록을 감싼 JSON 객체입니다.
     */
    data class UserGroup(val users: List<User>)

    /**
     * `FastjsonTable`을 생성하고 기본 레코드를 삽입한 뒤 테스트 본문을 실행합니다.
     */
    fun AbstractExposedTest.withFastjsonTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: FastjsonSchema.FastjsonTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = FastjsonSchema.FastjsonTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.fastjsonColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    /**
     * `FastjsonBTable`을 생성하고 기본 레코드를 삽입한 뒤 테스트 본문을 실행합니다.
     */
    fun AbstractExposedTest.withFastjsonBTable(
        testDB: TestDB,
        statement: JdbcTransaction.(tester: FastjsonSchema.FastjsonBTable, user1: User, data1: DataHolder) -> Unit,
    ) {
        val tester = FastjsonSchema.FastjsonBTable

        withTables(testDB, tester) {
            val user1 = User("Admin", null)
            val data1 = DataHolder(user1, 10, true, null)

            tester.insert {
                it[tester.fastjsonBColumn] = data1
            }

            statement(tester, user1, data1)
        }
    }

    /**
     * JSON 배열/객체 예제를 위한 `FastjsonArrayTable` 초기 데이터를 준비합니다.
     */
    fun AbstractExposedTest.withFastjsonArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: FastjsonSchema.FastjsonArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = FastjsonSchema.FastjsonArrayTable

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
     * JSONB 배열/객체 예제를 위한 `FastjsonBArrayTable` 초기 데이터를 준비합니다.
     */
    fun AbstractExposedTest.withFastjsonBArrays(
        testDB: TestDB,
        statement: JdbcTransaction.(
            tester: FastjsonSchema.FastjsonBArrayTable,
            singleId: EntityID<Int>,
            tripleId: EntityID<Int>,
        ) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in TestDB.ALL_H2_V1, "H2V1 does not support JSON arrays")

        val tester = FastjsonSchema.FastjsonBArrayTable

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
