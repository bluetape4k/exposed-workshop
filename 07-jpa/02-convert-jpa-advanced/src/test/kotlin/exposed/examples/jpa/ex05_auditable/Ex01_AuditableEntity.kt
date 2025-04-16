package exposed.examples.jpa.ex05_auditable

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex01_AuditableEntity: AbstractExposedTest() {

    object TaskTable: AuditableIntIdTable("tasks") {
        val title = varchar("title", 200)
        val description = text("description")
        val status = varchar("status", 20).default("NEW")
    }

    class TaskEntity(id: EntityID<Int>): AuditableIntEntity(id) {
        companion object: EntityClass<Int, TaskEntity>(TaskTable)

        var title by TaskTable.title
        var description by TaskTable.description
        var status by TaskTable.status

        override var createdBy by TaskTable.createdBy
        override var createdAt by TaskTable.createdAt
        override var updatedBy by TaskTable.updatedBy
        override var updatedAt by TaskTable.updatedAt
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditable entity 생성 시 createAt, updateAt 이 설정된다`(testDB: TestDB) {
        withTables(testDB, TaskTable) {
            UserContext.withUser("test") {
                val now = java.time.Instant.now()

                val task = TaskEntity.new {
                    title = "Test Task"
                    description = "This is a test task."
                    status = "NEW"
                }
                entityCache.clear()

                val loaded = TaskEntity.findById(task.id)!!
                loaded.createdAt.shouldNotBeNull() shouldBeGreaterOrEqualTo now
                loaded.createdBy.shouldNotBeNull() shouldBeEqualTo UserContext.getCurrentUser() // "test"
                loaded.updatedAt.shouldBeNull()
                loaded.updatedBy.shouldBeNull()

                loaded.title = "Test Task - Updated"
                entityCache.clear()

                val updated = TaskEntity.findById(task.id)!!
                updated.createdAt.shouldNotBeNull() shouldBeGreaterOrEqualTo now
                updated.createdBy.shouldNotBeNull() shouldBeEqualTo UserContext.getCurrentUser()
                updated.updatedAt.shouldNotBeNull() shouldBeGreaterOrEqualTo now
                updated.updatedBy.shouldNotBeNull() shouldBeEqualTo UserContext.getCurrentUser()
            }
        }
    }
}
