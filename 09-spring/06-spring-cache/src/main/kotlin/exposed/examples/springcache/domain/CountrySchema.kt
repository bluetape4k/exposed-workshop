package exposed.examples.springcache.domain

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import java.io.Serializable

/**
 * 국가 정보를 저장하는 테이블.
 *
 * `countries` 테이블에 매핑되며, 2자리 국가 코드에 유니크 인덱스가 설정되어 있습니다.
 */
object CountryTable: IntIdTable("countries") {
    val code = char("code", 2).uniqueIndex()
    val name = varchar("name", 50)
    val description = text("description").nullable()
}

/**
 * 국가 DAO 엔티티.
 *
 * [CountryTable]에 매핑됩니다.
 * 예제에서는 사용하지 않고, [CountryRecord]를 사용합니다.
 *
 * @param id 엔티티 식별자
 */
class Country(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Country>(CountryTable)

    var code by CountryTable.code
    var name by CountryTable.name
    var description by CountryTable.description

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = entityToStringBuilder()
        .add("code", code)
        .add("name", name)
        .add("description", description)
        .toString()
}

/**
 * 국가 정보를 담는 데이터 레코드.
 *
 * Spring Cache의 캐시 키/값으로 사용하기 위해 [Serializable]을 구현합니다.
 *
 * @property code 2자리 국가 코드 (예: "KR", "US")
 * @property name 국가 이름
 * @property description 국가 설명 (선택적)
 */
data class CountryRecord(
    val code: String,
    val name: String,
    val description: String? = null,
): Serializable
