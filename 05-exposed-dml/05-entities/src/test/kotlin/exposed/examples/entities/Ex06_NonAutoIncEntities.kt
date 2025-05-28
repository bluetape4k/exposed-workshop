package exposed.examples.entities

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.atomic.AtomicInteger

/**
 * 자동 증가가 아닌 Identifier를 가진 Entity 테스트
 */
class Ex06_NonAutoIncEntities: JdbcExposedTestBase() {

    companion object: KLogging()

    abstract class BaseNonAutoIncTable(name: String = ""): IdTable<Int>(name) {
        override val id: Column<EntityID<Int>> = integer("id").entityId()
        val b1: Column<Boolean> = bool("b1")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS notautointid (
     *      id INT NOT NULL,
     *      b1 BOOLEAN NOT NULL,
     *      i1 INT NOT NULL
     * )
     * ```
     */
    object NotAutoIntIdTable: BaseNonAutoIncTable() {
        val defaultedInt: Column<Int> = integer("i1")
    }

    class NotAutoIntEntity(id: EntityID<Int>): Entity<Int>(id) {
        var b1: Boolean by NotAutoIntIdTable.b1
        var defaultedInNew: Int by NotAutoIntIdTable.defaultedInt

        companion object: EntityClass<Int, NotAutoIntEntity>(NotAutoIntIdTable) {
            val lastId = AtomicInteger(0)
            internal const val defaultInt = 42

            fun new(b: Boolean) = new(lastId.incrementAndGet()) { b1 = b }

            // `new` 메소드를 재정의하여 id를 Client에서 제공합니다.
            override fun new(id: Int?, init: NotAutoIntEntity.() -> Unit): NotAutoIntEntity {
                // Client의 `lastId`를 사용하여 id를 생성합니다. (멀티 JVM, HA 구성 시 문제가 될 수 있습니다)
                return super.new(id ?: lastId.incrementAndGet()) {
                    defaultedInNew = defaultInt
                    init()
                }
            }
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("b1", b1)
            .add("defaultedInNew", defaultedInNew)
            .toString()

    }

    /**
     * 재정의한 `new` 메소드를 이용하여 엔티티를 생성합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `defaults with override new`(testDB: TestDB) {
        withTables(testDB, NotAutoIntIdTable) {

            // INSERT INTO notautointid (id, i1, b1) VALUES (27, 42, TRUE);
            val entity1 = NotAutoIntEntity.new(true)
            entity1.b1.shouldBeTrue()
            entity1.defaultedInNew shouldBeEqualTo NotAutoIntEntity.defaultInt

            // INSERT INTO notautointid (id, i1, b1) VALUES (28, 1, FALSE);
            val entity2 = NotAutoIntEntity.new {
                b1 = false
                defaultedInNew = 1
            }
            entity2.b1.shouldBeFalse()
            entity2.defaultedInNew shouldBeEqualTo 1
        }
    }

    /**
     * 자동 증가 Identifier 가 아닌, 클라이언트에서 지정한 Identifier를 사용하여 엔티티를 생성합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `not auto inc table`(testDB: TestDB) {
        withTables(testDB, NotAutoIntIdTable) {

            // INSERT INTO notautointid (id, i1, b1) VALUES (7, 42, TRUE)
            val e1 = NotAutoIntEntity.new(true)

            // INSERT INTO notautointid (id, i1, b1) VALUES (8, 42, FALSE)
            val e2 = NotAutoIntEntity.new(false)

            entityCache.clear()

            val all = NotAutoIntEntity.all()
            all.map { it.id } shouldBeEqualTo listOf(e1.id, e2.id)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS requests (
     *      deleted BOOLEAN DEFAULT FALSE NOT NULL,
     *      request_id VARCHAR(255) PRIMARY KEY
     * )
     * ```
     */
    object RequestsTable: IdTable<String>() {
        val requestId = varchar("request_id", 255)
        val deleted = bool("deleted").default(false)

        override val primaryKey = PrimaryKey(requestId)
        override val id: Column<EntityID<String>> = requestId.entityId()
    }

    class Request(id: EntityID<String>): Entity<String>(id) {
        companion object: EntityClass<String, Request>(RequestsTable)

        var requestId by RequestsTable.requestId
        var deleted by RequestsTable.deleted

        /**
         * Soft delete using `deleted` column
         */
        override fun delete() {
            RequestsTable.update({ RequestsTable.id eq id }) {
                it[deleted] = true
            }
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("requestId", requestId)
            .add("deleted", deleted)
            .toString()
    }

    /**
     * 엔티티 조회 시 `id`를 이용하여 엔티티를 조회합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `access entity id from override entity method`(testDB: TestDB) {
        withTables(testDB, RequestsTable) {
            val request = Request.new {
                requestId = "requestId"
                deleted = false
            }

            /**
             * Soft delete the entity
             *
             * ```sql
             * UPDATE requests
             *    SET deleted=TRUE
             *  WHERE requests.request_id = 'requestId'
             * ```
             */
            request.delete()

            /**
             * ```sql
             * SELECT requests.deleted, requests.request_id
             *   FROM requests
             *  WHERE requests.request_id = 'requestId'
             * ```
             */
            val updated: Request = Request["requestId"]
            updated.deleted.shouldBeTrue()
        }
    }
}
