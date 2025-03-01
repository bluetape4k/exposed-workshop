package exposed.examples.jpa.ex04_relations.ex02_one_to_many

import exposed.examples.jpa.ex04_relations.ex02_one_to_many.Ex07_OneToMany_Map.CarOptionTable
import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

/**
 * one-to-many relation 방식을 List가 아닌 Map으로 표현하는 방법
 */
class Ex07_OneToMany_Map: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many with embeddable mapped by @CollectionTable`(testDB: TestDB) {
        withTables(testDB, CarTable, CarOptionTable, CarPartTable, CarPartMapTable) {
            val car = Car.new { name = "BMW" }
            val option1 = CarOption("i-Navi", 40)
            val option2 = CarOption("JBL", 60)
            val option3 = CarOption("Diamond Black Wheel", 128)

            car.addOption("Navigation", option1)
            car.addOption("Audio", option2)
            car.addOption("Wheel", option3)

            flushCache()
            entityCache.clear()

            // eager loading
            val loaded = Car.findById(car.id)!!
            loaded shouldBeEqualTo car
            car.options.values shouldContainSame listOf(option1, option2, option3)
            val options = car.options
            options["Navigation"] shouldBeEqualTo option1
            options["Audio"] shouldBeEqualTo option2
            options["Wheel"] shouldBeEqualTo option3

            // Remove Option
            car.removeOption("Audio")
            entityCache.clear()

            val loaded2 = Car.findById(car.id)!!
            loaded2 shouldBeEqualTo car
            loaded2.options.values shouldContainSame listOf(option1, option3)

            // Remove Car
            car.delete()
            entityCache.clear()

            CarTable.selectAll().count() shouldBeEqualTo 0L
            CarOptionTable.selectAll().count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many with entity mapped by @JoinTable`(testDB: TestDB) {
        withTables(testDB, CarTable, CarOptionTable, CarPartTable, CarPartMapTable) {
            val car = Car.new { name = "BMW" }
            val engine = CarPart.new { name = "Engine-B48" }
            val misson = CarPart.new { name = "Misson-ZF8" }
            val fueltank = CarPart.new { name = "FuelTank-60L" }

            car.addPart("engine", engine)
            car.addPart("mission", misson)
            car.addPart("fueltank", fueltank)

            entityCache.clear()

            val loaded = Car.findById(car.id)!!
            loaded shouldBeEqualTo car
            loaded.parts.values shouldContainSame listOf(engine, misson, fueltank)

            fueltank.delete()
            entityCache.clear()

            val loaded2 = Car.findById(car.id)!!
            loaded2 shouldBeEqualTo car
            loaded2.parts.values shouldContainSame listOf(engine, misson)

            car.delete()
            entityCache.clear()

            CarTable.selectAll().count() shouldBeEqualTo 0L
            CarPartMapTable.selectAll().count() shouldBeEqualTo 0L

            // `CarPart` 엔티티는 삭제되지 않는다.
            CarPartTable.selectAll().count() shouldBeEqualTo 2L
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS car (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     *
     * ALTER TABLE car ADD CONSTRAINT car_name_unique UNIQUE ("name");
     * ```
     */
    object CarTable: IntIdTable("car") {
        val name = varchar("name", 255).uniqueIndex()
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS car_option (
     *      car_id INT NOT NULL,
     *      option_key VARCHAR(255) NOT NULL,
     *      "name" VARCHAR(255) NOT NULL,
     *      price INT DEFAULT 0 NOT NULL,
     *
     *      CONSTRAINT fk_car_option_car_id__id FOREIGN KEY (car_id)
     *      REFERENCES car(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     *
     * ALTER TABLE car_option
     *      ADD CONSTRAINT car_option_car_id_option_key_unique UNIQUE (car_id, option_key);
     * ```
     */
    object CarOptionTable: Table("car_option") {
        val carId = reference("car_id", CarTable, onDelete = CASCADE, onUpdate = CASCADE)
        val optionKey = varchar("option_key", 255)
        val name = varchar("name", 255)
        val price = integer("price").default(0)

        init {
            uniqueIndex(carId, optionKey)
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS car_part (
     *      id SERIAL PRIMARY KEY,
     *      part_name VARCHAR(255) NOT NULL,
     *      description TEXT NULL
     * );
     * ```
     */
    object CarPartTable: IntIdTable("car_part") {
        val name = varchar("part_name", 255)
        val descriptin = text("description").nullable()
    }

    /**
     * [CarTable] 과 [CarPartTable] 의 관계를 one-to-many map 방식으로 표현하기 위한 조인 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS car_part_map (
     *      car_id INT NOT NULL,
     *      part_key VARCHAR(255) NOT NULL,
     *      part_id INT NOT NULL,
     *
     *      CONSTRAINT fk_car_part_map_car_id__id FOREIGN KEY (car_id)
     *      REFERENCES car(id) ON DELETE CASCADE ON UPDATE CASCADE,
     *
     *      CONSTRAINT fk_car_part_map_part_id__id FOREIGN KEY (part_id)
     *      REFERENCES car_part(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     *
     * ALTER TABLE car_part_map
     *      ADD CONSTRAINT car_part_map_car_id_part_key_unique UNIQUE (car_id, part_key);
     * ```
     */
    object CarPartMapTable: Table("car_part_map") {
        val carId = reference("car_id", CarTable, onDelete = CASCADE, onUpdate = CASCADE)
        val partKey = varchar("part_key", 255)
        val partId = reference("part_id", CarPartTable, onDelete = CASCADE, onUpdate = CASCADE)

        init {
            uniqueIndex(carId, partKey)
        }
    }

    class Car(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Car>(CarTable)

        var name by CarTable.name

        // OneToMany Map with MapKeyColumn (optionKey: CarOption)  (CarOption is Component/Embeddable)
        // JoinTable: CarOptionTable
        val options: Map<String, CarOption>
            get() = CarOptionTable.selectAll()
                .where { CarOptionTable.carId eq this@Car.id }
                .associate { it[CarOptionTable.optionKey] to it.toCarOption() }

        fun addOption(optionKey: String, option: CarOption) {
            CarOptionTable.insert {
                it[CarOptionTable.carId] = this@Car.id
                it[CarOptionTable.optionKey] = optionKey

                it[CarOptionTable.name] = option.name
                it[CarOptionTable.price] = option.price
            }
        }

        fun removeOption(optionKey: String): Int {
            return CarOptionTable.deleteWhere {
                (CarOptionTable.carId eq this@Car.id) and (CarOptionTable.optionKey eq optionKey)
            }
        }

        // OneToMany Map with MapKeyColumn (partKey: CarPart) (CarPart is Entity)
        // JoinTable: CarPartMapTable
        val parts: Map<String, CarPart>
            get() = CarPartMapTable.innerJoin(CarPartTable)
                .select(listOf(CarPartMapTable.partKey) + CarPartTable.columns)
                .where { CarPartMapTable.carId eq this@Car.id }
                .associate { it[CarPartMapTable.partKey] to CarPart.wrapRow(it) }

        fun addPart(partKey: String, part: CarPart) {
            CarPartMapTable.insert {
                it[CarPartMapTable.carId] = this@Car.id
                it[CarPartMapTable.partKey] = partKey
                it[CarPartMapTable.partId] = part.id
            }
        }

        fun removePart(partKey: String): Int {
            return CarPartMapTable.deleteWhere {
                (CarPartMapTable.carId eq this@Car.id) and (CarPartMapTable.partKey eq partKey)
            }
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("id", id)
            .add("name", name)
            .toString()
    }

    class CarPart(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<CarPart>(CarPartTable)

        var name by CarPartTable.name
        var description by CarPartTable.descriptin

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("description", description)
            .toString()
    }
}

data class CarOption(
    val name: String,
    val price: Int = 0,
): Serializable

fun ResultRow.toCarOption(): CarOption = CarOption(
    name = this[CarOptionTable.name],
    price = this[CarOptionTable.price]
)
