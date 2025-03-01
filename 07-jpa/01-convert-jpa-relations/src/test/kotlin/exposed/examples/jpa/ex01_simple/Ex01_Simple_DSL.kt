package exposed.examples.jpa.ex01_simple

import exposed.examples.jpa.ex01_simple.SimpleSchema.SimpleEntity
import exposed.examples.jpa.ex01_simple.SimpleSchema.SimpleTable
import exposed.examples.jpa.ex01_simple.SimpleSchema.toSimpleDTOs
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [SimpleTable] 을 DSL 을 이용하여 작업하는 예제
 */
class Ex01_Simple_DSL: AbstractExposedTest() {

    companion object: KLogging() {
        private const val ENTITY_COUNT = 10
    }

    private fun Transaction.persistSimpleEntities() {
        val names = List(ENTITY_COUNT) { faker.name().name() }

        SimpleTable.batchInsert(names) { name ->
            this[SimpleTable.name] = name
            this[SimpleTable.description] = faker.lorem().sentence()
        }

        flushCache()
        entityCache.clear()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find by names`(testDB: TestDB) {
        withTables(testDB, SimpleTable) {

            persistSimpleEntities()

            /**
             * ```sql
             * SELECT simple_entity."name"
             *   FROM simple_entity
             *  LIMIT 2
             *  OFFSET 2
             * ```
             */
            val names: List<String> = SimpleTable
                .select(SimpleTable.name)
                .limit(2)
                .offset(2)
                .map { it[SimpleTable.name] }

            /**
             * ```sql
             * SELECT simple_entity.id,
             *        simple_entity."name",
             *        simple_entity.description
             *   FROM simple_entity
             *  WHERE simple_entity."name" IN ('Ms. Cyril Doyle', 'Byron Hermiston Sr.')
             * ```
             */
            val query: Query = SimpleTable.selectAll()
                .where { SimpleTable.name inList names }

            // ResultRow 를 엔티티로 만든다.
            val entities: List<SimpleEntity> = SimpleEntity.wrapRows(query).toList()
            entities shouldHaveSize names.size
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `projection DTO`(testDB: TestDB) {
        withTables(testDB, SimpleTable) {
            persistSimpleEntities()

            val query = SimpleTable.selectAll()

            // ResultRow 를 DTO 로 만든다.
            val dtos = query.toSimpleDTOs()
            dtos.forEach { dto ->
                log.debug { "DTO=$dto" }
            }
            dtos shouldHaveSize ENTITY_COUNT
        }
    }
}
