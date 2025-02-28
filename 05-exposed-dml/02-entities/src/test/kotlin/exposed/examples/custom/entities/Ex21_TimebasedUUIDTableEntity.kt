package exposed.examples.custom.entities

import exposed.examples.entities.TimebasedUUIDTables.Address
import exposed.examples.entities.TimebasedUUIDTables.Addresses
import exposed.examples.entities.TimebasedUUIDTables.Cities
import exposed.examples.entities.TimebasedUUIDTables.City
import exposed.examples.entities.TimebasedUUIDTables.People
import exposed.examples.entities.TimebasedUUIDTables.Person
import exposed.examples.entities.TimebasedUUIDTables.Town
import exposed.examples.entities.TimebasedUUIDTables.Towns
import exposed.shared.mapping.PersonSchema.AddressTable.city
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityClass
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.idgenerators.uuid.TimebasedUuid.Epoch
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

/**
 * Timebased UUID 를 Identifier 로 사용하는 Entity 테스트
 */
object TimebasedUUIDTables {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS cities (
     *      id uuid PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * );
     * ```
     */
    object Cities: TimebasedUUIDTable() {
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
    object People: TimebasedUUIDTable() {
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
     * ALTER TABLE addresses
     *      ADD CONSTRAINT addresses_person_id_city_id_unique UNIQUE (person_id, city_id);
     * ```
     */
    object Addresses: TimebasedUUIDTable() {
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
     * )
     * ```
     */
    object Towns: TimebasedUUIDTable("towns") {
        val cityId = uuid("city_id").references(Cities.id)
    }

    class City(id: EntityID<UUID>): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<City>(Cities)

        var name by Cities.name
        val towns by Town referrersOn Towns.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().add("name", name).toString()
    }

    class Person(id: EntityID<UUID>): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<Person>(People)

        var name by People.name
        var city by City referencedOn People.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().add("name", name).add("city id", city.idValue).toString()
    }

    class Address(id: EntityID<UUID>): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<Address>(Addresses)

        var person by Person.referencedOn(Addresses.personId)
        var city by City.referencedOn(Addresses.cityId)
        var address by Addresses.address

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder().add("address", address).add("person id", person.idValue).add("city id", city.idValue)
                .toString()
    }

    class Town(id: EntityID<UUID>): TimebasedUUIDEntity(id) {
        companion object: TimebasedUUIDEntityClass<Town>(Towns)

        var city by City referencedOn Towns.cityId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().add("city id", city.idValue).toString()
    }
}

class Ex21_TimebasedUUIDTableEntity: AbstractExposedTest() {

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
            val seoul = City.new { name = "Seoul" }
            val busan = City.new { name = "Busan" }

            Person.new(Epoch.nextId()) {
                name = "Debop"
                city = seoul
            }
            Person.new(Epoch.nextId()) {
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
             * SELECT cities.id, cities."name" FROM cities
             * ```
             */
            val allCities = City.all().map { it.name }
            allCities shouldBeEqualTo listOf("Seoul", "Busan")

            /**
             * many-to-one Earger Loaing
             *
             * ```sql
             * SELECT people.id, people."name", people.city_id
             *   FROM people;
             *
             * SELECT cities.id, cities."name"
             *   FROM cities
             *  WHERE cities.id IN ('1efe168f-dd0c-6303-8372-b5321e042980', '1efe168f-dd0c-6305-8372-b5321e042980');
             * ```
             */
            val allPeople = Person.all().with(Person::city).map { it.name to it.city.name }
            allPeople shouldBeEqualTo listOf(
                "Debop" to "Seoul", "BTS" to "Seoul", "Sam" to "Busan"
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update and delete records`(testDB: TestDB) {
        withTables(testDB, Cities, People) {
            val seoul = City.new { name = "Seoul" }
            val busan = City.new { name = "Busan" }

            Person.new(Epoch.nextId()) {
                name = "Debop"
                city = seoul
            }
            Person.new(Epoch.nextId()) {
                name = "BTS"
                city = seoul
            }
            val sam = Person.new {
                name = "Sam"
                city = busan
            }

            // DELETE FROM people WHERE people.id = '1efe168f-dc48-6de1-8372-b5321e042980'
            sam.delete()
            // DELETE FROM cities WHERE cities.id = '1efe168f-dc48-6ddd-8372-b5321e042980'
            busan.delete()

            entityCache.clear()

            val allCities = City.all().map { it.name }
            allCities shouldBeEqualTo listOf("Seoul")

            /**
             * many-to-one Earger Loaing
             *
             * ```sql
             * SELECT people.id, people."name", people.city_id
             *   FROM people;
             *
             * SELECT cities.id, cities."name"
             *   FROM cities
             *  WHERE cities.id = '1efe168f-dc48-6ddb-8372-b5321e042980'
             * ```
             */
            val allPeople = Person.all().with(Person::city).map { it.name to it.city.name }
            allPeople shouldBeEqualTo listOf(
                "Debop" to "Seoul",
                "BTS" to "Seoul",
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with inner table`(testDB: TestDB) {
        withTables(testDB, Addresses, Cities, People) {
            val city1 = City.new { name = "City1" }
            val person1 = Person.new {
                name = "Person1"
                city = city1
            }
            val address1 = Address.new {
                person = person1
                city = city1
                address = "Address1"
            }
            val person2 = Person.new {
                name = "Person2"
                city = city1
            }
            val address2 = Address.new {
                person = person2
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
     *
     * ```sql
     * SELECT towns.id, towns.city_id FROM towns
     * SELECT cities.id, cities."name" FROM cities WHERE cities.id = '1efe168f-db41-62fd-8372-b5321e042980'
     * ```
     *
     * Eager loading referencedOn
     *
     * ```sql
     * SELECT towns.id, towns.city_id FROM towns
     * SELECT cities.id, cities."name" FROM cities WHERE cities.id = '1efe168f-db41-62fd-8372-b5321e042980'
     * SELECT cities.id, cities."name" FROM cities WHERE cities.id = '1efe168f-db41-62fd-8372-b5321e042980'
     * ```
     *
     * Lazy loading referrersOn
     * ```sql
     * SELECT cities.id, cities."name" FROM cities
     * SELECT towns.id, towns.city_id FROM towns WHERE towns.city_id = '1efe168f-db41-62fd-8372-b5321e042980'
     * SELECT cities.id, cities."name" FROM cities WHERE cities.id = '1efe168f-db41-62fd-8372-b5321e042980'
     * ```
     *
     * Eager loading referrersOn
     * ```sql
     * SELECT cities.id, cities."name" FROM cities
     * SELECT towns.id, towns.city_id, cities.id FROM towns INNER JOIN cities ON towns.city_id = cities.id WHERE towns.city_id = '1efe168f-db41-62fd-8372-b5321e042980'
     * SELECT cities.id, cities."name" FROM cities WHERE cities.id = '1efe168f-db41-62fd-8372-b5321e042980'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `foreign key between uuid and entity id column`(testDB: TestDB) {
        withTables(testDB, Cities, Towns) {
            val cId: EntityID<UUID> = Cities.insertAndGetId {
                it[name] = "City A"
            }
            val tId: EntityID<UUID> = Towns.insertAndGetId {
                it[cityId] = cId.value
            }
            val tId2: EntityID<UUID> = Towns.insertAndGetId {
                it[cityId] = cId.value
            }

            flushCache()

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
            city1WithTowns.towns.first().id shouldBeEqualTo tId
        }
    }
}
