package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import nl.altindag.log.LogCaptor
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.exposedLogger
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.orWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Join Query 예제 모음
 *
 * 참조: [5 Infographics to Understand SQL Joins visually](https://datalemur.com/blog/sql-joins-infographics)
 */
class Ex11_Join: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * `innerJoin` 의 조건이 자동으로 지정되지만, 사용자가 where 절에 추가할 수도 있다.
     *
     * ```sql
     * -- Postgres
     * SELECT users."name",
     *        cities."name"
     *   FROM users INNER JOIN cities ON cities.city_id = users.city_id
     *  WHERE ((users.id = 'andrey') OR (users."name" = 'Sergey'))
     *    AND (users.city_id = cities.city_id)  -- 추가 조건
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `manual join`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            users.innerJoin(cities)
                .select(users.name, cities.name)
                .where { (users.id.eq("andrey") or users.name.eq("Sergey")) }
                .andWhere { users.cityId eq cities.id }   // 없어도 같은 결과이다.
                .forEach {
                    val userName = it[users.name]
                    val cityName = it[cities.name]
                    when (userName) {
                        "Andrey" -> cityName shouldBeEqualTo "St. Petersburg"
                        "Sergey" -> cityName shouldBeEqualTo "Munich"
                        else -> error("Unexpected user $userName")
                    }
                }
        }
    }

    /**
     * `innerJoin` 시 Foreign Key 가 정의되어 있으면 그 조건으로 자동으로 join 한다.
     *
     * ```sql
     * -- Postgres
     * SELECT users."name",
     *        users.city_id,
     *        cities."name"
     *   FROM users INNER JOIN cities ON cities.city_id = users.city_id
     *  WHERE (cities."name" = 'St. Petersburg')
     *     OR (users.city_id IS NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with foreign key`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val stPetersburgUser = users.innerJoin(cities)
                .select(users.name, users.cityId, cities.name)
                .where { cities.name eq "St. Petersburg" }
                .orWhere { users.cityId.isNull() }
                .single()

            stPetersburgUser[users.name] shouldBeEqualTo "Andrey"
            stPetersburgUser[cities.name] shouldBeEqualTo "St. Petersburg"
        }
    }


    /**
     * 3개의 테이블을 `innerJoin` 하는 예제
     *
     * ```sql
     * -- Postgres
     * SELECT cities.city_id,
     *        cities."name",
     *        users.id,
     *        users."name",
     *        users.city_id,
     *        users.flags,
     *        userdata.user_id,
     *        userdata.comment,
     *        userdata."value"
     * FROM cities
     *      INNER JOIN users ON cities.city_id = users.city_id
     *      INNER JOIN userdata ON users.id = userdata.user_id
     * ORDER BY users.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `triple join`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, userData ->
            val rows = cities.innerJoin(users).innerJoin(userData)
                .selectAll()
                .orderBy(users.id)
                .toList()

            rows shouldHaveSize 2

            rows[0][users.name] shouldBeEqualTo "Eugene"
            rows[0][userData.comment] shouldBeEqualTo "Comment for Eugene"
            rows[0][cities.name] shouldBeEqualTo "Munich"

            rows[1][users.name] shouldBeEqualTo "Sergey"
            rows[1][userData.comment] shouldBeEqualTo "Comment for Sergey"
            rows[1][cities.name] shouldBeEqualTo "Munich"
        }
    }

    /**
     * `many-to-many` 관계의 테이블 들을 `innerJoin` 하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-many join`(testDB: TestDB) {

        val numbers = object: Table("numbers") {
            val id = integer("id")
            override val primaryKey = PrimaryKey(id)
        }
        val names = object: Table("names") {
            val name = varchar("name", 10)
            override val primaryKey = PrimaryKey(name)
        }

        /**
         * `numbers` 와 `names` 테이블의 관계를 `many-to-many` 로 매핑하는 테이블
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS "map" (
         *      id_ref INT NOT NULL,
         *      name_ref VARCHAR(10) NOT NULL,
         *
         *      CONSTRAINT fk_map_id_ref__id FOREIGN KEY (id_ref)
         *          REFERENCES numbers(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
         *      CONSTRAINT fk_map_name_ref__name FOREIGN KEY (name_ref)
         *          REFERENCES "names"("name") ON DELETE RESTRICT ON UPDATE RESTRICT
         * )
         * ```
         */
        val map = object: Table("map") {
            val idRef = reference("id_ref", numbers.id)
            val nameRef = reference("name_ref", names.name)
        }

        withTables(testDB, numbers, names, map) {
            numbers.insert { it[id] = 1 }
            numbers.insert { it[id] = 2 }
            names.insert { it[name] = "Foo" }
            names.insert { it[name] = "Bar" }
            map.insert {
                it[idRef] = 2
                it[nameRef] = "Foo"
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT numbers.id,
             *        "map".id_ref,
             *        "map".name_ref,
             *        "names"."name"
             *   FROM numbers
             *      INNER JOIN "map" ON numbers.id = "map".id_ref
             *      INNER JOIN "names" ON "names"."name" = "map".name_ref
             * ```
             */
            val rows = numbers.innerJoin(map).innerJoin(names)
                .selectAll()
                .toList()

            rows shouldHaveSize 1
            rows[0][numbers.id] shouldBeEqualTo 2
            rows[0][names.name] shouldBeEqualTo "Foo"
        }
    }

    /**
     * `crossJoin` 을 사용한 예제
     *
     * ```sql
     * -- Postgres
     * SELECT
     *      users."name",
     *      users.city_id,
     *      cities."name"
     * FROM cities CROSS JOIN users
     * WHERE cities."name" = 'St. Petersburg'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `cross join`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val allUsersToStPetersburg = cities.crossJoin(users)
                .select(users.name, users.cityId, cities.name)
                .where { cities.name.eq("St. Petersburg") }
                .map { it[users.name] to it[cities.name] }
                .onEach { (userName, cityName) ->
                    log.debug { "user=$userName, city=$cityName" }
                }

            val allUsers = setOf(
                "Andrey",
                "Sergey",
                "Eugene",
                "Alex",
                "Something"
            )

            allUsersToStPetersburg.map { it.second }.distinct() shouldBeEqualTo listOf("St. Petersburg")
            allUsersToStPetersburg.map { it.first }.toSet() shouldBeEqualTo allUsers
        }
    }

    /**
     * 한 테이블에 대한 FK 가 복수 개인 2개의 테이블의 JOIN 예
     *
     * JOIN 조건에 2개의 FK 가 모두 포함되어야 한다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS foo (
     *      id SERIAL PRIMARY KEY,
     *      baz INT NOT NULL
     * );
     *
     * ALTER TABLE foo ADD CONSTRAINT foo_baz_unique UNIQUE (baz);
     *
     * CREATE TABLE IF NOT EXISTS bar (
     *      id SERIAL PRIMARY KEY,
     *      foo INT NOT NULL,
     *      baz INT NOT NULL,
     *
     *      CONSTRAINT fk_bar_foo__id FOREIGN KEY (foo) REFERENCES foo(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_bar_baz__baz FOREIGN KEY (baz) REFERENCES foo(baz)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     *
     * ```sql
     * SELECT COUNT(*)
     *   FROM foo INNER JOIN bar ON foo.id = bar.foo AND foo.baz = bar.baz
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multiple reference join 01`(testDB: TestDB) {
        val foo = object: IntIdTable("foo") {
            val baz = integer("baz").uniqueIndex()
        }
        val bar = object: IntIdTable("bar") {
            val foo = reference("foo", foo)
            val baz = reference("baz", foo.baz) // integer("baz") references foo.baz
        }

        withTables(testDB, foo, bar) {
            val fooId: EntityID<Int> = foo.insertAndGetId {
                it[baz] = 5
            }

            bar.insert {
                it[this.foo] = fooId
                it[baz] = 5
            }

            val result = foo.innerJoin(bar).selectAll()
            result.count() shouldBeEqualTo 1L
        }
    }

    /**
     * Primary Key 를 참조하는 FK를 복수개를 가진다면, INNER JOIN 을 할 수 없다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS foo (
     *      id SERIAL PRIMARY KEY,
     *      baz INT NOT NULL
     * );
     *
     * ALTER TABLE foo ADD CONSTRAINT foo_baz_unique UNIQUE (baz);
     *
     * CREATE TABLE IF NOT EXISTS bar (
     *      id SERIAL PRIMARY KEY,
     *      foo INT NOT NULL,
     *      foo2 INT NOT NULL,
     *      baz INT NOT NULL,
     *
     *      CONSTRAINT fk_bar_foo__id FOREIGN KEY (foo) REFERENCES foo(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_bar_foo2__id FOREIGN KEY (foo2) REFERENCES foo(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_bar_baz__baz FOREIGN KEY (baz) REFERENCES foo(baz)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multiple reference join 02`(testDB: TestDB) {
        val foo = object: IntIdTable("foo") {
            val baz = integer("baz").uniqueIndex()
        }
        val bar = object: IntIdTable("bar") {
            val foo = reference("foo", foo)       // FK to foo's PK
            val foo2 = reference("foo2", foo)     // FK to foo's PK
            val baz = integer("baz") references foo.baz
        }

        withTables(testDB, foo, bar) {
            val fooId = foo.insertAndGetId {
                it[baz] = 5
            }
            bar.insert {
                it[this.foo] = fooId
                it[this.foo2] = fooId
                it[baz] = 5
            }

            // 복수의 FK 가 같은 컬럼을 참조할 때에는 INNER JOIN 이 불가능하다.
            expectException<IllegalStateException> {
                val result = foo.innerJoin(bar).selectAll()
                result.count()
            }
        }
    }

    /**
     * 동일 테이블을 Alias 를 이용해 Left Join 하기
     *
     * ```sql
     * -- Postgres
     * SELECT users.id,
     *        users."name",
     *        users.city_id,
     *        users.flags,
     *        u2.id,
     *        u2."name",
     *        u2.city_id,
     *        u2.flags
     *   FROM users LEFT JOIN users u2 ON u2.id = 'smth'
     *  WHERE users.id = 'alex'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with alias 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val usersAlias = users.alias("u2")

            val resultRow = users
                .join(usersAlias, JoinType.LEFT, usersAlias[users.id], stringLiteral("smth"))
                .selectAll()
                .where { users.id eq "alex" }
                .single()

            resultRow[users.name] shouldBeEqualTo "Alex"
            resultRow[usersAlias[users.name]] shouldBeEqualTo "Something"
        }
    }

    /**
     * Nested Join
     *
     * ```sql
     * SELECT COUNT(*)
     *   FROM cities
     *        INNER JOIN (users INNER JOIN userdata ON users.id = userdata.user_id)
     *              ON cities.city_id = users.city_id
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with join`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, userData ->
            val rows = cities.innerJoin(users innerJoin userData).selectAll()
            rows.count() shouldBeEqualTo 2L
        }
    }

    /**
     * 추가적인 조건절을 가진 Join
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM CITIES INNER JOIN USERS u2
     *          ON CITIES.CITY_ID = u2.CITY_ID
     *              AND ((CITIES.CITY_ID > 1) AND (CITIES."name" <> u2."name"))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with additional constraint`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val u2 = users.alias("u2")
            val join = cities
                .join(u2, JoinType.INNER, cities.id, u2[users.cityId]) {
                    cities.id greater 1 and (cities.name.neq(u2[users.name]))
                }

            val rows = join.selectAll()
            rows.count() shouldBeEqualTo 2L
        }
    }

    /**
     * Main table에만 데이터가 있고, Main table을 참조하는 Join table에 데이터가 없는 경우에는 null을 반환해야 한다.
     *
     * ```sql
     * -- Postgres
     * SELECT jointable."dataCol"
     *   FROM maintable
     *      LEFT JOIN jointable ON jointable."idCol" = maintable."idCol"
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `no warnings on left join regression`(testDB: TestDB) {
        val logCaptor = LogCaptor.forName(exposedLogger.name)

        val leftTable = object: Table("left_table") {
            val id = integer("idCol")
        }
        val rightTable = object: Table("right_table") {
            val id = integer("idCol")
            val data = integer("dataCol").default(42)
        }

        withTables(testDB, leftTable, rightTable) {
            // leftTable 에만 데이터를 넣는다
            leftTable.insert { it[id] = 2 }

            val data = leftTable.join(rightTable, JoinType.LEFT, rightTable.id, leftTable.id)
                .select(rightTable.data)
                .single()
                .getOrNull(rightTable.data)

            data.shouldBeNull()  // left join 이므로, leftTable 정보만 있고, rightTable 정보가 없으므로 null 이어야 한다.

            // Assert no logging took place
            logCaptor.warnLogs.shouldBeEmpty()
            logCaptor.errorLogs.shouldBeEmpty()
        }
    }
}
