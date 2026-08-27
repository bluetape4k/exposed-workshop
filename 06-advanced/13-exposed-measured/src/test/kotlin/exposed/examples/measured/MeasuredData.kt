package exposed.examples.measured

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.measured.Length
import io.bluetape4k.measured.Mass
import io.bluetape4k.exposed.core.measured.length
import io.bluetape4k.exposed.core.measured.mass
import io.bluetape4k.exposed.core.measured.temperature
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntity

/**
 * 측정값을 기준 단위([Length.meters], [Mass.kilograms], Kelvin)로 저장하는 예제 테이블입니다.
 *
 * 모든 측정 컬럼은 `DOUBLE`로 저장되지만 애플리케이션에서는 측정 family가 보존됩니다.
 */
internal object ProductTable: IntIdTable("measured_products") {
    val name = varchar("name", 100)
    val length = length("length")
    val mass = mass("mass")
    val nullableMass = mass("nullable_mass").nullable()
    val temperature = temperature("temperature")
}

/** DSL 테이블을 DAO 엔티티로 사용하는 measured 예제입니다. */
internal class ProductEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: EntityClass<Int, ProductEntity>(ProductTable)

    var name by ProductTable.name
    var length by ProductTable.length
    var mass by ProductTable.mass
    var nullableMass by ProductTable.nullableMass
    var temperature by ProductTable.temperature

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = entityToStringBuilder()
        .add("name", name)
        .add("length", length)
        .add("mass", mass)
        .add("nullableMass", nullableMass)
        .add("temperature", temperature)
        .toString()
}
