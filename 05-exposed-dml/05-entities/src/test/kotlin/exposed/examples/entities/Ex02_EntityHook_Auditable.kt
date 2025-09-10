package exposed.examples.entities

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.toEntity
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Ex02_EntityHook_Auditable: JdbcExposedTestBase() {

    interface AuditableEntity {
        var createdAt: Instant?
        var updatedAt: Instant?
    }

    open class AuditableLongTable(name: String = ""): LongIdTable(name) {
        var createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp).nullable()
        var updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp).nullable()
    }

    open class AuditableEntityListener: (EntityChange) -> Unit {

        companion object: KLogging()

        override fun invoke(change: EntityChange) {
            if (isAuditableEntity(change)) {
                val entity = change.toEntity<Any, Entity<Any>>() as? AuditableEntity
                if (entity != null) {
                    when (change.changeType) {
                        EntityChangeType.Created -> onCreated(change, entity)
                        EntityChangeType.Updated -> onUpdated(change, entity)
                        else -> {} // Nothing to do
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        protected fun isAuditableEntity(change: EntityChange): Boolean =
            (change.entityClass.javaClass.enclosingClass as? Class<out AuditableEntity>) != null

        fun onCreated(change: EntityChange, entity: AuditableEntity) {
            log.debug { "Entity created: ${change.entityId}, entityClass=${change.entityClass.javaClass.enclosingClass.enclosingClass} " }
            // 굳이 할 필요 없다 (DB에서 기본값으로 지정했기 때문)
            // val now = Instant.now()
            // entity.updatedAt = now
        }

        fun onUpdated(change: EntityChange, entity: AuditableEntity) {
            log.debug { "Entity updated: ${change.entityId}, entityClass=${change.entityClass.javaClass.enclosingClass.enclosingClass} " }
            val now = Instant.now()
            // 재귀호출을 방지하기 위해서 10ms 정도의 여유를 둡니다.
            // 물론 현 Transaction에서 ThreadLocal 에 상태를 관리해서 처리할 수도 있으나
            // 문제는 Coroutines 방식이나 Virtual Thread 사용 시에는 ThreadLocal 사용도 위험할 수 있습니다.
            if (entity.updatedAt?.plusMillis(10)?.isBefore(now) != false) {
                entity.updatedAt = now
            }
        }

        fun subscribe() {
            EntityHook.subscribe(this)
        }

        fun unsubscribe() {
            EntityHook.unsubscribe(this)
        }
    }

    object Articles: AuditableLongTable("articles") {
        val title = varchar("title", 255)
        val content = varchar("content", 255)
    }

    class Article(id: EntityID<Long>): LongEntity(id), AuditableEntity {
        companion object: LongEntityClass<Article>(Articles)

        override var createdAt: Instant? by Articles.createdAt
        override var updatedAt: Instant? by Articles.updatedAt

        var title by Articles.title
        var content by Articles.content

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("id", idValue)
            .add("title", title)
            .add("content", content)
            .add("createdAt", createdAt)
            .add("updatedAt", updatedAt)
            .toString()
    }

    /**
     * NOTE: 이 작업은 다른 속성 UPDATE 후 updatedAt 속성을 또 UPDATE 하므로 좋은 방법이 아닙니다.
     *
     * NOTE: 아래의 property deletegate 를 이용하는 방법도 좋지 않습니다.
     *
     * NOTE: 엔티티의 flush 함수를 재정의해서 사용하는 것이 제일 좋습니다. (참고: AuditableEntity)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditable entity with entity hook`(testDB: TestDB) {
        val articleListener = AuditableEntityListener()

        withTables(testDB, Articles) {
            articleListener.subscribe()

            val article1 = Article.new {
                title = "Article 1"
                content = "Content of article 1"
            }
            article1.refresh(true)

            Thread.sleep(100)

            article1.title = "Updated Article 1"
            runCatching { articleListener.unsubscribe() }
        }
    }

    /**
     * property delegate 를 이용하여 AuditableEntity 를 구현합니다.
     */
    abstract class AbstractAuditableLongEntity(id: EntityID<Long>): LongEntity(id), AuditableEntity {
        fun <T> auditing(column: Column<T>): ReadWriteProperty<AbstractAuditableLongEntity, T> {
            return object: ReadWriteProperty<AbstractAuditableLongEntity, T> {
                override fun getValue(thisRef: AbstractAuditableLongEntity, property: KProperty<*>): T {
                    return column.getValue(thisRef, property)
                }

                override fun setValue(thisRef: AbstractAuditableLongEntity, property: KProperty<*>, value: T) {
                    column.setValue(thisRef, property, value)
                    thisRef.updatedAt = Instant.now()
                }
            }
        }
    }

    class AuditableArticle(id: EntityID<Long>): AbstractAuditableLongEntity(id) {
        companion object: LongEntityClass<AuditableArticle>(Articles)

        // 속셩 변경 시, updatedAt 속성을 자동으로 변경합니다.
        var title: String by auditing(Articles.title)

        // 속셩 변경 시, updatedAt 속성을 자동으로 변경합니다.
        var content: String by auditing(Articles.content)

        override var createdAt: Instant? by Articles.createdAt
        override var updatedAt: Instant? by Articles.updatedAt
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auditable entity with delegate`(testDB: TestDB) {
        withTables(testDB, Articles) {
            val article1 = AuditableArticle.new {
                title = "Article 1"
                content = "Content of article 1"
            }
            article1.flush()

            article1.title = "Updated Article 1"
            article1.flush()
        }
    }
}
