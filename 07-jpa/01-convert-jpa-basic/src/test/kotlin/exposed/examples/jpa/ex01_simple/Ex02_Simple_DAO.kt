package exposed.examples.jpa.ex01_simple

import exposed.examples.jpa.ex01_simple.SimpleSchema.SimpleEntity
import exposed.examples.jpa.ex01_simple.SimpleSchema.SimpleTable
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class Ex02_Simple_DAO: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `equals for entities`(testDB: TestDB) {
        withTables(testDB, SimpleTable) {
            val name1 = faker.name().name()
            val name2 = faker.name().name()

            val entity1 = SimpleEntity.new {
                name1.requireNotBlank("name1")
                name = name1
            }
            val entity2 = SimpleEntity.new {
                name2.requireNotBlank("name2")
                name = name2
            }

            entityCache.clear()

            val persisted1 = SimpleEntity.findById(entity1.id)!!
            val persisted2 = SimpleEntity.findById(entity2.id)!!

            persisted1 shouldBeEqualTo entity1
            persisted2 shouldBeEqualTo entity2
            persisted1 shouldNotBeEqualTo persisted2
        }
    }

    @Test
    fun `if name is empty, throw exception`() {
        assertFailsWith<IllegalArgumentException> {
            SimpleEntity.new("")
        }
    }

    /**
     * Unique index violation
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `violance unique index`(testDB: TestDB) {
        withTables(testDB, SimpleTable) {
            val name = faker.name().name()

            SimpleEntity.new { this.name = name }
            commit()

            // name 속성은 unique 하므로, 두번째 insert 는 실패합니다.
            assertFailsWith<ExposedSQLException> {
                SimpleEntity.new { this.name = name }
                commit()
            }
        }
    }

    /**
     * [batchInsert] 로 Batch Insert 작업을 수행합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert records and load all`(testDB: TestDB) {
        withTables(testDB, SimpleTable) {
            val entityCount = 10

            val names = List(entityCount) { faker.name().name() }.distinct()
            SimpleTable.batchInsert(names) { name ->
                this[SimpleTable.name] = name
                this[SimpleTable.description] = faker.lorem().paragraph()
            }

            // SQL DSL 로 조회
            SimpleTable.selectAll().count() shouldBeEqualTo names.size.toLong()

            // DAO 로 조회
            SimpleEntity.all().count() shouldBeEqualTo names.size.toLong()
        }
    }
}
