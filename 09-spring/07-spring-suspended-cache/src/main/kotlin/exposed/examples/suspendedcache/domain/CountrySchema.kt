package exposed.examples.suspendedcache.domain

import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object CountryTable: IntIdTable("countries") {
    val code = char("code", 2).uniqueIndex()
    val name = varchar("name", 50)
    val description = text("description").nullable()
}

// 예제에서는 사용하지 않고, [Country] DTO 를 사용합니다.
class Country(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Country>(CountryTable)

    var code by CountryTable.code
    var name by CountryTable.name
    var description by CountryTable.description

    override fun toString(): String = toStringBuilder()
        .add("code", code)
        .add("name", name)
        .add("description", description)
        .toString()
}

data class CountryDTO(
    val code: String,
    val name: String,
    val description: String? = null,
)
