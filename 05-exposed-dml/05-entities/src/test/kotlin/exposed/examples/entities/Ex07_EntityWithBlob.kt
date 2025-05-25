package exposed.examples.entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.sql.statements.api.toUtf8String
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [ExposedBlob] 필드를 가지는 엔티티를 다루는 예제입니다.
 */
class Ex07_EntityWithBlob: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS blobtable (
     *      id BIGSERIAL PRIMARY KEY,
     *      "content" bytea NULL
     * )
     * ```
     */
    object BlobTable: LongIdTable("BlobTable") {
        val blob: Column<ExposedBlob?> = blob("content").nullable()
    }

    class BlobEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<BlobEntity>(BlobTable) {
            // Custom 엔티티 생성 함수
            fun new(bytes: ByteArray, id: Long? = null, init: BlobEntity.() -> Unit = {}): BlobEntity {
                return new(id, init).apply {
                    content = ExposedBlob(bytes)
                }
            }
        }

        var content: ExposedBlob? by BlobTable.blob

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("content", content?.bytes?.toUtf8String())
            .toString()
    }

    /**
     * Blob 필드를 다루는 테스트
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `handle blob field`(testDB: TestDB) {
        withTables(testDB, BlobTable) {

            val text1 = Fakers.randomString(1024, 4096)
            val bytes1 = text1.toUtf8Bytes()

            // INSERT INTO blobtable ("content") VALUES (E'\\x')
            val blobEntity = BlobEntity.new(bytes1)
            entityCache.clear()

            // SELECT blobtable.id, blobtable."content" FROM blobtable WHERE blobtable.id = 1
            var y2 = BlobEntity.reload(blobEntity)!!
            y2.content!!.bytes.toUtf8String() shouldBeEqualTo text1

            // UPDATE blobtable SET "content"=NULL WHERE id = 1
            y2.content = null
            entityCache.clear()

            // SELECT blobtable.id, blobtable."content" FROM blobtable WHERE blobtable.id = 1
            y2 = BlobEntity.reload(blobEntity)!!
            y2.content.shouldBeNull()

            val text2 = Fakers.randomString(1024, 4096)
            val bytes2 = text2.toUtf8Bytes()

            // UPDATE blobtable SET "content"=E'\\x' WHERE id = 1
            y2.content = ExposedBlob(bytes2)
            entityCache.clear()

            // SELECT blobtable.id, blobtable."content" FROM blobtable WHERE blobtable.id = 1
            y2 = BlobEntity.reload(blobEntity)!!
            y2.content!!.toUtf8String() shouldBeEqualTo text2
        }
    }
}
