package exposed.dao.example

import exposed.dao.example.Schema.City
import exposed.dao.example.Schema.CityTable
import exposed.dao.example.Schema.User
import exposed.dao.example.Schema.UserTable
import exposed.dao.example.Schema.withCityUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.intLiteral
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Exposed DAO 를 사용하는 예제입니다.
 */
class ExposedDaoExample: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * DAO Entity 를 사용하여 도시 정보를 조회합니다.
     *
     * ```sql
     * SELECT COUNT(*) FROM city;
     * SELECT city.id, city."name" FROM city;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Entity 정보 조회하기`(testDB: TestDB) {
        withCityUsers(testDB) {
            // 도시 정보를 조회합니다.
            City.all().count() shouldBeEqualTo 3L
            City.all().map { it.name }.toSet() shouldBeEqualTo setOf("Seoul", "Busan", "Daegu")
            City.find { CityTable.name eq "Seoul" }.single().name shouldBeEqualTo "Seoul"
        }
    }

    /**
     * 엔티티를 조건절로 검색합니다.
     * 이때, one-to-many 관계를 가지는 엔티티도 eager loading 합니다.
     *
     * ```sql
     * SELECT city.id, city."name"
     *   FROM city
     *  WHERE city."name" = 'Seoul';
     *
     * SELECT "User".id,
     *        "User"."name",
     *        "User".age,
     *        "User".city_id
     *   FROM "User"
     *  WHERE "User".city_id = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Entity를 조건절로 검색하기 01`(testDB: TestDB) {
        withCityUsers(testDB) {
            // 도시 정보와 함께 User 정보를 eager loading 합니다.
            val seoul = City.find { CityTable.name eq "Seoul" }.with(City::users).single()

            val usersInSeoul = seoul.users.toList()
            usersInSeoul shouldHaveSize 2
            usersInSeoul.map { it.city } shouldBeEqualTo listOf(seoul, seoul)

            usersInSeoul.forEach { user ->
                log.info { "${user.name} lives in ${user.city?.name}" }
            }
        }
    }

    /**
     * 엔티티를 조건절로 검색합니다.
     * 이때, many-to-one 관계를 가지는 엔티티도 eager loading 합니다.
     *
     * ```sql
     * SELECT "User".id, "User"."name", "User".age, "User".city_id
     *   FROM "User"
     *  WHERE "User".age >= 18;
     *
     * SELECT city.id, city."name"
     *   FROM city
     *  WHERE city.id IN (1, 2);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Entity를 조건절로 검색하기 02`(testDB: TestDB) {
        withCityUsers(testDB) {
            val users = User.find { UserTable.age greaterEq intLiteral(18) }.with(User::city).toList()
            
            users shouldHaveSize 4
            users.forEach { user ->
                log.info { "${user.name} lives in ${user.city?.name ?: "unknown"}" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Entity Update 하기`(testDB: TestDB) {
        withCityUsers(testDB) {
            val temp = User.new {
                name = "temp"
                age = 14
            }
            flushCache()

            val loaded = User.findById(temp.id)!!
            loaded.age = 42
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO Entity 삭제하기`(testDB: TestDB) {
        withCityUsers(testDB) {
            val temp = User.new {
                name = "temp"
                age = 14
            }
            flushCache()
            temp.delete()
        }
    }
}
