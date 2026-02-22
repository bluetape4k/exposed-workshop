package exposed.examples.jpa.ex05_relations.ex02_one_to_many

import exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema.BatchSchema.Batch
import exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema.BatchSchema.BatchItem
import exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema.BatchSchema.BatchItemTable
import exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema.BatchSchema.batchTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * one-to-many bidirectional 관계를 Exposed로 구현한 예제
 */
class Ex01_OneToMany_Bidirectional_Batch: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * one-to-many bidirectional 관계의 엔티티를 저장하고 조회하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many with bidirectional relationship`(testDB: TestDB) {
        withTables(testDB, *batchTables) {
            val batch1 = createSamples()
            val batchItems = batch1.items.toList()

            entityCache.clear()

            val loaded = Batch.findById(batch1.id)!!
            loaded shouldBeEqualTo batch1
            // items 조회시 쿼리를 실행합니다.
            loaded.items
                .orderBy(BatchItemTable.id to SortOrder.ASC)
                .toList() shouldBeEqualTo batchItems

            // eager loading
            val loaded2 = Batch.all().with(Batch::items).single()
            loaded2.items.toList() shouldBeEqualTo batchItems

            // eager loading
            val loaded3 = Batch.findById(batch1.id)?.load(Batch::items)
            loaded3?.items?.toList() shouldBeEqualTo batchItems
        }
    }

    /**
     * one-to-many bidirectional 관계의 엔티티를 삭제하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many with bidirectional - delete`(testDB: TestDB) {
        withTables(testDB, *batchTables) {
            val batch1 = createSamples()
            val batchItems = batch1.items.toList()

            entityCache.clear()

            val loaded = Batch.findById(batch1.id)!!
            loaded shouldBeEqualTo batch1
            loaded.items.toList() shouldBeEqualTo batchItems

            // DELETE FROM batch_item WHERE batch_item.id = 1
            batchItems.first().delete()
            entityCache.clear()

            val loaded2 = Batch.findById(batch1.id)!!
            loaded2.items.count() shouldBeEqualTo 2L
            loaded2.items.toList() shouldBeEqualTo batchItems.drop(1)

            // batch 를 삭제하면, 관련된 batchItem 도 삭제된다. (onDelete = CASCADE)
            loaded2.delete()
            loaded2.items.count() shouldBeEqualTo 0L
        }
    }

    private fun JdbcTransaction.createSamples(): Batch {
        val batch1 = Batch.new { name = "B-123" }

        BatchItem.new { name = "Item 1"; batch = batch1 }
        BatchItem.new { name = "Item 2"; batch = batch1 }
        BatchItem.new { name = "Item 3"; batch = batch1 }

        commit()
        return batch1
    }
}
