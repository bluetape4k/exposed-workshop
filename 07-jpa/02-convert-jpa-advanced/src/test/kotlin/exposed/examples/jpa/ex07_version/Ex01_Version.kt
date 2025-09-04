package exposed.examples.jpa.ex07_version

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.sql.SQLException
import kotlin.test.assertFailsWith

/**
 * 05-exposed-dml/01-dml/ Ex04_Upsert 를 참고하세요.
 */
class Ex01_Version: JdbcExposedTestBase() {

    // these DB require key columns from ON clause to be included in the derived source table (USING clause)
    private val upsertViaMergeDB = TestDB.ALL_H2

    interface VersionedEntity {
        val version: Int
    }

    internal object Products: IntIdTable("products") {
        val name = varchar("name", 50)
        val price = decimal("price", 10, 2)
        val version = integer("version").default(0)
    }

    internal class Product(id: EntityID<Int>): IntEntity(id), VersionedEntity {
        companion object: IntEntityClass<Product>(Products)

        var name: String by Products.name
        var price: BigDecimal by Products.price

        override var version: Int by Products.version
            private set

        /**
         * Optimistic locking 적용
         * NOTE: Exposed 에서는 findByIdAndUpdate() 처럼 DB의 Pessimistic Locking 을 사용하세요.
         */
        override fun flush(batch: EntityBatchUpdate?): Boolean {
            log.debug { "flush() called" }
            if (writeValues.isNotEmpty()) {
                writeValues.forEach { (column, value) ->
                    log.debug { "column: $column, value: $value" }
                }
                val matched = Product.count((Products.id eq id) and (Products.version eq version)) == 1L
                if (matched) {
                    version += 1
                } else {
                    // return false
                    throw SQLException("Version mismatch: expected $version, but record has been modified by another transaction.")
                }
            }
            return super.flush(batch)
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("price", price)
            .add("version", version)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update versioned record`(testDB: TestDB) {
        withTables(testDB, Products) {
            // Insert a product with version 0
            val id = Products.insertAndGetId {
                it[name] = "Product A"
                it[price] = 100.0.toBigDecimal()
            }
            Products.selectAll().forEach {
                println("Product: ${it[Products.name]}, Price: ${it[Products.price]}, Version: ${it[Products.version]}")
            }

            // Check the initial version
            val updatedRows = Products.update({ Products.id eq id and (Products.version eq 0) }) {
                it[name] = "Updated Product A"
                it[version] = Products.version + 1
            }
            Products.selectAll().forEach {
                println("Product: ${it[Products.name]}, Price: ${it[Products.price]}, Version: ${it[Products.version]}")
            }
            updatedRows shouldBeEqualTo 1

            // 앞서 업데이트가 되었기 때문에 version=0 인 레코드는 없다.
            val updatedRows2 = Products.update({ Products.id eq id and (Products.version eq 0) }) {
                it[name] = "Updated Product A"
                it[version] = Products.version + 1
            }
            Products.selectAll().forEach {
                println("Product: ${it[Products.name]}, Price: ${it[Products.price]}, Version: ${it[Products.version]}")
            }
            updatedRows2 shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update versioned entity`(testDB: TestDB) {
        withTables(testDB, Products) {
            // 1. Insert a product with version 0
            val p1 = Product.new {
                name = "Product A"
                price = 100.0.toBigDecimal()
            }
            flushCache()

            Product.all().forEach {
                println(it)
            }

            // 2. p1 엔티티 를 업데이트 한다. (버전이 1로 올라간다)
            p1.name = "Updated Product A"

            entityCache.clear()

            Product.all().forEach {
                println(it)
            }

            // 3. DSL 로 업데이트 한다. (버전이 100로 올라간다)
            Products.update({ Products.id eq p1.id }) {
                it[name] = "Updated Product A - Other transaction"
                it[version] = 2
            }

            // 4. p1 을 Update 한다 (p1의 version은 1이므로, 업데이트가 되지 않는다)
            assertFailsWith<SQLException> {
                p1.name = "Updated Product A 2"
                flushCache()
            }
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert versioned record`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }
        withTables(testDB, Products) {
            // INSERT
            val upsertedRows = Products.upsert(onUpdate = { it[Products.version] = Products.version + 1 }) {
                if (testDB in upsertViaMergeDB)
                    it[Products.id] = 1
                it[Products.name] = "Product B"
                it[Products.price] = 200.0.toBigDecimal()
                it[Products.version] = 0
            }
            Products.selectAll().forEach {
                println("Product: ${it[Products.name]}, Price: ${it[Products.price]}, Version: ${it[Products.version]}")
            }

            // UPDATE 된다. (id=2, version=1)
            val results = Products.upsert(Products.id, onUpdate = { it[Products.version] = Products.version + 1 }) {
                if (testDB in upsertViaMergeDB)
                    it[Products.id] = 2
                else
                    it[Products.id] = 1
                it[Products.name] = "Product B - Updated"
                it[Products.price] = 300.0.toBigDecimal()
                it[Products.version] = 0
            }
            Products.selectAll().forEach {
                println("Product: ${it[Products.name]}, Price: ${it[Products.price]}, Version: ${it[Products.version]}")
            }
            results.insertedCount shouldBeEqualTo 1

            // UPDATE 된다.
            // assertFailsWith<ExposedSQLException> {
            val failed = Products.upsert(
                keys = arrayOf(Products.id),
                onUpdate = { it[Products.version] = Products.version + 1 },
                where = { Products.id eq 1 and (Products.version eq 0) }) {
                it[Products.id] = 1
                it[Products.name] = "Product B - Updated 2"
                it[Products.price] = 300.0.toBigDecimal()
                it[Products.version] = 0
            }
            failed.insertedCount shouldBeEqualTo 0
            // }
            Products.selectAll().forEach {
                println("Product: ${it[Products.name]}, Price: ${it[Products.price]}, Version: ${it[Products.version]}")
            }
        }
    }


}
