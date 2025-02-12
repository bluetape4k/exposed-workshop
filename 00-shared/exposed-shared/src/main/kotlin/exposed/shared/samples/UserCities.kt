package exposed.shared.samples

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table

/**
 * ```sql
 * CREATE TABLE IF NOT EXISTS "User" (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL,
 *      age INT NOT NULL
 * );
 *
 * CREATE INDEX user_name ON "User" ("name");
 * ```
 */
object UserTable: IntIdTable() {
    val name = varchar("name", 50).index()
    val age = integer("age")
}

/**
 * ```sql
 * CREATE TABLE IF NOT EXISTS city (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL,
 *      country_id INT NOT NULL,
 *
 *      CONSTRAINT fk_city_country_id__id FOREIGN KEY (country_id) REFERENCES country(id)
 *          ON DELETE RESTRICT ON UPDATE RESTRICT
 * );
 *
 * CREATE INDEX city_name ON city ("name");
 * ALTER TABLE city ADD CONSTRAINT city_country_id_name_unique UNIQUE (country_id, "name");
 * ```
 */
object CityTable: IntIdTable() {
    val name = varchar("name", 50).index()
    val countryId = reference("country_id", CountryTable)  // many-to-one

    init {
        uniqueIndex(countryId, name)
    }
}

/**
 * ```sql
 * CREATE TABLE IF NOT EXISTS country (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL
 * );
 *
 * ALTER TABLE country ADD CONSTRAINT country_name_unique UNIQUE ("name")
 * ```
 */
object CountryTable: IntIdTable() {
    val name = varchar("name", 50).uniqueIndex()
}

/**
 * City - User  Many-to-many relationship table
 *
 * ```sql
 * CREATE TABLE IF NOT EXISTS usertocity (
 *      user_id INT NOT NULL,
 *      city_id INT NOT NULL,
 *
 *      CONSTRAINT fk_usertocity_user_id__id FOREIGN KEY (user_id) REFERENCES "User"(id)
 *          ON DELETE CASCADE ON UPDATE RESTRICT,
 *
 *      CONSTRAINT fk_usertocity_city_id__id FOREIGN KEY (city_id) REFERENCES city(id)
 *          ON DELETE CASCADE ON UPDATE RESTRICT
 * );
 * ```
 */
object UserToCityTable: Table() {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val cityId = reference("city_id", CityTable, onDelete = ReferenceOption.CASCADE)
}

class User(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<User>(UserTable)

    var name by UserTable.name
    var age by UserTable.age
    var cities: SizedIterable<City> by City via UserToCityTable      // many-to-many

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String {
        return toStringBuilder()
            .add("name", name)
            .add("age", age)
            .toString()
    }
}

class City(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<City>(CityTable)

    var name by CityTable.name
    var country: Country by Country referencedOn CityTable.countryId   // many-to-one
    var users: SizedIterable<User> by User via UserToCityTable       // many-to-many

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String {
        return toStringBuilder()
            .add("id", id)
            .add("name", name)
            .add("country", country)
            .toString()
    }
}

class Country(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Country>(CountryTable)

    var name by CountryTable.name
    val cities: SizedIterable<City> by City referrersOn CityTable.countryId   // one-to-many

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String {
        return toStringBuilder()
            .add("id", id)
            .add("name", name)
            .toString()
    }
}
