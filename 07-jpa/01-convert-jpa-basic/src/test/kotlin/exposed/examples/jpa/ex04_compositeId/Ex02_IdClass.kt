package exposed.examples.jpa.ex04_compositeId

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertIs

class Ex02_IdClass: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * CompositeIdTable 를 사용하여 Entity 를 정의합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS car_table (
     *      car_brand VARCHAR(64),
     *      car_year INT,
     *      serial_no VARCHAR(32) NULL,
     *
     *      CONSTRAINT pk_car_table PRIMARY KEY (car_brand, car_year)
     * );
     * ```
     */
    object CarTable: CompositeIdTable("car_table") {
        val brand = varchar("car_brand", 64).entityId()
        val carYear = integer("car_year").entityId()
        val serialNo = varchar("serial_no", 32).nullable()

        override val primaryKey = PrimaryKey(brand, carYear)
    }

    class IdClassCar(id: EntityID<CompositeID>): CompositeEntity(id) {
        companion object: CompositeEntityClass<IdClassCar>(CarTable) {
            /**
             * CompositeID를 이용한 Entity를 생성합니다.
             */
            fun new(brand: String, carYear: Int, init: IdClassCar.() -> Unit): IdClassCar {
                brand.requireNotBlank("brand")
                carYear.requirePositiveNumber("carYear")

                val compositeId = carCompositeIdOf(brand, carYear)
                return new(compositeId, init)
            }

            fun carCompositeIdOf(brand: String, carYear: Int): CompositeID {
                return CompositeID {
                    it[CarTable.brand] = brand
                    it[CarTable.carYear] = carYear
                }
            }
        }

        val brand by CarTable.brand
        val carYear by CarTable.carYear
        var serialNo by CarTable.serialNo

        val carIdentifier get() = CarIdentifier(id.value)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("serial no", serialNo)
            .toString()
    }

    data class CarIdentifier(val compositeId: CompositeID): EntityID<CompositeID>(CarTable, compositeId) {
        val brand: String get() = compositeId[CarTable.brand].value
        val carYear: Int get() = compositeId[CarTable.carYear].value
    }

    fun newIdClassCar(): IdClassCar {
        val brand = faker.company().name()
        val carYear = faker.random().nextInt(1950, 2023)

        return IdClassCar.new(brand, carYear) {
            serialNo = faker.random().nextLong().toString(32)
        }
    }

    /**
     * CompositeID 를 사용하여 EntityID 를 생성하고 읽습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `composite id entity`(testDB: TestDB) {
        withTables(testDB, CarTable) {
            /**
             * CompositeID 를 사용하여 EntityID 를 생성합니다.
             */
            val compositeId = CompositeID {
                it[CarTable.brand] = faker.company().name()
                it[CarTable.carYear] = faker.random().nextInt(1950, 2023)
            }
            val car1 = IdClassCar.new(compositeId) {
                serialNo = faker.random().nextLong().toString(32)
            }

            entityCache.clear()

            val loaded1 = IdClassCar.findById(car1.carIdentifier)!!
            loaded1 shouldBeEqualTo car1
            loaded1.carIdentifier shouldBeEqualTo car1.carIdentifier

            /**
             * 새로운 new 함수를 사용하여 CompositeID를 가진 Entity 를 생성합니다.
             */
            val brand = faker.company().name()
            val carYear = faker.random().nextInt(1950, 2023)
            val car2 = IdClassCar.new(brand, carYear) {
                serialNo = faker.random().nextLong().toString(32)
            }

            entityCache.clear()

            val loaded2 = IdClassCar.findById(car2.carIdentifier)!!
            loaded2 shouldBeEqualTo car2
            loaded2.carIdentifier shouldBeEqualTo CarIdentifier(car2.id.value)

            val allCars = IdClassCar.all().toList()
            allCars.forEach { log.debug { it } }
            allCars shouldHaveSize 2


            val searched1 = IdClassCar.find { CarTable.brand eq car1.brand }.single()
            searched1 shouldBeEqualTo car1

            /**
             * ```sql
             * -- Postgres
             * DELETE FROM car_table
             *  WHERE (car_table.car_brand = 'Morissette, Gorczany and Boehm')
             *    AND (car_table.car_year = 1983)
             * ```
             */
            val deletedCount = CarTable.deleteWhere { CarTable.id eq compositeId }
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `compoisite id with DSL`(testDB: TestDB) {
        withTables(testDB, CarTable) {

            val brand = faker.company().name()
            val carYear = faker.random().nextInt(1950, 2023)
            val serialNo = faker.random().nextLong().toString(32)

            val id = CarTable.insertAndGetId {
                it[CarTable.brand] = brand
                it[CarTable.carYear] = carYear
                it[CarTable.serialNo] = serialNo
            }
            entityCache.clear()

            val result = CarTable.selectAll().single()

            result[CarTable.serialNo] shouldBeEqualTo serialNo

            val idResult: EntityID<CompositeID> = result[CarTable.id]
            assertIs<EntityID<CompositeID>>(idResult)

            val resultBrand: EntityID<String> = idResult.value[CarTable.brand]
            val resultCarYear: EntityID<Int> = idResult.value[CarTable.carYear]

            resultBrand.value shouldBeEqualTo brand
            resultCarYear.value shouldBeEqualTo carYear


            /**
             * Query by [CompositeID]
             *
             * ```sql
             * -- Postgres
             * SELECT car_table.car_brand,
             *        car_table.car_year
             *   FROM car_table
             *  WHERE (car_table.car_brand = ?)
             *    AND (car_table.car_year = ?)
             * ```
             */
            val dslQuery = CarTable.select(CarTable.id)
                .where { CarTable.id eq idResult }
                .prepareSQL(this, true)

            log.debug { "DSL Query: $dslQuery" }

            val entityId = EntityID(
                CompositeID {
                    it[CarTable.brand] = brand
                    it[CarTable.carYear] = carYear
                },
                CarTable
            )

            /**
             * CompositeID를 이용하여 Entity를 조회합니다.
             *
             * ```sql
             * SELECT car_table.car_brand,
             *        car_table.car_year,
             *        car_table.serial_no
             *   FROM car_table
             *  WHERE (car_table.car_brand = 'Schaden, Koepp and Spinka')
             *    AND (car_table.car_year = 1967)
             * ```
             */
            CarTable.selectAll().where { CarTable.id eq entityId }.toList() shouldHaveSize 1

            /**
             * Composite ID의 부분 컬럼만 사용하여 조회합니다.
             */
            CarTable.selectAll()
                .where { CarTable.brand neq resultBrand }
                .count() shouldBeEqualTo 0L

        }
    }

}
