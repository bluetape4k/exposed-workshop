package exposed.examples.springcache.domain

import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object CountryCodeTable: IntIdTable("country_codes") {
    val code = varchar("code", 2).uniqueIndex()
    val name = varchar("name", 50)
    val description = text("description").nullable()
}

class CountryCode(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<CountryCode>(CountryCodeTable)

    var code by CountryCodeTable.code
    var name by CountryCodeTable.name
    var description by CountryCodeTable.description

    override fun toString(): String = toStringBuilder()
        .add("code", code)
        .add("name", name)
        .add("description", description)
        .toString()
}
