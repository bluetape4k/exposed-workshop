package exposed.examples.measured

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.measured.Length
import io.bluetape4k.measured.Mass
import io.bluetape4k.measured.celsius
import io.bluetape4k.measured.centimeters
import io.bluetape4k.measured.fahrenheit
import io.bluetape4k.measured.kilograms
import io.bluetape4k.measured.meters
import io.bluetape4k.exposed.core.measured.MeasureColumnType
import io.bluetape4k.measured.Measure
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 측정값 컬럼을 DSL과 DAO에서 사용하는 JDBC 예제입니다.
 *
 * 저장 시에는 각 측정값이 테이블이 선언한 기준 단위로 정규화되고,
 * 조회 시에는 타입 정보를 유지한 [Measure] 또는 [Temperature]로 복원됩니다.
 */
class Ex01MeasuredColumns: AbstractExposedTest() {

    /** DSL 컬럼이 단위 변환 후 기준 단위로 round-trip 되는지 검증합니다. */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL measured 컬럼은 기준 단위로 round-trip 된다`(testDB: TestDB) {
        withTables(testDB, ProductTable) {
            val id = ProductTable.insertAndGetId {
                it[ProductTable.name] = "온도 센서"
                it[ProductTable.length] = 150.centimeters()
                it[ProductTable.mass] = 2.5.kilograms()
                it[ProductTable.temperature] = 77.fahrenheit()
            }

            val row = ProductTable
                .selectAll()
                .where { ProductTable.id eq id }
                .single()

            (row[ProductTable.length] `in` Length.meters).shouldBeNear(1.5, 1e-10)
            (row[ProductTable.mass] `in` Mass.kilograms).shouldBeNear(2.5, 1e-10)
            row[ProductTable.temperature].inCelsius().shouldBeNear(25.0, 1e-10)
        }
    }

    /** DAO 엔티티도 같은 측정값 컬럼 계약을 사용하고 nullable 값을 보존하는지 검증합니다. */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO measured 컬럼은 nullable 값을 보존한다`(testDB: TestDB) {
        withTables(testDB, ProductTable) {
            val product = ProductEntity.new {
                name = "정밀 저울"
                length = 1.5.meters()
                mass = 2.5.kilograms()
                nullableMass = null
                temperature = 25.celsius()
            }
            flushCache()

            val reloaded = ProductEntity.findById(product.id)
            reloaded!!.length.let { (it `in` Length.centimeters).shouldBeNear(150.0, 1e-10) }
            reloaded.mass.let { (it `in` Mass.kilograms).shouldBeNear(2.5, 1e-10) }
            reloaded.nullableMass.shouldBeNull()
            reloaded.temperature.inCelsius().shouldBeNear(25.0, 1e-10)
        }
    }

    /** Double 기반 저장의 작은 소수 오차는 허용 오차로 비교합니다. */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `정밀한 측정값은 허용 오차 안에서 round-trip 된다`(testDB: TestDB) {
        withTables(testDB, ProductTable) {
            val expected = 123_456.789123.meters()
            val id = ProductTable.insertAndGetId {
                it[ProductTable.name] = "정밀 길이"
                it[ProductTable.length] = expected
                it[ProductTable.mass] = 0.000123.kilograms()
                it[ProductTable.temperature] = 273.15.celsius()
            }

            val row = ProductTable.selectAll().where { ProductTable.id eq id }.single()
            (row[ProductTable.length] `in` Length.meters).shouldBeNear(123_456.789123, 1e-6)
            (row[ProductTable.mass] `in` Mass.kilograms).shouldBeNear(0.000123, 1e-12)
        }
    }

    /** provider가 지원하지 않는 DB 값 타입을 조용히 허용하지 않는지 검증합니다. */
    @Test
    fun `MeasureColumnType은 지원하지 않는 DB 값 타입을 거부한다`() {
        val columnType = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }

        assertFailsWith<IllegalStateException> {
            columnType.valueFromDB("not-a-number")
        }
    }
}
