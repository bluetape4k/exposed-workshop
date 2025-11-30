package exposed.examples.entities

import exposed.examples.entities.LongIdTables.Cities
import exposed.examples.entities.LongIdTables.City
import exposed.examples.entities.LongIdTables.People
import exposed.examples.entities.LongIdTables.Person
import exposed.examples.entities.LongIdTables.Town
import exposed.examples.entities.LongIdTables.Towns
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

object LongIdTables {

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS cities (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Cities: LongIdTable() {
        val name: Column<String> = varchar("name", 50)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS people (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(80) NOT NULL,
     *      city_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_people_city_id__id FOREIGN KEY (city_id) REFERENCES cities(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object People: LongIdTable() {
        val name: Column<String> = varchar("name", 80)
        val cityId: Column<EntityID<Long>> = reference("city_id", Cities)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS towns (
     *      id BIGSERIAL PRIMARY KEY,
     *      city_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_towns_city_id__id FOREIGN KEY (city_id) REFERENCES cities(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Towns: LongIdTable("towns") {
        val cityId: Column<Long> = long("city_id").references(Cities.id)
    }

    class City(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<City>(Cities)

        var name: String by Cities.name

        // one-to-many 관계
        val towns: SizedIterable<Town> by Town referrersOn Towns.cityId

        // one-to-many 관계
        // val people: SizedIterable<Person> by Person referrersOn People.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Person(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Person>(People)

        var name: String by People.name

        // many-to-one 관계
        var city: City by City referencedOn People.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("city", city.name)
            .toString()
    }


    class Town(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Town>(Towns)

        // many-to-one 관계
        var city: City by City referencedOn Towns.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("city", city.name)
            .toString()
    }
}

/**
 * 엔티티의 Id 수형이 Long 인 경우의 테스트
 */
class Ex04_LongIdTableEntity: JdbcExposedTestBase() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create tables`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            Cities.exists().shouldBeTrue()
            People.exists().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val seoul: City = City.new { name = "Seoul" }
            val busan: City = City.new { name = "Busan" }

            Person.new {
                name = "Sunghyouk Bae"
                city = seoul
            }
            Person.new {
                name = "Minseok Kim"
                city = seoul
            }
            Person.new {
                name = "Sunghoon Lee"
                city = busan
            }

            // all() 는 모든 테이블의 레코드를 가져온다.
            val allCities = City.all().map { it.name }
            allCities shouldContainSame listOf("Seoul", "Busan")

            val allPeople = Person.all().map { it.name to it.city.name }
            allPeople shouldContainSame listOf(
                "Sunghyouk Bae" to "Seoul",
                "Minseok Kim" to "Seoul",
                "Sunghoon Lee" to "Busan"
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update and delete records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val seoul: City = City.new { name = "Seoul" }
            val busan: City = City.new { name = "Busan" }

            Person.new {
                name = "Sunghyouk Bae"
                city = seoul
            }
            Person.new {
                name = "Minseok Kim"
                city = seoul
            }
            val sunghoon = Person.new {
                name = "Sunghoon Lee"
                city = busan
            }

            sunghoon.delete() // delete() 는 레코드를 삭제한다.
            busan.delete()    // delete() 는 레코드를 삭제한다.

            val allCities = City.all().map { it.name }
            allCities shouldContainSame listOf("Seoul")

            val allPeople = Person.all().map { it.name to it.city.name }
            allPeople shouldContainSame listOf(
                "Sunghyouk Bae" to "Seoul",
                "Minseok Kim" to "Seoul",
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `foreign key between long and EntityID columns`(testDB: TestDB) {
        withTables(testDB, Cities, Towns) {
            val cId = Cities.insertAndGetId {
                it[name] = "City A"
            }
            val tId = Towns.insertAndGetId {
                it[cityId] = cId.value
            }
            val tId2 = Towns.insertAndGetId {
                it[cityId] = cId.value
            }

            /**
             * lazy loaded referencedOn
             *
             * ```sql
             * SELECT towns.id, towns.city_id FROM towns LIMIT 1
             * ```
             */
            val town1 = Town.findById(tId)!!
            town1.shouldNotBeNull()

            /**
             * eager loaded referencedOn (`load(Town::city)`)
             *
             * ```sql
             * SELECT towns.id, towns.city_id FROM towns WHERE towns.id = 2
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * ```
             */
            val town1WithCity = Town.findById(tId2)!!.load(Town::city)
            town1WithCity.shouldNotBeNull()

            /**
             * lazy loaded referrersOn
             *
             * ```sql
             * SELECT cities.id, cities."name" FROM cities
             * ```
             */
            val city1 = City.all().single()
            city1.shouldNotBeNull()

            /**
             * eager loaded referrersOn (`with(City::towns)`)
             *
             * ```sql
             * SELECT cities.id, cities."name"
             *   FROM cities;
             *
             * SELECT towns.id, towns.city_id, cities.id
             *   FROM towns INNER JOIN cities ON towns.city_id = cities.id
             *  WHERE towns.city_id = 1;
             *
             * SELECT cities.id, cities."name" FROM cities WHERE cities.id = 1
             * ```
             */
            val city2 = City.all().with(City::towns).single()
            city2.shouldNotBeNull()
        }
    }
}
