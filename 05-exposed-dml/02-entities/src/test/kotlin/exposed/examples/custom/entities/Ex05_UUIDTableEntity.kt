package exposed.examples.custom.entities

import exposed.examples.entities.UUIDTables.Address
import exposed.examples.entities.UUIDTables.Addresses
import exposed.examples.entities.UUIDTables.Cities
import exposed.examples.entities.UUIDTables.City
import exposed.examples.entities.UUIDTables.People
import exposed.examples.entities.UUIDTables.Person
import exposed.examples.entities.UUIDTables.Town
import exposed.examples.entities.UUIDTables.Towns
import exposed.shared.mapping.PersonSchema.AddressTable.city
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*


object UUIDTables {
    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS cities (
     *      d uuid PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object Cities: UUIDTable() {
        val name = varchar("name", 50)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS people (
     *      id uuid PRIMARY KEY,
     *      "name" VARCHAR(80) NOT NULL,
     *      city_id uuid NOT NULL,
     *
     *      CONSTRAINT fk_people_city_id__id FOREIGN KEY (city_id)
     *      REFERENCES cities(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object People: UUIDTable() {
        val name = varchar("name", 80)
        val cityId = reference("city_id", Cities)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS addresses (
     *      id uuid PRIMARY KEY,
     *      person_id uuid NOT NULL,
     *      city_id uuid NOT NULL,
     *      address VARCHAR(255) NOT NULL,
     *
     *      CONSTRAINT fk_addresses_person_id__id FOREIGN KEY (person_id)
     *      REFERENCES people(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_addresses_city_id__id FOREIGN KEY (city_id)
     *      REFERENCES cities(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE addresses ADD CONSTRAINT addresses_person_id_city_id_unique UNIQUE (person_id, city_id);
     * ```
     */
    object Addresses: UUIDTable() {
        val personId = reference("person_id", People)
        val cityId = reference("city_id", Cities)

        val address = varchar("address", 255)

        init {
            uniqueIndex(personId, cityId)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS towns (
     *      id uuid PRIMARY KEY,
     *      city_id uuid NOT NULL,
     *
     *      CONSTRAINT fk_towns_city_id__id FOREIGN KEY (city_id)
     *      REFERENCES cities(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object Towns: UUIDTable("towns") {
        val cityId = uuid("city_id").references(Cities.id)
    }

    class City(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<City>(Cities)

        var name: String by Cities.name
        val towns: SizedIterable<Town> by Town referrersOn Towns.cityId  // one-to-many

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Person(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<Person>(People)

        var name: String by People.name
        var city: City by City referencedOn People.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("city id", city.idValue)
            .toString()
    }

    class Address(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<Address>(Addresses)

        var person: Person by Person referencedOn Addresses.personId  // many-to-one
        var city: City by City referencedOn Addresses.cityId          // many-to-one
        var address: String by Addresses.address

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("address", address)
            .add("person id", person.idValue)
            .add("city id", city.idValue)
            .toString()
    }

    class Town(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<Town>(Towns)

        var city: City by City referencedOn Towns.cityId     // many-to-one

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("city id", city.idValue)
            .toString()
    }
}

/**
 * Primary Key 가 UUID 인 테이블을 사용하는 예
 */
class Ex05_UUIDTableEntity: AbstractExposedTest() {


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create tables`(testDB: TestDB) {
        withTables(testDB, Cities, People, Addresses, Towns) {
            Cities.exists().shouldBeTrue()
            People.exists().shouldBeTrue()
            Addresses.exists().shouldBeTrue()
            Towns.exists().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val seoul = City.new { name = "Seoul" }
            val busan = City.new { name = "Busan" }

            Person.new(UUID.randomUUID()) {
                name = "Debop"
                city = seoul
            }
            Person.new(UUID.randomUUID()) {
                name = "BTS"
                city = seoul
            }
            Person.new {
                name = "Sam"
                city = busan
            }

            flushCache()

            /**
             * ```sql
             * SELECT cities.id, cities."name"
             *   FROM cities
             *  ORDER BY cities.id ASC
             * ```
             */
            val allCities = City.all()
                .orderBy(Cities.id to ASC)
                .map { it.name }
            allCities shouldContainSame listOf("Seoul", "Busan")

            /**
             * eager loading of many-to-one
             *
             * ```sql
             * SELECT people.id, people."name", people.city_id
             *   FROM people;
             *
             * SELECT cities.id, cities."name"
             *   FROM cities
             *  WHERE cities.id IN ('15fd22e5-7480-47f5-b054-5d60b5c1b456', '45fba8f9-502f-4469-8a21-3ab119c08ee0');
             * ```
             */
            val allPeople = Person.all().with(Person::city).map { it.name to it.city.name }
            allPeople shouldContainSame listOf(
                "Debop" to "Seoul",
                "BTS" to "Seoul",
                "Sam" to "Busan"
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update and delete records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val seoul = City.new { name = "Seoul" }
            val busan = City.new { name = "Busan" }

            Person.new(UUID.randomUUID()) {
                name = "Debop"
                city = seoul
            }
            Person.new(UUID.randomUUID()) {
                name = "BTS"
                city = seoul
            }
            val sam = Person.new {
                name = "Sam"
                city = busan
            }

            // DELETE FROM people WHERE people.id = 'a7b353ef-2fcb-4daf-9d41-ed6516d18bda'
            sam.delete()
            // DELETE FROM cities WHERE cities.id = '038bcadb-a8a5-46b4-9e98-2385b70669e4'
            busan.delete()

            flushCache()

            val allCities = City.all().map { it.name }
            allCities shouldBeEqualTo listOf("Seoul")

            /**
             * Eager loading of many-to-one
             *
             * ```sql
             * SELECT people.id, people."name", people.city_id FROM people;
             *
             * SELECT cities.id, cities."name"
             *   FROM cities
             *  WHERE cities.id = '54ae8589-f7b9-4fd4-a0ef-fc74a598c99e'
             * ```
             */
            val allPeople = Person.all().with(Person::city).map { it.name to it.city.name }
            allPeople shouldContainSame listOf(
                "Debop" to "Seoul",
                "BTS" to "Seoul",
            )
        }
    }

    /**
     * Entity 속성에 바로 entity 생성을 수행할 수 있다.
     *
     * ```sql
     * -- create city
     * INSERT INTO cities (id, "name") VALUES ('ff62fa07-59cc-44c9-b407-df2f49151e11', 'City1');
     *
     * -- create person1, person2
     * INSERT INTO people (id, "name", city_id)
     * VALUES ('d8974bd1-3585-416c-a536-59db33e79f74', 'Person1', 'ff62fa07-59cc-44c9-b407-df2f49151e11');
     *
     * INSERT INTO people (id, "name", city_id)
     * VALUES ('c23924b3-3215-4ad5-911f-10aec993ae02', 'Person2', 'ff62fa07-59cc-44c9-b407-df2f49151e11');
     *
     * -- create address1, address2
     * INSERT INTO addresses (id, person_id, city_id, address)
     * VALUES ('a2f74e17-47bd-4245-b0b7-99c7d0c8b480', 'd8974bd1-3585-416c-a536-59db33e79f74', 'ff62fa07-59cc-44c9-b407-df2f49151e11', 'Address1');
     *
     * INSERT INTO addresses (id, person_id, city_id, address)
     * VALUES ('3bf486ba-728d-4f26-892b-38ce68359639', 'c23924b3-3215-4ad5-911f-10aec993ae02', 'ff62fa07-59cc-44c9-b407-df2f49151e11', 'Address2');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with inner table`(testDB: TestDB) {
        withTables(testDB, Addresses, Cities, People) {
            val city1 = City.new { name = "City1" }
            val address1 = Address.new {
                person = Person.new {
                    name = "Person1"
                    city = city1
                }
                city = city1
                address = "Address1"
            }
            val address2 = Address.new {
                person = Person.new {
                    name = "Person2"
                    city = city1
                }
                city = city1
                address = "Address2"
            }

            address1.refresh(flush = true)
            address1.address shouldBeEqualTo "Address1"

            address2.refresh(flush = true)
            address2.address shouldBeEqualTo "Address2"
        }
    }

    /**
     * Lazy loading referencedOn
     * ```sql
     * SELECT towns.id, towns.city_id FROM towns
     *
     * SELECT cities.id, cities."name"
     *   FROM cities WHERE cities.id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619'
     * ```
     *
     * Eager loading referencedOn
     * ```sql
     * SELECT towns.id, towns.city_id FROM towns
     *
     * SELECT cities.id, cities."name"
     *   FROM cities
     *  WHERE cities.id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619'
     *
     * SELECT cities.id, cities."name"
     *   FROM cities
     *  WHERE cities.id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619'
     * ```
     *
     * Lazy loading referrersOn
     * ```sql
     * SELECT cities.id, cities."name" FROM cities
     *
     * SELECT towns.id, towns.city_id
     *   FROM towns
     *  WHERE towns.city_id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619'
     *
     * SELECT cities.id, cities."name"
     *   FROM cities
     *  WHERE cities.id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619'
     * ```
     *
     * Eager loading referrersOn
     * ```sql
     * SELECT cities.id, cities."name" FROM cities;
     *
     * SELECT towns.id, towns.city_id, cities.id
     *   FROM towns INNER JOIN cities ON towns.city_id = cities.id
     *  WHERE towns.city_id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619';
     *
     * SELECT cities.id, cities."name"
     *   FROM cities
     *  WHERE cities.id = '7f14895f-9a26-4f5e-9f07-fb6b7265c619';
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `foreign key between uuid and entity id column`(testDB: TestDB) {
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

            entityCache.clear()

            // lazy loading referencedOn
            log.debug { "Lazy loading referencedOn" }
            val town1 = Town.all().first()
            town1.city.id shouldBeEqualTo cId

            // eager loading referencedOn
            log.debug { "Eager loading referencedOn" }
            val town1WithCity = Town.all().with(Town::city).first()
            town1WithCity.city.id shouldBeEqualTo cId

            // lazy loading referrersOn
            log.debug { "Lazy loading referrersOn" }
            val city1 = City.all().single()
            city1.towns.first().city.id shouldBeEqualTo cId

            // eager loading referrersOn
            log.debug { "Eager loading referrersOn" }
            val city1WithTowns = City.all().with(City::towns).single()
            listOf(tId, tId2) shouldContain city1WithTowns.towns.first().id
        }
    }
}
