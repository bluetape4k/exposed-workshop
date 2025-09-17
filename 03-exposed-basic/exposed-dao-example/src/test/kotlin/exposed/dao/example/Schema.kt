package exposed.dao.example

import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import exposed.shared.tests.withTables
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable

object Schema {

    /**
     * 도시 정보를 저장하는 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS cities (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * )
     * ```
     */
    object CityTable: IntIdTable("cities") {
        val name = varchar("name", 50)
    }

    /**
     * 사용자 정보를 저장하는 테이블
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS users (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      age INT NOT NULL,
     *      city_id INT NULL,
     *
     *      CONSTRAINT fk_users_city_id__id FOREIGN KEY (city_id)
     *          REFERENCES cities(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object UserTable: IntIdTable("users") {
        val name = varchar("name", 50)
        val age = integer("age")
        val cityId = optReference("city_id", CityTable)
    }


    class City(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<City>(CityTable)

        var name: String by CityTable.name
        val users: SizedIterable<User> by User optionalReferrersOn UserTable.cityId

        override fun equals(other: Any?): Boolean = other is City && id._value == other.id._value
        override fun hashCode(): Int = id._value.hashCode()
        override fun toString(): String = "City(id=$id, name=$name)"
    }

    class User(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<User>(UserTable)

        var name: String by UserTable.name
        var age: Int by UserTable.age

        // many-to-one
        var city: City? by City optionalReferencedOn UserTable.cityId

        override fun equals(other: Any?): Boolean = other is User && id._value == other.id._value
        override fun hashCode(): Int = id._value.hashCode()
        override fun toString(): String = "User(id=$id, name=$name, age=$age, city=$city)"
    }

    fun withCityUsers(testDB: TestDB, statement: JdbcTransaction.() -> Unit) {
        withTables(testDB, CityTable, UserTable) {
            populateSamples()

            flushCache()
            entityCache.clear()

            statement()
        }
    }

    suspend fun withSuspendedCityUsers(testDB: TestDB, statement: suspend JdbcTransaction.() -> Unit) {
        withSuspendedTables(testDB, CityTable, UserTable) {
            populateSamples()

            flushCache()
            entityCache.clear()

            statement()
        }
    }

    fun populateSamples() {
        val seoul = City.new { name = "Seoul" }
        val busan = City.new { name = "Busan" }
        val daegu = City.new { name = "Daegu" }

        seoul.shouldNotBeNull()
        busan.shouldNotBeNull()
        daegu.shouldNotBeNull()

        User.new {
            name = "debop"
            age = 56
            city = seoul
        }
        User.new {
            name = "jane"
            age = 27
            city = seoul
        }
        User.new {
            name = "john"
            age = 33
            city = busan
        }
        User.new {
            name = "alex"
            age = 44
            city = null
        }
    }
}
