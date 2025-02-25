package exposed.examples.entities

import exposed.shared.samples.City
import exposed.shared.samples.CityTable
import exposed.shared.samples.Country
import exposed.shared.samples.CountryTable
import exposed.shared.samples.User
import exposed.shared.samples.UserTable
import exposed.shared.samples.UserToCityTable
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.EntityChange
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.registeredChanges
import org.jetbrains.exposed.dao.toEntity
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.emptySized
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Entity 에 대한 변경사항을 추적하는 `EntityHook` 을 테스트합니다.
 *
 * Hibernate의 `EntityListener` 와 유사한 기능을 제공합니다.
 */
class Ex02_EntityHook: AbstractExposedTest() {

    companion object: KLogging()

    private val allTables = arrayOf(UserTable, CityTable, UserToCityTable, CountryTable)

    /**
     * 변경사항을 추적하고, 변경사항과 함께 결과를 반환합니다.
     *
     * @param statement 변경사항을 추적할 람다
     * @return 변경사항, 변경사항 목록, 트랜잭션 ID
     */
    private fun <T> trackChanges(statement: Transaction.() -> T): Triple<T, Collection<EntityChange>, String> {
        // 기존에 존재했던 변경사항의 수
        val alreadyChanged = TransactionManager.current().registeredChanges().size
        return transaction {
            val result = statement()
            flushCache()
            Triple(result, registeredChanges().drop(alreadyChanged), id)
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
