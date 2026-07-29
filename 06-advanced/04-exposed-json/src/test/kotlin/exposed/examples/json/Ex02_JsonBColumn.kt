package exposed.examples.json

import exposed.examples.json.JsonTestData.JsonBArrayTable
import exposed.examples.json.JsonTestData.JsonBEntity
import exposed.examples.json.JsonTestData.JsonBTable
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
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
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
import org.jetbrains.exposed.v1.json.jsonb
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * `jsonb` 컬럼 사용 예제
 */
class Ex02_JsonBColumn: AbstractExposedJsonTest() {

    companion object: KLogging()

    private val binaryJsonNotSupportedDB = emptyList<TestDB>()

    /**
     * JSONB 컬럼 생성 및 조회 테스트
     *
     * ```sql
     * -- Postgres 예:
     * INSERT INTO j_b_table (j_b_column)
     * VALUES ({"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":true,"team":"A"});
     *
     * SELECT j_b_table.id, j_b_table.j_b_column
     *   FROM j_b_table
     *  WHERE j_b_table.id = 2;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select`(testDB: TestDB) {
        withJsonBTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[JsonBTable.jsonBColumn] = newData
            }

            val newResult = tester.selectAll().where { tester.id eq newId }.singleOrNull()
            newResult?.get(JsonBTable.jsonBColumn) shouldBeEqualTo newData
        }
    }

    /**
     * JSONB 컬럼 값을 갱신한다.
     *
     * ```sql
     * -- Postgres 예:
     * UPDATE j_b_table
     *   SET j_b_column={"user":{"name":"Admin","team":null},"logins":10,"active":false,"team":null}
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update jsonb`(testDB: TestDB) {
        withJsonBTable(testDB) { tester, _, data1 ->
            tester.selectAll().single()[JsonBTable.jsonBColumn] shouldBeEqualTo data1

            val updatedData = data1.copy(active = false)
            tester.update {
                it[JsonBTable.jsonBColumn] = updatedData
            }

            tester.selectAll().single()[JsonBTable.jsonBColumn] shouldBeEqualTo updatedData
        }
    }

    /**
     * JSONB 컬럼의 배열 부분을 `Slice` 로 추출하는 테스트
     *
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select with slice extract`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonBTable(testDB) { tester, user1, data1 ->
            val pathPrefix = if (currentDialectTest is PostgreSQLDialect) "" else "."
            // SQLServer와 Oracle은 추출된 JSON 값이 스칼라가 아니면 null을 반환한다.
            val requiresScalar = currentDialectTest is SQLServerDialect || currentDialectTest is OracleDialect

            /**
             * ```sql
             * -- Postgres
             * SELECT JSONB_EXTRACT_PATH(j_b_table.j_b_column, 'active')
             *   FROM j_b_table
             *
             * -- MySQL V8
             * SELECT JSON_EXTRACT(j_b_table.j_b_column, "$.active")
             *   FROM j_b_table
             * ```
             */
            val isActive = JsonBTable.jsonBColumn.extract<Boolean>("${pathPrefix}active", toScalar = requiresScalar)
            val result1 = tester.select(isActive).singleOrNull()
            result1?.get(isActive) shouldBeEqualTo data1.active

            /**
             * ```sql
             * -- Postgres
             * SELECT JSONB_EXTRACT_PATH(j_b_table.j_b_column, 'user')
             *   FROM j_b_table
             *
             * -- MySQL V8
             * SELECT JSON_EXTRACT(j_b_table.j_b_column, "$.user")
             *   FROM j_b_table
             * ```
             */
            val storedUser = JsonBTable.jsonBColumn.extract<User>("${pathPrefix}user", toScalar = requiresScalar)
            val result2 = tester.select(storedUser).singleOrNull()
            result2?.get(storedUser) shouldBeEqualTo user1

            /**
             * ```sql
             * -- Postgres
             * SELECT JSONB_EXTRACT_PATH_TEXT(j_b_table.j_b_column, 'user', 'name')
             *   FROM j_b_table
             *
             * -- MySQL V8
             * SELECT JSON_UNQUOTE(JSON_EXTRACT(j_b_table.j_b_column, "$.user.name"))
             *   FROM j_b_table
             * ```
             */
            val path = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("user", "name")
                else -> arrayOf(".user.name")
            }
            val username = JsonBTable.jsonBColumn.extract<String>(*path)
            val result3 = tester.select(username).singleOrNull()
            result3?.get(username) shouldBeEqualTo user1.name
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select where with extract`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonBTable(testDB) { tester, user1, data1 ->
            val newId = tester.insertAndGetId {
                it[JsonBTable.jsonBColumn] = data1.copy(logins = 1000)
            }

            // Postgres는 json 필드를 DB 내부에서 정수 값으로 비교하려면 타입 캐스팅이 필요하다.
            val logins = when (currentDialectTest) {
                is PostgreSQLDialect ->
                    JsonBTable.jsonBColumn.extract<Int>("logins").castTo(IntegerColumnType())

                else ->
                    JsonBTable.jsonBColumn.extract<Int>(".logins")
            }

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT j_b_table.id
             *   FROM j_b_table
             *  WHERE CAST(JSONB_EXTRACT_PATH_TEXT(j_b_table.j_b_column, 'logins') AS INT) >= 1000;
             *
             * -- MySQL V8 예:
             * SELECT j_b_table.id
             *   FROM j_b_table
             *  WHERE JSON_UNQUOTE(JSON_EXTRACT(j_b_table.j_b_column, "$.logins")) >= 1000;
             * ```
             */
            val tooManyLogins = logins greaterEq 1000
            val result = tester.select(tester.id)
                .where { tooManyLogins }
                .singleOrNull()
            result?.get(tester.id) shouldBeEqualTo newId
        }
    }

    /**
     * Kotlinx Serialization 의 `Serializable` 어노테이션이 없는 클래스는 사용할 수 없습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `with non serializable class`(testDB: TestDB) {
        data class Fake(val name: Int)

        withDb(testDB) {
            expectException<SerializationException> {
                // 다음 메시지로 예외가 발생한다: Serializer for class 'Fake' is not found.
                // 해당 클래스에 '@Serializable'을 지정하고 serialization compiler plugin이 적용되었는지 확인해야 한다.
                object: Table("tester") {
                    val jCol = jsonb<Fake>("J_col", Json)
                }
            }
        }
    }

    /**
     * DAO 엔티티를 사용하여 INSERT, UPDATE, SELECT 테스트
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Functions with Json Column`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        val dataTable = JsonBTable
        val dataEntity = JsonBEntity

        withTables(testDB, dataTable) {
            /**
             * 새 엔티티를 삽입한다.
             * ```sql
             * INSERT INTO j_b_table (j_b_column)
             * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null})
             * ```
             */
            val jsonA = DataHolder(User("Admin", "Alpha"), 10, true, null)
            val newUser = dataEntity.new {
                jsonBColumn = jsonA
            }

            entityCache.clear()

            dataEntity.findById(newUser.id)?.jsonBColumn shouldBeEqualTo jsonA
            dataEntity.findById(-1).shouldBeNull()

            /**
             * DSL 방식으로 값을 갱신한다.
             *
             * ```sql
             * -- Postgres 예:
             * UPDATE j_b_table
             *   SET j_b_column={"user":{"name":"Lead","team":"Beta"},"logins":10,"active":true,"team":null}
             * ```
             */
            val updatedJson = jsonA.copy(user = User("Lead", "Beta"))
            dataTable.update {
                it[jsonBColumn] = updatedJson
            }

            dataEntity.all().single().jsonBColumn shouldBeEqualTo updatedJson

            // JSON 경로 조건을 검증한다.
            Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

            /**
             * 새 엔티티를 삽입한다.
             *
             * ```sql
             * INSERT INTO j_b_table (j_b_column)
             * VALUES ({"user":{"name":"Admin","team":"Alpha"},"logins":10,"active":true,"team":null})
             * ```
             */
            dataEntity.new { jsonBColumn = jsonA }
            val path = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("user", "team")
                else -> arrayOf(".user.team")
            }

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT j_b_table.id, j_b_table.j_b_column
             *   FROM j_b_table
             *  WHERE JSONB_EXTRACT_PATH_TEXT(j_b_table.j_b_column, 'user', 'team') LIKE 'B%';
             *
             * -- MySQL V8 예:
             * SELECT j_b_table.id, j_b_table.j_b_column
             *   FROM j_b_table
             *  WHERE JSON_UNQUOTE(JSON_EXTRACT(j_b_table.j_b_column, "$.user.team")) LIKE 'B%';
             * ```
             */
            val userTeam = JsonBTable.jsonBColumn.extract<String>(*path)
            val userInTeamB = dataEntity.find { userTeam like "B%" }.single()

            userInTeamB.jsonBColumn shouldBeEqualTo updatedJson
        }
    }

    /**
     * JSONB 컬럼의 객체 내부의 속성을 잉용하여 검색하는 테스트
     *
     * ```sql
     * -- Postgres 예:
     * SELECT j_b_table.id, j_b_table.j_b_column
     *   FROM j_b_table
     *  WHERE j_b_table.j_b_column @> '{"active":false}';
     *
     * SELECT COUNT(*)
     *   FROM j_b_table
     *  WHERE j_b_table.j_b_column @> '{"user":{"name":"Admin","team":"Alpha"}}';
     * ```
     * ```sql
     * -- MySQL V8 예:
     * SELECT j_b_table.id, j_b_table.j_b_column
     *   FROM j_b_table
     *  WHERE JSON_CONTAINS(j_b_table.j_b_column, '{"active":false}');
     *
     * SELECT COUNT(*)
     *   FROM j_b_table
     *  WHERE JSON_CONTAINS(j_b_table.j_b_column, '{"user":{"name":"Admin","team":"Alpha"}}');
     *
     * SELECT j_b_table.id, j_b_table.j_b_column
     *   FROM j_b_table
     *  WHERE JSON_CONTAINS(j_b_table.j_b_column, '"Alpha"', '$.user.team');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb contains`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonBTable(testDB) { tester, user1, data1 ->
            val alphaTeamUser = user1.copy(team = "Alpha")
            val newId = tester.insertAndGetId {
                it[JsonBTable.jsonBColumn] = data1.copy(user = alphaTeamUser)
            }

            val userIsInactive = JsonBTable.jsonBColumn.contains("""{"active":false}""")
            val result = tester.selectAll().where { userIsInactive }.toList()
            result.shouldBeEmpty()

            val alphaTreamUserAsJson = """{"user":${Json.encodeToString(alphaTeamUser)}}"""
            val userIsInAlphaTeam = JsonBTable.jsonBColumn.contains(stringLiteral(alphaTreamUserAsJson))
            tester.selectAll().where { userIsInAlphaTeam }.count() shouldBeEqualTo 1L

            // 지정한 경로의 대상 JSON이 후보 값을 포함하는지 검증한다.
            if (testDB in TestDB.ALL_MYSQL_LIKE) {
                // Path 를 이용하여 특정 필드를 비교할 수 있습니다.
                val userIsInAlphaTeam2 = JsonBTable.jsonBColumn.contains("\"Alpha\"", path = ".user.team")
                val alphaTeamUsers = tester.selectAll().where { userIsInAlphaTeam2 }
                alphaTeamUsers.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * JSONB 컬럼에 대한 검색 조건 테스트
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `json exists`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonBTable(testDB) { tester, user1, data1 ->
            val maximumLogins = 1000
            val teamA = "A"

            /**
             * ```sql
             * -- Postgres 예:
             * INSERT INTO j_b_table (j_b_column)
             * VALUES ({"user":{"name":"Admin","team":"A"},"logins":1000,"active":true,"team":null})
             * ```
             */
            val newId = tester.insertAndGetId {
                it[JsonBTable.jsonBColumn] = data1.copy(user = data1.user.copy(team = teamA), logins = maximumLogins)
            }

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT COUNT(*) FROM j_b_table WHERE JSONB_PATH_EXISTS(j_b_table.j_b_column, '$')
             * ```
             */
            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            // 경로 인자를 전달하지 않아 루트 경로 `$`의 데이터 존재 여부를 검증한다.
            val hasAnyData = JsonBTable.jsonBColumn.exists(optional = optional)
            tester.selectAll().where { hasAnyData }.count() shouldBeEqualTo 2L

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT COUNT(*)
             *   FROM j_b_table
             *  WHERE JSONB_PATH_EXISTS(j_b_table.j_b_column, '$.fakeKey')
             * ```
             */
            val hasFakeKey = JsonBTable.jsonBColumn.exists(".fakeKey", optional = optional)
            tester.selectAll().where { hasFakeKey }.count() shouldBeEqualTo 0L

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT COUNT(*)
             *   FROM j_b_table
             *  WHERE JSONB_PATH_EXISTS(j_b_table.j_b_column, '$.logins')
             * ```
             */
            val hasLogins = JsonBTable.jsonBColumn.exists(".logins", optional = optional)
            tester.selectAll().where { hasLogins }.count() shouldBeEqualTo 2L

            // 필터 조건과 선택 인자를 사용해 경로의 데이터 존재 여부를 검증한다.
            val testDialect = currentDialectTest
            if (testDialect is PostgreSQLDialect) {
                /**
                 * ```sql
                 * -- Postgres 예:
                 * SELECT j_b_table.id
                 *   FROM j_b_table
                 *  WHERE JSONB_PATH_EXISTS(j_b_table.j_b_column, '$.logins ? (@ == 1000)')
                 * ```
                 */
                val filterPath = ".logins ? (@ == $maximumLogins)"
                val hasMaxLogins: Exists = JsonBTable.jsonBColumn.exists(filterPath)
                val usersWithMaxLogin: Query = tester.select(tester.id).where { hasMaxLogins }
                usersWithMaxLogin.single()[tester.id] shouldBeEqualTo newId

                /**
                 * ```sql
                 * -- Postgres 예:
                 * SELECT j_b_table.id
                 *   FROM j_b_table
                 *  WHERE JSONB_PATH_EXISTS(j_b_table.j_b_column, '$.user.team ? (@ == $team)', '{"team":"A"}')
                 * ```
                 */
                val jsonPath = ".user.team ? (@ == \$team)"
                val optionalArg = """{"team":"$teamA"}"""
                val isOnTeamA: Exists = JsonBTable.jsonBColumn.exists(jsonPath, optional = optionalArg)
                val usersOnTeamA: Query = tester.select(tester.id).where { isOnTeamA }
                usersOnTeamA.single()[tester.id] shouldBeEqualTo newId
            }
        }
    }

    /**
     * JSONB 컬럼에 Array 데이터를 저장하고 검색하는 테스트
     *
     * ```sql
     * -- Postgres 예:
     * SELECT j_b_arrays.id, j_b_arrays."groups", j_b_arrays.numbers
     *   FROM j_b_arrays
     *  WHERE JSONB_EXTRACT_PATH_TEXT(j_b_arrays."groups", 'users', '0', 'team') = 'Team A';
     *
     * SELECT JSONB_EXTRACT_PATH_TEXT(j_b_arrays.numbers, '0') FROM j_b_arrays;
     * ```
     *
     * ```sql
     * -- Postgres 예:
     * SELECT j_b_arrays.id, j_b_arrays.`groups`, j_b_arrays.numbers
     *   FROM j_b_arrays
     *  WHERE JSON_UNQUOTE(JSON_EXTRACT(j_b_arrays.`groups`, "$.users[0].team")) = 'Team A';
     *
     * SELECT JSON_UNQUOTE(JSON_EXTRACT(j_b_arrays.numbers, "$[0]"))
     *   FROM j_b_arrays;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb extract with arrays`(testDB: TestDB) {
        withJsonBArrays(testDB) { tester, singleId, tripleId ->
            val path1 = when (currentDialectTest) {
                is PostgreSQLDialect -> arrayOf("users", "0", "team")
                else -> arrayOf(".users[0].team")
            }

            val firstIsOnTeamA = JsonBArrayTable.groups.extract<String>(*path1) eq "Team A"
            tester.selectAll()
                .where { firstIsOnTeamA }
                .single()[tester.id] shouldBeEqualTo singleId

            // 오래된 MySQL 및 MariaDB 버전은 JSON 배열에서 추출한 비스칼라 값 형식이 필요하다.
            val toScala = testDB != TestDB.MYSQL_V5
            val path2 = if (currentDialectTest is PostgreSQLDialect) "0" else "[0]"
            val firstNumber = JsonBArrayTable.numbers.extract<Int>(path2, toScalar = toScala)
            tester.select(firstNumber).map { it[firstNumber] } shouldBeEqualTo listOf(100, 3)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb contains with array`(testDB: TestDB) {
        withJsonBArrays(testDB) { tester, _, tripleId ->
            /**
             * ```sql
             * -- Postgres
             * SELECT j_b_arrays.id, j_b_arrays."groups", j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE j_b_arrays.numbers @> '[3, 5]';
             *
             * -- MySQL V8
             * SELECT j_b_arrays.id, j_b_arrays.`groups`, j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE JSON_CONTAINS(j_b_arrays.numbers, '[3, 5]');
             * ```
             */
            val hasSmallNumbers = JsonBArrayTable.numbers.contains("[3, 5]")
            tester.selectAll()
                .where { hasSmallNumbers }
                .single()[tester.id] shouldBeEqualTo tripleId

            /**
             * ```sql
             * -- MySQL V8
             * SELECT j_b_arrays.id, j_b_arrays.`groups`, j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE JSON_CONTAINS(j_b_arrays.`groups`, '"B"', '$.users[0].name');
             * ```
             */
            if (testDB in TestDB.ALL_MYSQL_LIKE) {
                val hasSmallNumbers2 = JsonBArrayTable.groups.contains("\"B\"", path = ".users[0].name")
                tester.selectAll()
                    .where { hasSmallNumbers2 }
                    .single()[tester.id] shouldBeEqualTo tripleId
            }
        }
    }

    /**
     * JSONB 컬럼에 대한 검색 조건 테스트
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb exists with array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withJsonBArrays(testDB) { tester, singleId, tripleId ->
            val optional = if (testDB in TestDB.ALL_MYSQL_LIKE) "one" else null

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT j_b_arrays.id, j_b_arrays."groups", j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE JSONB_PATH_EXISTS(j_b_arrays."groups", '$.users[1]');
             *
             * -- MySQL V8 예:
             * SELECT j_b_arrays.id, j_b_arrays.`groups`, j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE JSON_CONTAINS_PATH(j_b_arrays.`groups`, 'one', '$.users[1]');
             * ```
             */
            val hasMultipleUsers = JsonBArrayTable.groups.exists(".users[1]", optional = optional)
            tester.selectAll().where { hasMultipleUsers }.single()[tester.id] shouldBeEqualTo tripleId

            /**
             * ```sql
             * -- Postgres 예:
             * SELECT j_b_arrays.id, j_b_arrays."groups", j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE JSONB_PATH_EXISTS(j_b_arrays.numbers, '$[2]');
             *
             * -- MySQL V8 예:
             * SELECT j_b_arrays.id, j_b_arrays.`groups`, j_b_arrays.numbers
             *   FROM j_b_arrays
             *  WHERE JSON_CONTAINS_PATH(j_b_arrays.numbers, 'one', '$[2]');
             * ```
             */
            val hasAtLeast3Numbers = JsonBArrayTable.numbers.exists("[2]", optional = optional)
            tester.selectAll().where { hasAtLeast3Numbers }.single()[tester.id] shouldBeEqualTo tripleId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb with defaults`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }
        // MariaDB: JSONB(JSON) 컬럼 default 메타데이터가 declared schema와 round-trip되지 않아 migration diff 발생 → 스킵
        Assumptions.assumeFalse { testDB == TestDB.MARIADB }
        /**
         * JSON 컬럼 기본값을 검증한다.
         * ```sql
         * -- Postgres 예:
         * CREATE TABLE IF NOT EXISTS default_tester (
         *      user_1 JSONB DEFAULT '{"name":"UNKNOWN","team":"UNASSIGNED"}'::jsonb NOT NULL,
         *      user_2 JSONB NOT NULL
         * )
         * ```
         */
        val defaultUser = User("UNKNOWN", "UNASSIGNED")
        val defaultTester = object: Table("default_tester") {
            val user1 = jsonb<User>("user_1", Json.Default).default(defaultUser)
            val user2 = jsonb<User>("user_2", Json.Default).clientDefault { defaultUser }
        }

        withDb(testDB) {
            if (testDB in binaryJsonNotSupportedDB) {
                expectException<UnsupportedByDialectException> {
                    SchemaUtils.create(defaultTester)
                }
            } else {
                SchemaUtils.create(defaultTester)
                defaultTester.exists().shouldBeTrue()

                // 반환된 메타데이터의 기본값과 선언한 기본값이 일치하는지 검증한다.
                // [deprecated] SchemaUtils.statementsRequiredToActualizeScheme → MigrationUtils.statementsRequiredForDatabaseMigration 로 교체
                // val alters = SchemaUtils.statementsRequiredToActualizeScheme(defaultTester)
                val alters = MigrationUtils.statementsRequiredForDatabaseMigration(defaultTester)
                alters.shouldBeEmpty()

                /**
                 * Client default 값이므로, Insert 시에는 값이 제공된다.
                 * ```sql
                 * -- Postgres 예:
                 * INSERT INTO default_tester (user_2)
                 * VALUES ({"name":"UNKNOWN","team":"UNASSIGNED"})
                 */
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
     * [StdOutSqlLogger] 를 사용하여 JSONB 컬럼에 대한 로깅 테스트
     */
    @OptIn(ExperimentalSerializationApi::class)
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `logger with jsonb collections`(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS iterables_tester (
         *      user_list JSONB NOT NULL,
         *      int_list JSONB NOT NULL,
         *      user_array JSONB NOT NULL,
         *      int_array JSONB NOT NULL
         * )
         * ```
         */
        val iterables = object: Table("iterables_tester") {
            val userList = jsonb<List<User>>("user_list", Json.Default, ListSerializer(User.serializer()))
            val intList = jsonb<List<Int>>("int_list", Json.Default)
            val userArray = jsonb<Array<User>>("user_array", Json.Default, ArraySerializer(User.serializer()))
            val intArray = jsonb<IntArray>("int_array", Json.Default)
        }

        withTables(testDB, iterables) {
            // Iterable 값을 삽입할 때 ClassCastException이 발생하지 않는지 검증하려고 logger를 남겨 둔다.
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
     * JSONB 컬럼에 대한 NULL 값 테스트
     *
     * ```sql
     * -- Postgres 예:
     * CREATE TABLE IF NOT EXISTS nullable_tester (
     *      id SERIAL PRIMARY KEY,
     *      "user" JSONB NULL
     * );
     *
     * INSERT INTO nullable_tester ("user") VALUES (NULL);
     * INSERT INTO nullable_tester ("user") VALUES ({"name":"A","team":"Team A"});
     *
     * SELECT nullable_tester."user" FROM nullable_tester WHERE nullable_tester.id = 1;
     * SELECT nullable_tester."user" FROM nullable_tester WHERE nullable_tester.id = 2;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb with nullable column`(testDB: TestDB) {
        val tester = object: IntIdTable("nullable_tester") {
            val user = jsonb<User>("user", Json.Default).nullable()
        }

        withTables(testDB, tester) {
            val nullId = tester.insertAndGetId {
                it[user] = null
            }
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

    /**
     * JSONB 컬럼에 대한 UPSERT 테스트
     *
     * ```sql
     * -- Postgres 예:
     * INSERT INTO j_b_table (id, j_b_column)
     * VALUES (2, {"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":false,"team":"A"})
     * ON CONFLICT (id) DO UPDATE SET j_b_column=EXCLUDED.j_b_column;
     * ```
     *
     * ```sql
     * -- MySQL V8 예:
     * INSERT INTO j_b_table (id, j_b_column)
     * VALUES (2, {"user":{"name":"Pro","team":"Alpha"},"logins":999,"active":false,"team":"A"})
     * AS NEW ON DUPLICATE KEY UPDATE id=NEW.id, j_b_column=NEW.j_b_column;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb with upsert`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withJsonBTable(testDB) { tester, _, _ ->
            val newData = DataHolder(User("Pro", "Alpha"), 999, true, "A")
            val newId = tester.insertAndGetId {
                it[JsonBTable.jsonBColumn] = newData
            }

            val newData2 = newData.copy(active = false)
            tester.upsert {
                it[tester.id] = newId
                it[JsonBTable.jsonBColumn] = newData2
            }

            val newResult = tester.selectAll().where { tester.id eq newId }.singleOrNull()
            newResult?.get(JsonBTable.jsonBColumn) shouldBeEqualTo newData2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `jsonb with transformer`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      numbers JSONB NOT NULL
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val numbers: Column<DoubleArray> = jsonb<IntArray>("numbers", Json.Default)
                .transform(
                    wrap = { DoubleArray(it.size) { i -> it[i].toDouble() } },
                    unwrap = { IntArray(it.size) { i -> it[i].toInt() } }
                )
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
    fun `jsonb as default`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        val defaultUser = User("name", "team")
        val tester = object: IntIdTable("testJsonAsDefault") {
            val value = jsonb<User>("value", Json.Default).default(defaultUser)
        }
        val testerDatabaseGenerated = object: IntIdTable("testJsonAsDefault") {
            val value = jsonb<User>("value", Json.Default).databaseGenerated()
        }

        // MySQL 8.0.13 이전 버전은 JSON 컬럼 기본값을 허용하지 않는다.
        withTables(testDB, tester) {
            testerDatabaseGenerated.insert { }

            val value = testerDatabaseGenerated.selectAll().single()[tester.value]
            value shouldBeEqualTo defaultUser
        }
    }

    private class KeyExistsOp(left: Expression<*>, right: Expression<*>): ComparisonOp(left, right, "??")

    private infix fun ExpressionWithColumnType<*>.keyExists(other: String) =
        KeyExistsOp(this, stringLiteral(other))

    /**
     * 사용자 정의 연산자를 사용하여 JSONB 컬럼의 특정 키가 존재하는지 확인합니다.
     *
     * **단, Postgres 에서만 사용 가능합니다.**
     *
     * ```sql
     * -- Postgres 예:
     *
     * SELECT j_b_table.id, j_b_table.j_b_column
     *   FROM j_b_table
     *  WHERE j_b_table.j_b_column ?? 'logins';
     *
     * SELECT j_b_table.id, j_b_table.j_b_column
     *   FROM j_b_table
     *  WHERE j_b_table.j_b_column ?? 'name';
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `escaped placeholder in custom operator`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }

        withJsonBTable(testDB) { tester, _, data1 ->

            // `.logins`, `.active`, `.team` 이 top level 에 있다.
            val topLevelKeyResult = tester
                .selectAll()
                .where { JsonBTable.jsonBColumn keyExists "logins" }
                .single()
            topLevelKeyResult[JsonBTable.jsonBColumn] shouldBeEqualTo data1

            // `.user.name` 이 있지만, `.name` 은 없다.
            val nestedKeyResult = tester
                .selectAll()
                .where { JsonBTable.jsonBColumn keyExists "name" }
                .toList()
            nestedKeyResult.shouldBeEmpty()

        }
    }
}
