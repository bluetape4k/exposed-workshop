package exposed.examples.jpa.ex02_entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.javatime.date
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class Ex03_Task: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * TaskEntity 는 TaskTable 에 매핑된다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS TASKS (
     *      ID BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      STATUS VARCHAR(10) NOT NULL,
     *      CHANGED_ON DATE NOT NULL,
     *      CHANGED_BY VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object TaskTable: LongIdTable("tasks") {
        val status = enumerationByName("status", 10, TaskStatusType::class)
        val changedOn = date("changed_on")
        val changedBy = varchar("changed_by", 255)
    }

    class Task(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Task>(TaskTable)

        var status by TaskTable.status
        var changedOn by TaskTable.changedOn
        var changedBy by TaskTable.changedBy

        override fun hashCode(): Int = idHashCode()
        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun toString(): String = toStringBuilder()
            .add("status", status)
            .add("changedOn", changedOn)
            .add("changedBy", changedBy)
            .toString()
    }

    enum class TaskStatusType {
        TO_DO,
        DONE,
        FAILED
    }

    private val today = LocalDate.now()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create task entity with enum as string`(testDB: TestDB) {
        withTables(testDB, TaskTable) {
            val task = Task.new {
                status = TaskStatusType.TO_DO
                changedOn = today
                changedBy = "admin"
            }

            entityCache.clear()

            val loadedTask = Task.findById(task.id.value)!!
            loadedTask shouldBeEqualTo task

            loadedTask.status shouldBeEqualTo TaskStatusType.TO_DO
            loadedTask.changedOn shouldBeEqualTo today
            loadedTask.changedBy shouldBeEqualTo "admin"
        }
    }

}
