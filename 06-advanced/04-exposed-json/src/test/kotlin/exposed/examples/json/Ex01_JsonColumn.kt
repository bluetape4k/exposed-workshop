package exposed.examples.json

import exposed.examples.json.JsonTestData.JsonArrayTable
import exposed.examples.json.JsonTestData.JsonEntity
import exposed.examples.json.JsonTestData.JsonTable
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.addLogger
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.json.Exists
import org.jetbrains.exposed.v1.json.contains
import org.jetbrains.exposed.v1.json.exists
import org.jetbrains.exposed.v1.json.extract
import org.jetbrains.exposed.v1.json.json
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * JSON 컬럼에 Kotlinx Serialization을 이용하여 JSON 객체를 저장/조회하는 예제
 */
@Suppress("DEPRECATION")
class Ex01_JsonColumn: AbstractExposedJsonTest() {

    companion object: KLogging()

    /**
     * Insert and Select JSON data
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS j_table (
     *      id SERIAL PRIMARY KEY,
     *      j_column JSON NOT NULL
     * );
     *
     * INSERT INTO j_table (j_column)
     * VALUES ({"user":{"name":"Admin","team":null},"logins":10,"active":true,"team":null});
     *
     * INSERT INTO j_table (j_column)
     * VALUES ({"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"});
     *
     * SELECT j_table.id, j_table.j_column
     *   FROM j_table
     *  WHERE j_table.id = 2
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select`(testDB: TestDB) {
        withJsonTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[jsonColumn] = newData
            }

            entityCache.clear()

            val newResult = tester.selectAll().where { tester.id eq newId }.singleOrNull()

            newResult?.get(JsonTable.jsonColumn) shouldBeEqualTo newData
        }
    }

    /**
     * Update with JSON
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with json`(testDB: TestDB) {
        withJsonTable(testDB) { tester, _, data1 ->
            tester.selectAll().single()[JsonTable.jsonColumn] shouldBeEqualTo data1

            /**
             * ```sql
             * UPDATE j_table
             *    SET j_column={"user":{"name":"Admin","team":null},"logins":10,"active":false,"team":"Update Team"}
             * ```
             */
            val updatedData = data1.copy(active = false, team = "Update Team")
            tester.update {
                it[jsonColumn] = updatedData
            }

            entityCache.clear()

            tester.selectAll().single()[JsonTable.jsonColumn] shouldBeEqualTo updatedData
        }
    }

    /**
     * Select with slice extract
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select with slice extract`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonTable(testDB) { tester, user1, data1 ->
            val pathPrefix = if (currentDialectTest is PostgreSQLDialect) "" else "."
            // SQLServer & Oracle return null if extracted JSON is not scalar
            val requiresScalar = currentDialectTest is SQLServerDialect || currentDialectTest is OracleDialect

            /**
             * ```sql
             * -- Postgres
             * SELECT JSON_EXTRACT_PATH(j_table.j_column, 'active')
             *   FROM j_table;
             *
             * -- MySQL V8
             * SELECT JSON_EXTRACT(j_table.j_column, "$.active")
             *   FROM j_table;
             * ```
             */
            val isActive = JsonTable.jsonColumn.extract<Boolean>("${pathPrefix}active", toScalar = requiresScalar)
            val result1 = tester.select(isActive).singleOrNull()
            result1?.get(isActive) shouldBeEqualTo data1.active

            /**
             * ```sql
             * -- Postgres
             * SELECT JSON_EXTRACT_PATH(j_table.j_column, 'user')
             *   FROM j_table
             *
             * -- MySQL V8
             * SELECT JSON_EXTRACT(j_table.j_column, "$.user")
             *   FROM j_table;
             * ```
             */
            val storedUser = JsonTable.jsonColumn.extract<User>("${pathPrefix}user", toScalar = requiresScalar)
            val result2 = tester.select(storedUser).singleOrNull()
            result2?.get(storedUser) shouldBeEqualTo user1

            /**
             * ```sql
             * -- Postgres
             * SELECT JSON_EXTRACT_PATH_TEXT(j_table.j_column, 'user', 'name')
             *   FROM j_table;
             *
             * -- MySQL V8
             * SELECT JSON_UNQUOTE(JSON_EXTRACT(j_table.j_column, "$.user.name"))
             *   FROM j_table
             * ```
             */
            val path = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("user", "name")
                else -> arrayOf(".user.name")
            }
            val username = JsonTable.jsonColumn.extract<String>(*path)
            val result3 = tester.select(username).singleOrNull()
            result3?.get(username) shouldBeEqualTo user1.name
        }
    }

    /**
     * Select where with extract
     *
     * ```sql
     * -- Postgres
     * SELECT j_table.id
     *   FROM j_table
     *  WHERE CAST(JSON_EXTRACT_PATH_TEXT(j_table.j_column, 'logins') AS INT) >= 1000
     *
     * -- MySQL V8
     * SELECT j_table.id
     *   FROM j_table
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(j_table.j_column, "$.logins")) >= 1000
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select where with extract`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonTable(testDB) { tester, user1, data1 ->
            val newId = tester.insertAndGetId {
                it[jsonColumn] = data1.copy(logins = 1000)
            }

            // Postgres requires type casting to compare json field as integer value in DB
            val logins = when (currentDialectTest) {
                is PostgreSQLDialect -> JsonTable.jsonColumn.extract<Int>("logins").castTo(IntegerColumnType())

                else -> JsonTable.jsonColumn.extract<Int>(".logins")
            }

            val tooManyLogins = logins greaterEq 1000
            val result = tester.select(tester.id).where { tooManyLogins }.singleOrNull()
            result?.get(tester.id) shouldBeEqualTo newId
        }
    }

    /**
     * Kotlinx Serialization의 Serializable 이 아니면 예외가 발생합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `with non serializable class`(testDB: TestDB) {
        data class Fake(val name: Int)

        withDb(testDB) {
            expectException<SerializationException> {
                // Throws with message: Serializer for class 'Fake' is not found.
                // Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
                object: Table("tester") {
                    val jCol = json<Fake>("J_col", Json)
                }
            }
        }
    }

    /**
     * DAO Entity 에서 JSON 컬럼을 사용 하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Functions with Json Column`(testDB: TestDB) {
        val dataTable = JsonTable
        val dataEntity = JsonEntity

        withTables(testDB, dataTable) {
            val jsonA = DataHolder(User("Admin", "Alpha"), 10, true, null)

            /**
             * ```sql
             * INSERT INTO j_table (j_column)
             * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null})
             * ```
             */
            val newUser = dataEntity.new {
                jsonColumn = jsonA
            }

            entityCache.clear()

            /**
             * ```sql
             * SELECT j_table.id, j_table.j_column
             *   FROM j_table
             *  WHERE j_table.id = 1
             * ```
             */
            dataEntity.findById(newUser.id)?.jsonColumn shouldBeEqualTo jsonA

            /**
             * ```sql
             * UPDATE j_table
             *    SET j_column={"user":{"name":"Lead","team":"Beta"},"logins":10,"active":true,"team":null}
             * ```
             */
            val updatedJson = jsonA.copy(user = User("Lead", "Beta"))
            dataTable.update {
                it[jsonColumn] = updatedJson
            }

            dataEntity.all().single().jsonColumn shouldBeEqualTo updatedJson

            // Json Path
            Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

            /**
             * Insert new entity
             *
             * ```sql
             * INSERT INTO j_table (j_column)
             * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null})
             * ```
             */
            dataEntity.new { jsonColumn = jsonA }

            /**
             * JSON_EXTRACT_PATH_TEXT 를 이용하여 특정 필드를 비교할 수 있습니다.
             *
             * ```sql
             * -- Postgres
             * SELECT j_table.id, j_table.j_column
             *   FROM j_table
             *  WHERE JSON_EXTRACT_PATH_TEXT(j_table.j_column, 'user', 'team') LIKE 'B%'
             * ```
             */
            val path = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("user", "team")
                else -> arrayOf(".user.team")
            }
            val userTeam = JsonTable.jsonColumn.extract<String>(*path)
            val userInTeamB = dataEntity.find { userTeam like "B%" }.single()

            userInTeamB.jsonColumn shouldBeEqualTo updatedJson
        }
    }

    private val jsonContainsSupported = TestDB.ALL_POSTGRES + TestDB.MYSQL_V5 + TestDB.MYSQL_V8

    /**
     * JSON 컬럼의 내용 중 PATH 에 해당하는 데이터가 포함되어 있는지 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json contains`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in jsonContainsSupported }

        withJsonTable(testDB) { tester, user1, data1 ->
            /**
             * Insert new entity
             * Postgres:
             * ```sql
             * INSERT INTO j_table (j_column)
             * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null})
             * ```
             */
            val alphaTeamUser = user1.copy(team = "Alpha")
            val newId = tester.insertAndGetId {
                it[jsonColumn] = data1.copy(user = alphaTeamUser)
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT j_table.id, j_table.j_column
             *   FROM j_table
             *  WHERE j_table.j_column::jsonb @> '{"active":false}'::jsonb
             *
             * -- MySQL V8
             * SELECT j_table.id, j_table.j_column
             *   FROM j_table
             *  WHERE JSON_CONTAINS(j_table.j_column, '{"active":false}')
             * ```
             */
            val userIsInactive = JsonTable.jsonColumn.contains("""{"active":false}""")
            val result = tester.selectAll().where { userIsInactive }.toList()
            result.shouldBeEmpty()

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM j_table
             *  WHERE j_table.j_column::jsonb @> '{"user":{"name":"Admin","team":"Alpha"}}'::jsonb
             *
             * -- MySQL V8
             * SELECT COUNT(*)
             *   FROM j_table
             *  WHERE JSON_CONTAINS(j_table.j_column, '{"user":{"name":"Admin","team":"Alpha"}}')
             * ```
             */
            val alphaTreamUserAsJson = """{"user":${Json.Default.encodeToString(alphaTeamUser)}}"""
            val userIsInAlphaTeam = JsonTable.jsonColumn.contains(stringLiteral(alphaTreamUserAsJson))
            tester.selectAll().where { userIsInAlphaTeam }.count() shouldBeEqualTo 1L

            // test target contains candidate at specified path
            if (testDB in TestDB.ALL_MYSQL_LIKE) {
                /**
                 * 아쉽게도 Postgres 에서는 JSON Path 의 값을 비교하는 방식은 지원하지 않습니다.
                 * ```sql
                 * -- MySQL V8
                 * SELECT j_table.id, j_table.j_column
                 *   FROM j_table
                 *  WHERE JSON_CONTAINS(j_table.j_column, '"Alpha"', '$.user.team')
                 * ```
                 */
                // Path 를 이용하여 특정 필드를 비교할 수 있습니다.
                val userIsInAlphaTeam2 = JsonTable.jsonColumn.contains("\"Alpha\"", path = ".user.team")
                val alphaTeamUsers = tester.selectAll().where { userIsInAlphaTeam2 }
                alphaTeamUsers.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * JSON 객체에 대해 EXISTS 조건을 사용하여 특정 경로에 데이터가 존재하는지 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json exists`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonTable(testDB) { tester, user1, data1 ->
            /**
             * ```sql
             * INSERT INTO j_table (j_column)
             * VALUES ({"user":{"name":"Admin","team":"A"},"logins":1000,"active":true,"team":null})
             * ```
             */
            val maximumLogins = 1000
            val teamA = "A"
            val newId = tester.insertAndGetId {
                it[jsonColumn] = data1.copy(user = data1.user.copy(team = teamA), logins = maximumLogins)
            }

            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            /**
             * test data at path root `$` exists by providing no path arguments
             *
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM j_table
             *  WHERE JSONB_PATH_EXISTS(CAST(j_table.j_column as jsonb), '$')
             *
             *  -- MySQL V8
             *  SELECT COUNT(*)
             *    FROM j_table
             *   WHERE JSON_CONTAINS_PATH(j_table.j_column, 'one', '$')
             * ```
             */
            val hasAnyData = JsonTable.jsonColumn.exists(optional = optional)
            tester.selectAll().where { hasAnyData }.count() shouldBeEqualTo 2L

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*) FROM j_table
             *  WHERE JSONB_PATH_EXISTS(CAST(j_table.j_column as jsonb), '$.fakeKey')
             *
             *  -- MySQL V8
             *  SELECT COUNT(*) FROM j_table
             *   WHERE JSON_CONTAINS_PATH(j_table.j_column, 'one', '$.fakeKey')
             * ```
             */
            val hasFakeKey = JsonTable.jsonColumn.exists(".fakeKey", optional = optional)
            tester.selectAll().where { hasFakeKey }.count() shouldBeEqualTo 0L

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*) FROM j_table
             *  WHERE JSONB_PATH_EXISTS(CAST(j_table.j_column as jsonb), '$.logins')
             *
             * -- MySQL V8
             * SELECT COUNT(*) FROM j_table
             * WHERE JSON_CONTAINS_PATH(j_table.j_column, 'one', '$.logins')
             * ```
             */
            val hasLogins = JsonTable.jsonColumn.exists(".logins", optional = optional)
            tester.selectAll().where { hasLogins }.count() shouldBeEqualTo 2L

            // test data at path exists with filter condition & optional arguments
            val testDialect = currentDialectTest
            if (testDialect is PostgreSQLDialect) {
                /**
                 * Postgres:
                 * ```sql
                 * SELECT j_table.id
                 *   FROM j_table
                 *  WHERE JSONB_PATH_EXISTS(CAST(j_table.j_column as jsonb), '$.logins ? (@ == 1000)')
                 * ```
                 */
                val filterPath = ".logins ? (@ == $maximumLogins)"
                val hasMaxLogins: Exists = JsonTable.jsonColumn.exists(filterPath)
                val usersWithMaxLogin: Query = tester.select(tester.id).where { hasMaxLogins }
                usersWithMaxLogin.single()[tester.id] shouldBeEqualTo newId

                /**
                 * Postgres:
                 * ```sql
                 * SELECT j_table.id
                 *   FROM j_table
                 *  WHERE JSONB_PATH_EXISTS(CAST(j_table.j_column as jsonb), '$.user.team ? (@ == $team)', '{"team":"A"}')
                 *
                 * ```
                 */
                val jsonPath = ".user.team ? (@ == \$team)"
                val optionalArg = """{"team":"$teamA"}"""
                val isOnTeamA: Exists = JsonTable.jsonColumn.exists(jsonPath, optional = optionalArg)
                val usersOnTeamA: Query = tester.select(tester.id).where { isOnTeamA }
                usersOnTeamA.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * JSON 컬럼의 세부 데이터에 대해 검색 조건으로 사용한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json extract with arrays`(testDB: TestDB) {

        withJsonArrays(testDB) { tester, singleId, _ ->
            /**
             * ```sql
             * -- Postgres
             * SELECT j_arrays.id, j_arrays."groups", j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSON_EXTRACT_PATH_TEXT(j_arrays."groups", 'users', '0', 'team') = 'Team A';
             *
             * -- MySQL V8
             * SELECT j_arrays.id, j_arrays.`groups`, j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSON_UNQUOTE(JSON_EXTRACT(j_arrays.`groups`, "$.users[0].team")) = 'Team A'
             * ```
             */
            val path1 = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("users", "0", "team")
                else -> arrayOf(".users[0].team")
            }
            val firstIsOnTeamA = JsonArrayTable.groups.extract<String>(*path1) eq "Team A"
            tester.selectAll().where { firstIsOnTeamA }.single()[tester.id] shouldBeEqualTo singleId

            /**
             * ```sql
             * -- Postgres
             * SELECT JSON_EXTRACT_PATH_TEXT(j_arrays.numbers, '0') FROM j_arrays;
             *
             * --- MySQL V8
             * SELECT JSON_UNQUOTE(JSON_EXTRACT(j_arrays.numbers, "$[0]")) FROM j_arrays;
             * ```
             */
            // older MySQL and MariaDB versions require non-scalar extracted value from JSON Array
            val toScala = testDB != TestDB.MYSQL_V5
            val path2 = if (currentDialectTest is PostgreSQLDialect) "0" else "[0]"
            val firstNumber = JsonArrayTable.numbers.extract<Int>(path2, toScalar = toScala)
            tester.select(firstNumber).map { it[firstNumber] } shouldBeEqualTo listOf(100, 3)
        }
    }

    /**
     * JSON ARRAY 컬럼에 대해 `Constains` 조건을 사용하여 특정 데이터가 포함되어 있는지 확인합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json contains with array`(testDB: TestDB) {
        withJsonArrays(testDB) { tester, _, tripleId ->
            /**
             * ```sql
             * -- Postgres
             * SELECT j_arrays.id, j_arrays."groups", j_arrays.numbers
             *   FROM j_arrays
             *  WHERE j_arrays.numbers::jsonb @> '[3, 5]'::jsonb
             *
             * -- MySQL V8
             * SELECT j_arrays.id, j_arrays.`groups`, j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSON_CONTAINS(j_arrays.numbers, '[3, 5]')
             * ```
             */
            val hasSmallNumbers = JsonArrayTable.numbers.contains("[3, 5]")
            tester.selectAll().where { hasSmallNumbers }.single()[tester.id] shouldBeEqualTo tripleId

            /**
             * ```sql
             * -- MySQL V8
             * SELECT j_arrays.id, j_arrays.`groups`, j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSON_CONTAINS(j_arrays.`groups`, '"B"', '$.users[0].name')
             * ```
             */
            if (testDB in TestDB.ALL_MYSQL_LIKE) {
                val hasSmallNumbers2 = JsonArrayTable.groups.contains("\"B\"", path = ".users[0].name")
                tester.selectAll().where { hasSmallNumbers2 }.single()[tester.id] shouldBeEqualTo tripleId
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json exists with array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonArrays(testDB) { tester, singleId, tripleId ->
            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            /**
             * ```sql
             * -- Postgres
             * SELECT j_arrays.id, j_arrays."groups", j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSONB_PATH_EXISTS(CAST(j_arrays."groups" as jsonb), '$.users[1]')
             *
             * -- MySQL V8
             * SELECT j_arrays.id, j_arrays.`groups`, j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSON_CONTAINS_PATH(j_arrays.`groups`, 'one', '$.users[1]')
             * ```
             */
            val hasMultipleUsers = JsonArrayTable.groups.exists(".users[1]", optional = optional)
            tester.selectAll().where { hasMultipleUsers }.single()[tester.id] shouldBeEqualTo tripleId

            /**
             * ```sql
             * -- Postgres
             * SELECT j_arrays.id, j_arrays."groups", j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSONB_PATH_EXISTS(CAST(j_arrays.numbers as jsonb), '$[2]')
             *
             * -- MySQL V8
             * SELECT j_arrays.id, j_arrays.`groups`, j_arrays.numbers
             *   FROM j_arrays
             *  WHERE JSON_CONTAINS_PATH(j_arrays.numbers, 'one', '$[2]')
             * ```
             */
            val hasAtLeast3Numbers = JsonArrayTable.numbers.exists("[2]", optional = optional)
            tester.selectAll().where { hasAtLeast3Numbers }.single()[tester.id] shouldBeEqualTo tripleId
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS iterables (
     *      id SERIAL PRIMARY KEY,
     *      user_list JSON NOT NULL,
     *      user_set JSON NOT NULL,
     *      user_array JSON NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json contains with iterables`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in jsonContainsSupported }

        val iterables = object: IntIdTable("iterables") {
            val userList = json<List<User>>("user_list", Json.Default)
            val userSet = json<Set<User>>("user_set", Json.Default)
            val userArray = json<Array<User>>("user_array", Json.Default)
        }

        fun selectIdWhere(condition: SqlExpressionBuilder.() -> Op<Boolean>): List<EntityID<Int>> {
            val query = iterables.select(iterables.id).where(SqlExpressionBuilder.condition())
            return query.map { it[iterables.id] }
        }

        withTables(testDB, iterables) {
            val user1 = User("A", "Team A")
            val user2 = User("B", "Team B")

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO iterables (user_list, user_set, user_array)
             * VALUES (
             *      [{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}],
             *      [{"name":"A","team":"Team A"}],
             *      [{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}]
             * );
             */
            val id1 = iterables.insertAndGetId {
                it[userList] = listOf(user1, user2)
                it[userSet] = setOf(user1)
                it[userArray] = arrayOf(user1, user2)
            }

            /**
             * ```sql
             * INSERT INTO iterables (user_list, user_set, user_array)
             * VALUES (
             *      [{"name":"B","team":"Team B"}],
             *      [{"name":"B","team":"Team B"},{"name":"A","team":"Team A"}],
             *      [{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}]
             * );
             * ```
             */
            val id2 = iterables.insertAndGetId {
                it[userList] = listOf(user2)
                it[userSet] = setOf(user2, user1)
                it[userArray] = arrayOf(user1, user2)
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT iterables.id FROM iterables
             *  WHERE iterables.user_list::jsonb @> '[{"name":"A","team":"Team A"}]'::jsonb;
             *
             * SELECT iterables.id FROM iterables
             *  WHERE iterables.user_set::jsonb @> '[{"name":"B","team":"Team B"}]'::jsonb;
             *
             * SELECT iterables.id FROM iterables
             *  WHERE iterables.user_array::jsonb @> '[{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}]'::jsonb;
             *
             * -- MySQL V8
             * SELECT iterables.id FROM iterables
             *  WHERE JSON_CONTAINS(iterables.user_list, '[{"name":"A","team":"Team A"}]');
             *
             * SELECT iterables.id FROM iterables
             *  WHERE JSON_CONTAINS(iterables.user_set, '[{"name":"B","team":"Team B"}]');
             *
             * SELECT iterables.id FROM iterables
             *  WHERE JSON_CONTAINS(iterables.user_array, '[{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}]');
             * ```
             */
            selectIdWhere { iterables.userList.contains(listOf(user1)) } shouldBeEqualTo listOf(id1)
            selectIdWhere { iterables.userSet.contains(setOf(user2)) } shouldBeEqualTo listOf(id2)
            selectIdWhere { iterables.userArray.contains(arrayOf(user1, user2)) } shouldBeEqualTo listOf(id1, id2)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json with defaults`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS default_tester (
         *      user_1 JSON DEFAULT '{"name":"UNKNOWN","team":"UNASSIGNED"}'::json NOT NULL,
         *      user_2 JSON NOT NULL
         * );
         * ```
         */
        val defaultUser = User("UNKNOWN", "UNASSIGNED")
        val defaultTester = object: Table("default_tester") {
            val user1 = json<User>("user_1", Json.Default).default(defaultUser)
            val user2 = json<User>("user_2", Json.Default).clientDefault { defaultUser }
        }

        withDb(testDB) {
            if (testDB == TestDB.MYSQL_V5) {
                expectException<UnsupportedByDialectException> {
                    SchemaUtils.createMissingTablesAndColumns(defaultTester)
                }
            } else {
                SchemaUtils.createMissingTablesAndColumns(defaultTester)
                defaultTester.exists().shouldBeTrue()

                // ensure defaults match returned metadata defaults
                val alters = SchemaUtils.statementsRequiredToActualizeScheme(defaultTester)
                alters.shouldBeEmpty()

                defaultTester.insert { }

                defaultTester.selectAll().single().also {
                    it[defaultTester.user1] shouldBeEqualTo defaultUser
                    it[defaultTester.user2] shouldBeEqualTo defaultUser
                }
            }

            SchemaUtils.drop(defaultTester)
        }
    }

    /**
     * [StdOutSqlLogger] 에 JSON 컬럼 정보를 출력하는 예
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS iterables_tester (
     *      user_list JSON NOT NULL,
     *      int_list JSON NOT NULL,
     *      user_array JSON NOT NULL,
     *      int_array JSON NOT NULL
     * );
     *
     * INSERT INTO iterables_tester (user_list, int_list, user_array, int_array)
     * VALUES (
     *      [{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}],
     *      [1,2,3],
     *      [{"name":"A","team":"Team A"},{"name":"B","team":"Team B"}],
     *      [4,5,6]
     * );
     * ```
     */
    @OptIn(ExperimentalSerializationApi::class)
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `logger with json collections`(testDB: TestDB) {
        val iterables = object: Table("iterables_tester") {
            val userList = json<List<User>>("user_list", Json.Default, ListSerializer(User.serializer()))
            val intList = json<List<Int>>("int_list", Json.Default)
            val userArray = json<Array<User>>("user_array", Json.Default, ArraySerializer(User.serializer()))
            val intArray = json<IntArray>("int_array", Json.Default)
        }

        withTables(testDB, iterables) {
            // the logger is left in to test that it does not throw ClassCastException on insertion of iterables
            addLogger(StdOutSqlLogger)

            val user1 = User("A", "Team A")
            val user2 = User("B", "Team B")
            val integerList = listOf(1, 2, 3)
            val integerArray = intArrayOf(4, 5, 6)

            iterables.insert {
                it[userList] = listOf(user1, user2)
                it[intList] = integerList
                it[userArray] = arrayOf(user1, user2)
                it[intArray] = integerArray
            }

            val result = iterables.selectAll().single()
            result[iterables.userList] shouldBeEqualTo listOf(user1, user2)
            result[iterables.intList] shouldBeEqualTo integerList
            result[iterables.userArray] shouldBeEqualTo arrayOf(user1, user2)
            result[iterables.intArray] shouldBeEqualTo integerArray
        }
    }

    /**
     * JSON 컬럼이 NULLABLE인 경우
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json with nullable column`(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS nullable_tester (
         *      id SERIAL PRIMARY KEY,
         *      "user" JSON NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("nullable_tester") {
            val user = json<User>("user", Json.Default).nullable()
        }

        withTables(testDB, tester) {
            /**
             * ```sql
             * INSERT INTO nullable_tester ("user") VALUES (NULL)
             * ```
             */
            val nullId = tester.insertAndGetId {
                it[user] = null
            }

            /**
             * ```sql
             * INSERT INTO nullable_tester ("user")
             * VALUES ({"name":"A","team":"Team A"})
             * ```
             */
            val nonNullId = tester.insertAndGetId {
                it[user] = User("A", "Team A")
            }

            flushCache()

            val result1 = tester.select(tester.user).where { tester.id eq nullId }.single()
            result1[tester.user].shouldBeNull()

            val result2 = tester.select(tester.user).where { tester.id eq nonNullId }.single()
            result2[tester.user].shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json with upsert`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withJsonTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[jsonColumn] = newData
            }

            /**
             * Upsert with JSON
             *
             * ```sql
             * -- Postgres
             * INSERT INTO j_table (id, j_column)
             * VALUES (2, {"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":false,"team":"A"})
             * ON CONFLICT (id) DO UPDATE SET j_column=EXCLUDED.j_column;
             *
             * -- MySQL V8
             * INSERT INTO j_table (id, j_column)
             * VALUES (2, {"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":false,"team":"A"})
             * AS NEW ON DUPLICATE KEY UPDATE id=NEW.id, j_column=NEW.j_column;
             * ```
             */
            val newData2 = newData.copy(active = false)
            tester.upsert {
                it[tester.id] = newId
                it[jsonColumn] = newData2
            }

            val newResult = tester.selectAll().where { tester.id eq newId }.singleOrNull()
            newResult?.get(JsonTable.jsonColumn) shouldBeEqualTo newData2
        }
    }

    /**
     * JSON 컬럼에 `transform` 함수를 사용하여 데이터 변환을 적용하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json with transformer`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val numbers: Column<DoubleArray> = json<IntArray>(
                "numbers",
                Json.Default
            ).transform(
                wrap = { DoubleArray(it.size) { i -> it[i].toDouble() } },
                unwrap = { IntArray(it.size) { i -> it[i].toInt() } })
        }

        withTables(testDB, tester) {

            /**
             * ```sql
             * INSERT INTO tester (numbers) VALUES ([1,2,3])
             * ```
             */
            val data = doubleArrayOf(1.0, 2.0, 3.0)
            tester.insert {
                it[numbers] = data
            }

            tester.selectAll().single()[tester.numbers] shouldBeEqualTo data
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json as default`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        val defaultUser = User("name", "team")
        val tester = object: IntIdTable("testJsonAsDefault") {
            val value = json<User>("value", Json.Default).default(defaultUser)
        }
        val testerDatabaseGenerated = object: IntIdTable("testJsonAsDefault") {
            val value = json<User>("value", Json.Default).databaseGenerated()
        }

        // MySQL versions prior to 8.0.13 do not accept default values on JSON columns
        withTables(testDB, tester) {
            testerDatabaseGenerated.insert { }

            val value = testerDatabaseGenerated.selectAll().single()[tester.value]
            value shouldBeEqualTo defaultUser
        }
    }
}
