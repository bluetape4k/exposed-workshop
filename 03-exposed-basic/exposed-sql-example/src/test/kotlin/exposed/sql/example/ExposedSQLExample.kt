package exposed.sql.example

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.sql.example.Schema.CityTable
import exposed.sql.example.Schema.UserTable
import exposed.sql.example.Schema.withCityUsers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.orWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ExposedSQLExample: AbstractExposedTest() {

    companion object: KLoggingChannel()

    /**
     * 특정 조건에 맞는 행을 UPDATE 합니다.
     *
     * ```sql
     * -- Postgres
     * UPDATE "users"
     *    SET "name"='Alexey'
     *  WHERE "users".ID = 'alex'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Raw SQL을 이용하여 Update 수행합니다`(testDB: TestDB) {
        withCityUsers(testDB) {
            UserTable.update({ UserTable.id eq "alex" }) {
                it[name] = "Alexey"   // Alex -> Alexey
            }

            // 갱신된 정보를 조회합니다.
            UserTable
                .selectAll()
                .where { UserTable.id eq "alex" }
                .single()[UserTable.name] shouldBeEqualTo "Alexey"

            // 이렇게 수행해도 된다.
            UserTable
                .selectAll()
                .where { UserTable.name eq "Alexey" }
                .count() shouldBeEqualTo 1L
        }
    }

    /**
     * 조건절에 따라 행을 삭제합니다.
     *
     * ```sql
     * -- Postgres
     * DELETE
     *   FROM "users"
     *  WHERE "users"."name" LIKE '%thing'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Raw SQL을 이용하여 DELETE를 수행합니다`(testDB: TestDB) {
        withCityUsers(testDB) {
            val affectedCount = UserTable.deleteWhere { UserTable.name like "%thing" }
            affectedCount shouldBeEqualTo 1

            // 삭제된 데이터는 조회되지 않아야 한다
            UserTable
                .selectAll()
                .where { UserTable.name like "%thing" }
                .count() shouldBeEqualTo 0L
        }
    }

    /**
     * Manual Join 조건을 추가로 지정합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT users."name",
     *        cities."name"
     *   FROM users INNER JOIN cities ON cities.id = users.city_id
     *  WHERE ((users.id = 'debop') OR (users."name" = 'Jane.Doe'))
     *    AND (users.id = 'jane')
     *    AND (users.city_id = cities.id)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `manual inner join`(testDB: TestDB) {
        withCityUsers(testDB) {
            val results = UserTable
                .innerJoin(CityTable)
                .select(UserTable.name, CityTable.name)
                .where { (UserTable.id eq "debop") or (UserTable.name eq "Jane.Doe") }
                .andWhere { UserTable.id eq "jane" }
                .andWhere { UserTable.cityId eq CityTable.id }  // manual join (굳이 할 필요 없음)
                .map { it[UserTable.name] to it[CityTable.name] }

            results shouldHaveSize 1
            results.single() shouldBeEqualTo ("Jane.Doe" to "Busan")
        }
    }

    /**
     * Foreign Key를 이용하여 Join을 수행합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT users."name",
     *        users.city_id,
     *        cities."name"
     *   FROM users INNER JOIN cities ON cities.id = users.city_id
     *  WHERE (cities."name" = 'Busan')
     *     OR (users.city_id IS NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with foreign key`(testDB: TestDB) {
        withCityUsers(testDB) {
            val results = UserTable
                .leftJoin(CityTable)
                .select(UserTable.name, UserTable.cityId, CityTable.name)
                .where { CityTable.name eq "Busan" }
                .orWhere { UserTable.cityId.isNull() }
                .map { it[UserTable.name] }

            results.toSet() shouldBeEqualTo setOf("Jane.Doe", "John.Doe", "Alex", "Something")
        }
    }

    /**
     * `GROUP BY` 와 집계 함수를 사용하는 예제
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name",
     *        COUNT(users.id)
     *   FROM cities INNER JOIN users ON cities.id = users.city_id
     *  GROUP BY cities."name"
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `use functions and group by`(testDB: TestDB) {
        withCityUsers(testDB) {
            val query = CityTable.innerJoin(UserTable)
                .select(CityTable.name, UserTable.id.count())
                .groupBy(CityTable.name)

            val userCountsByCity = query.associate {
                it[CityTable.name] to it[UserTable.id.count()]
            }

            userCountsByCity.keys shouldBeEqualTo setOf("Seoul", "Busan")
            userCountsByCity["Seoul"] shouldBeEqualTo 1L
            userCountsByCity["Busan"] shouldBeEqualTo 2L
        }
    }
}
