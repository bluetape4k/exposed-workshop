package exposed.examples.jpa.relations.onetoone

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex02_UnidirectionalMapsIdOneToOne: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS car (
     *      id BIGSERIAL PRIMARY KEY,
     *      brand VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object CarTable: LongIdTable("car") {
        val brand = varchar("brand", 255)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS wheel (
     *      car_id BIGINT PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      diameter DOUBLE PRECISION NULL,
     *
     *      CONSTRAINT fk_wheel_car_id__id FOREIGN KEY (car_id)
     *      REFERENCES car(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     * ```
     */
    object WheelTable: IdTable<Long>("wheel") {
        override val id = reference("car_id", CarTable, onDelete = CASCADE, onUpdate = CASCADE)
        val name = varchar("name", 255)
        val diameter = double("diameter").nullable()

        override val primaryKey = PrimaryKey(WheelTable.id)
    }

    class Car(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Car>(CarTable)

        var brand by CarTable.brand
        // 양방향일 때 사용
        // val wheel by Wheel optionalBackReferencedOn WheelTable.id

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("brand", brand)
            // .add("wheel id", wheel?.idValue)
            .toString()
    }

    class Wheel(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Wheel>(WheelTable)

        var name by WheelTable.name
        var diameter by WheelTable.diameter
        val car by Car referencedOn WheelTable.id

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("diameter", diameter)
            .add("car id", car.idValue)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `unidirectional one to one with @MapsId`(testDB: TestDB) {
        withTables(testDB, CarTable, WheelTable) {

            val car = Car.new { brand = "BMW" }
            val wheel = Wheel.new(car.id.value) {
                name = "18-inch"
                diameter = 18.0
            }

            entityCache.clear()

            val wheel2 = Wheel.findById(wheel.id)!!
            wheel2 shouldBeEqualTo wheel
            wheel2.car shouldBeEqualTo car

            entityCache.clear()

            // Wheel 이 삭제되도, onwer 인 Car 는 삭제되지 않는다. (Car가 삭제되면 Wheel 도 삭제된다)
            wheel2.delete()
            Car.findById(car.id).shouldNotBeNull()
            // car.wheel.shouldBeNull()

            entityCache.clear()

            car.delete()

            // SELECT COUNT(car.id) FROM car
            Car.count() shouldBeEqualTo 0L
            // SELECT COUNT(*) FROM car
            Car.all().count() shouldBeEqualTo 0L

            // `Wheel.count()` 는 자신만의 entity id 가 없으므로 실행할 수 없다.
            // SELECT COUNT(*) FROM wheel
            Wheel.all().count() shouldBeEqualTo 0L
        }
    }
}
