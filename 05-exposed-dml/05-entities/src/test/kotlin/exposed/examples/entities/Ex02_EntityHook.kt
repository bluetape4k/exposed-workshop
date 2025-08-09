package exposed.examples.entities

import exposed.shared.samples.City
import exposed.shared.samples.CityTable
import exposed.shared.samples.Country
import exposed.shared.samples.CountryTable
import exposed.shared.samples.User
import exposed.shared.samples.UserTable
import exposed.shared.samples.UserToCityTable
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.dao.registeredChanges
import org.jetbrains.exposed.v1.dao.toEntity
import org.jetbrains.exposed.v1.dao.withHook
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.emptySized
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Entity 에 대한 변경사항을 추적하는 `EntityHook` 을 테스트합니다.
 *
 * Hibernate의 `EntityListener` 와 유사한 기능을 제공합니다.
 */
class Ex02_EntityHook: JdbcExposedTestBase() {

    companion object: KLogging()

    private val allTables = arrayOf(UserTable, CityTable, UserToCityTable, CountryTable)

    /**
     * 변경사항을 추적하고, 변경사항과 함께 결과를 반환합니다.
     *
     * @param statement 변경사항을 추적할 람다
     * @return 변경사항, 변경사항 목록, 트랜잭션 ID
     */
    private fun <T> trackChanges(statement: JdbcTransaction.() -> T): Triple<T, Collection<EntityChange>, String> {
        // 기존에 존재했던 변경사항의 수
        val alreadyChanged = TransactionManager.current().registeredChanges().size
        return transaction {
            val result = statement()
            flushCache()
            Triple(result, registeredChanges().drop(alreadyChanged), id)
        }
    }

    /**
     * User Entity에 대한 변경사항을 추적하는 이벤트 리스너입니다.
     */
    open class UserEventListener: (EntityChange) -> Unit {
        fun subscribe() {
            EntityHook.subscribe(this)
        }

        fun unsubscribe() {
            EntityHook.unsubscribe(this)
        }

        override fun invoke(change: EntityChange) {
            when (change.changeType) {
                EntityChangeType.Created -> onCreate(change)
                EntityChangeType.Updated -> onUpdate(change)
                EntityChangeType.Removed -> onDelete(change)
            }
        }

        open fun onCreate(change: EntityChange) {
            log.info { "Entity created: ${change.toEntity<Int, User>()} " }
        }

        open fun onUpdate(change: EntityChange) {
            log.info { "Entity updated: ${change.toEntity<Int, User>()} " }
        }

        open fun onDelete(change: EntityChange) {
            log.info { "Entity deleted: ${change.toEntity<Int, User>()} " }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `subscribe EntityHook`(testDB: TestDB) {
        val entityChangeLogger = { change: EntityChange ->
            log.info { "Entity changes: [${change.changeType}], entity id=${change.entityId}, entity class=${change.toEntity<Int, User>()} " }
        }
        withTables(testDB, UserTable) {
            EntityHook.subscribe(entityChangeLogger)
            val user = User.new {
                name = "John"
                age = 30
            }
            user.flush()

            user.delete()

            EntityHook.unsubscribe(entityChangeLogger)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `custom event listener`(testDB: TestDB) {
        val eventListener = UserEventListener()
        withTables(testDB, UserTable) {
            eventListener.subscribe()
            val user = User.new {
                name = "John"
                age = 30
            }
            user.flush()

            user.delete()

            eventListener.unsubscribe()
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withHook example`(testDB: TestDB) {
        withTables(testDB, UserTable) {
            withHook(
                action = { change ->
                    log.info { "Entity changes: [${change.changeType}], entity id=${change.entityId} " }
                },
                body = {
                    val user = User.new {
                        name = "John"
                        age = 30
                    }
                    user.flush()

                    user.delete()
                }
            )
        }
    }

    /**
     * Entity 생성 시에 발생한 이벤트 ([EntityChangeType.Created])를 추적합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun created01(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val (_, events, txId) = trackChanges {
                val ru: Country = Country.new {
                    name = "RU"
                }
                val moscow = City.new {
                    name = "Moscow"
                    country = ru
                }
            }

            events.forEach {
                log.debug { "event=$it" }
            }
            events shouldHaveSize 2
            events.all { it.changeType == EntityChangeType.Created }.shouldBeTrue()
            events.mapNotNull { it.toEntity(City)?.name } shouldBeEqualTo listOf("Moscow")
            events.mapNotNull { it.toEntity(Country)?.name } shouldBeEqualTo listOf("RU")
            events.all { it.transactionId == txId }.shouldBeTrue()
        }
    }

    /**
     * Entity 삭제 시에 발생한 이벤트 ([EntityChangeType.Removed])를 추적합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun delete01(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val moscowId = transaction {
                val ru = Country.new { name = "RU" }
                val x = City.new {
                    name = "Moscow"
                    country = ru
                }
                flushCache()
                x.id
            }
            val (_, events, txId) = trackChanges {
                val moscow = City.findById(moscowId)!!
                moscow.delete()
            }

            events.forEach {
                log.debug { "event=$it" }
            }
            events shouldHaveSize 1
            events.single().changeType shouldBeEqualTo EntityChangeType.Removed
            events.single().entityId shouldBeEqualTo moscowId
            events.single().transactionId shouldBeEqualTo txId
        }
    }

    /**
     * Entity 수정 시에 발생한 이벤트 ([EntityChangeType.Updated])를 추적합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `modified simple 01`(tesgDB: TestDB) {
        withTables(tesgDB, *allTables) {
            val (_, events1, _) = trackChanges {
                val ru = Country.new {
                    name = "RU"
                }
                City.new {
                    name = "Moscow"
                    country = ru
                }
            }

            events1 shouldHaveSize 2
            events1.all { it.changeType == EntityChangeType.Created }.shouldBeTrue()

            val (_, events2, txId2) = trackChanges {
                val de = Country.new {
                    name = "DE"
                }
                val x = City.all().single()
                x.name = "Munich"
                x.country = de
            }

            // One may expect change for `RU` but we do not send it due to performance reasons
            events2.forEach {
                log.debug { "event=$it" }
            }
            events2 shouldHaveSize 2
            // create country (DE)
            events2.any { it.changeType == EntityChangeType.Created }.shouldBeTrue()
            // update city (Moscow -> Munich, RU -> DE)
            events2.any { it.changeType == EntityChangeType.Updated }.shouldBeTrue()

            events2.mapNotNull { it.toEntity(City)?.name } shouldBeEqualTo listOf("Munich")
            events2.mapNotNull { it.toEntity(Country)?.name } shouldBeEqualTo listOf("DE")
            events2.all { it.transactionId == txId2 }.shouldBeTrue()
        }
    }

    /**
     * Entity 수정 시에 발생한 이벤트 ([EntityChangeType.Updated])를 추적합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `modified inner table 01`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            transaction {
                val ru = Country.new { name = "RU" }
                val de = Country.new { name = "DE" }
                City.new { name = "Moscow"; country = ru }
                City.new { name = "Berlin"; country = de }
                User.new { name = "John"; age = 30 }

                flushCache()
            }

            val (_, events, txId) = trackChanges {
                /**
                 * ```sql
                 * SELECT city.id, city."name", city.country, usertocity.city_id, usertocity.user_id
                 *   FROM city INNER JOIN usertocity ON city.id = usertocity.city_id
                 *  WHERE usertocity.user_id = 1;
                 *
                 * DELETE
                 *   FROM usertocity
                 *  WHERE (usertocity.user_id = 1) AND (usertocity.city_id != 1);
                 *
                 * INSERT INTO usertocity (user_id, city_id) VALUES (1, 1);
                 * ```
                 */
                val moscow = City.find { CityTable.name eq "Moscow" }.single()
                val john = User.all().single()
                john.cities = SizedCollection(listOf(moscow))     // association 을 이렇게 지정할 수 있습니다.
            }

            events.forEach {
                log.debug { "event=$it" }
            }
            events shouldHaveSize 2
            events.all { it.changeType == EntityChangeType.Updated }.shouldBeTrue()
            events.mapNotNull { it.toEntity(City)?.name } shouldBeEqualTo listOf("Moscow")
            events.mapNotNull { it.toEntity(User)?.name } shouldBeEqualTo listOf("John")
            events.all { it.transactionId == txId }.shouldBeTrue()
        }
    }

    /**
     * Entity 수정 시에 발생한 이벤트 ([EntityChangeType.Updated])를 추적합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `modified inner table 02`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            transaction {
                val ru = Country.new { name = "RU" }
                val de = Country.new { name = "DE" }
                val moscow = City.new { name = "Moscow"; country = ru }
                val berlin = City.new { name = "Berlin"; country = de }
                val john = User.new { name = "John"; age = 30 }

                john.cities = SizedCollection(listOf(berlin))
                flushCache()
            }

            val (_, events, txId) = trackChanges {
                val moscow = City.find { CityTable.name eq "Moscow" }.single()
                val john = User.all().single()
                john.cities = SizedCollection(listOf(moscow))
            }

            events.forEach {
                log.debug { "event=$it" }
            }
            events shouldHaveSize 3
            // User[1], City[2], City[1] is Updated
            events.all { it.changeType == EntityChangeType.Updated }.shouldBeTrue()
            events.mapNotNull { it.toEntity(City)?.name } shouldBeEqualTo listOf("Berlin", "Moscow")
            events.mapNotNull { it.toEntity(User)?.name } shouldBeEqualTo listOf("John")
            events.all { it.transactionId == txId }.shouldBeTrue()
        }
    }

    /**
     * Entity 수정 시에 발생한 이벤트 ([EntityChangeType.Updated])를 추적합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `modified inner table 03`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            transaction {
                val ru = Country.new { name = "RU" }
                val de = Country.new { name = "DE" }
                val moscow = City.new { name = "Moscow"; country = ru }
                val berlin = City.new { name = "Berlin"; country = de }
                val john = User.new { name = "John"; age = 30 }

                john.cities = SizedCollection(listOf(moscow))
                flushCache()
            }

            val (_, events, txId) = trackChanges {
                val john = User.all().single()
                john.cities = emptySized()
            }

            events.forEach {
                log.debug { "event=$it" }
            }
            events shouldHaveSize 2
            events.all { it.changeType == EntityChangeType.Updated }.shouldBeTrue() // User[1], City[1] is Updated
            events.mapNotNull { it.toEntity(City)?.name } shouldBeEqualTo listOf("Moscow")
            events.mapNotNull { it.toEntity(User)?.name } shouldBeEqualTo listOf("John")
            events.all { it.transactionId == txId }.shouldBeTrue()
        }
    }

    /**
     * Entity의 `flush` 메서드를 호출하면 Event 가 발생합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `single entity flush should trigger events`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            val (user, events, _) = trackChanges {
                User
                    .new { name = "John"; age = 30 }
                    .apply { flush() }
            }

            events shouldHaveSize 1
            events.forEach {
                log.debug { "event1=$it" }
            }
            val createEvent = events.single()
            createEvent.changeType shouldBeEqualTo EntityChangeType.Created
            createEvent.entityId shouldBeEqualTo user.id

            val (_, events2, _) = trackChanges {
                user.name = "Carl"
                user.flush()
            }
            events2.forEach {
                log.debug { "event2=$it" }
            }
            user.name shouldBeEqualTo "Carl"
            events2 shouldHaveSize 1
            val updateEvent = events2.single()
            updateEvent.changeType shouldBeEqualTo EntityChangeType.Updated
            updateEvent.entityId shouldBeEqualTo user.id
        }
    }

    /**
     * EntityHook 은 Transaction 완료나 `flush` 메서드를 호출할 때 Event가 발생합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `calling flush notifies entity hook subscribers`(testDB: TestDB) {
        withTables(testDB, *allTables) {
            var hookCalls = 0
            val user = User.new {
                name = "1@test.local"
                age = 30
            }
            log.debug { "Flush user - insert" }
            user.flush()

            EntityHook.subscribe {
                hookCalls++
            }

            user.name = "2@test.local"
            hookCalls shouldBeEqualTo 0

            log.debug { "Flush user - update" }
            user.flush()
            hookCalls shouldBeEqualTo 1

            user.name = "3@test.local"
            hookCalls shouldBeEqualTo 1

            log.debug { "Flush user - update" }
            user.flush()
            hookCalls shouldBeEqualTo 2
        }
    }
}

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

            article1.refresh(true)

            articleListener.unsubscribe()
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
