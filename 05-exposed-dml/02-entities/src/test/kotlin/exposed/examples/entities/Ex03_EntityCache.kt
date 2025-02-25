package exposed.examples.entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.idValue
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * DAO 사용 시 활용하는 [org.jetbrains.exposed.dao.EntityCache] 에 대한 테스트
 *
 * 참고: Hibernate 의 First Level Cache 인 Session과 유사한 역할을 수행함
 */
class Ex03_EntityCache: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS testcache (
     *      id SERIAL PRIMARY KEY,
     *      "value" INT NOT NULL
     * )
     * ```
     */
    object TestTable: IntIdTable("TestCache") {
        val value = integer("value")
    }

    class TestEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TestEntity>(TestTable)

        var value: Int by TestTable.value

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("id", idValue)
            .add("value", value)
            .toString()
    }

    /**
     * Entity Cache의 전역 설정 값을 변경하여 캐시 크기를 제한하는 테스트
     */
    @Test
    fun `global entity cache limit`() {
        val entitiesCount = 25
        val cacheSize = 10          // 10개의 엔티티만 캐시에 저장
        val db = TestDB.H2_PSQL.connect {
            maxEntitiesToStoreInCachePerEntity = cacheSize
        }

        transaction(db) {
            try {
                SchemaUtils.create(TestTable)

                repeat(entitiesCount) {
                    TestEntity.new {
                        value = Random.nextInt().absoluteValue
                    }
                }

                val allEntities = TestEntity.all().toList()
                allEntities shouldHaveSize entitiesCount

                // 엔티티 캐시로부터 특정 엔티티 수형을 조회하기
                val allCachedEntities = entityCache.findAll(TestEntity)
                allCachedEntities shouldHaveSize cacheSize
                allCachedEntities shouldContainSame allEntities.drop(entitiesCount - cacheSize)
            } finally {
                SchemaUtils.drop(TestTable)
            }
        }
    }

    /**
     * 전역 `EntityCace` 의 `maxEntitiesToStoreInCachePerEntity` 값을 0 으로 설정하면, 캐시를 사용하지 않습니다.
     */
    @Test
    fun `global entity cache limit zero`() {
        val entitiesCount = 25

        // 기본 캐시 사이즈를 사용하는 DB 연결
        val db = TestDB.H2_PSQL.connect()

        // limit 을 0 개로 제한한 DB 연결
        val dbNoCache = TestDB.H2_PSQL.connect {
            maxEntitiesToStoreInCachePerEntity = 0
        }

        /**
         * 기본 캐시 사이즈를 사용하는 DB 연결로 테스트
         */
        val entityIds = transaction(db) {
            SchemaUtils.create(TestTable)

            repeat(entitiesCount) {
                TestEntity.new {
                    value = Random.nextInt().absoluteValue
                }
            }
            val entityIds = TestTable.selectAll().map { it[TestTable.id] }
            val initialStatementCount = statementCount
            entityIds.forEach {
                TestEntity[it]
            }
            // All read from cache
            statementCount shouldBeEqualTo initialStatementCount
            log.debug { "Statement count (before): $statementCount" }

            entityCache.clear()
            // Load all into cache
            TestEntity.all().toList()

            entityIds.forEach {
                TestEntity[it]
            }
            log.debug { "Statement count (after): $statementCount" }
            statementCount shouldBeEqualTo initialStatementCount + 1
            entityIds
        }

        entityIds shouldHaveSize entitiesCount

        /**
         * 캐시를 사용하지 않는 DB 연결로 테스트
         */
        transaction(dbNoCache) {
            entityCache.clear()
            debug = true
            TestEntity.all().toList()
            statementCount shouldBeEqualTo 1

            val initialStatementCount = statementCount
            log.debug { "Statement count (before): $statementCount" }
            entityIds.forEach {
                TestEntity[it]
            }
            log.debug { "Statement count (after): $statementCount" }
            statementCount shouldBeEqualTo initialStatementCount + entitiesCount

            SchemaUtils.drop(TestTable)
        }
    }

    /**
     * 트랜잭션별 Entity Cache의 maxEntitiesToStore 값을 변경하여 캐시 크기를 제한하는 테스트
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `per transaction entity cache limit`(testDB: TestDB) {
        val entitiesCount = 25
        val cacheSize = 10

        withTables(testDB, TestTable) {
            // 트랜잭션별 캐시 제한 설정 (기본값: Int.MAX_VALUE)
            entityCache.maxEntitiesToStore = cacheSize

            repeat(entitiesCount) {
                TestEntity.new {
                    value = Random.nextInt().absoluteValue
                }
            }

            val allEntities = TestEntity.all().toList()
            allEntities shouldHaveSize entitiesCount

            val allCachedEntities = entityCache.findAll(TestEntity)
            allCachedEntities shouldHaveSize cacheSize
            allCachedEntities shouldContainSame allEntities.drop(entitiesCount - cacheSize)
        }
    }

    /**
     * 트랜잭션 중간에 Entity Cache의 maxEntitiesToStore 값을 변경해도, 거기에 맞게 캐시가 제한됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `change entity cache maxEntitiesToStore in middle of transaction`(testDB: TestDB) {
        withTables(testDB, TestTable) {
            repeat(20) {
                TestEntity.new {
                    value = Random.nextInt().absoluteValue
                }
            }
            entityCache.clear()

            TestEntity.all().limit(15).toList()
            entityCache.findAll(TestEntity) shouldHaveSize 15

            entityCache.maxEntitiesToStore = 18
            TestEntity.all().toList()
            entityCache.findAll(TestEntity) shouldHaveSize 18

            // Resize current cache
            entityCache.maxEntitiesToStore = 10
            entityCache.findAll(TestEntity) shouldHaveSize 10

            entityCache.maxEntitiesToStore = 18
            TestEntity.all().toList()
            entityCache.findAll(TestEntity) shouldHaveSize 18

            // 캐시를 사용하지 않는다.
            entityCache.maxEntitiesToStore = 0
            entityCache.findAll(TestEntity) shouldHaveSize 0
        }
    }

    /**
     * 명시적 commit 이후에도 EntityCache는 삭제되지 않습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `EntityCache should not be cleaned on explicit commit`(testDB: TestDB) {
        withTables(testDB, TestTable) {
            val entity = TestEntity.new {
                value = Random.nextInt().absoluteValue
            }
            TestEntity.testCache(entity.id) shouldBeEqualTo entity

            // 명시적 commint 후에도 캐시는 유지되어야 함
            commit()
            TestEntity.testCache(entity.id) shouldBeEqualTo entity

            // 명시적으로 entityCache를 삭제해야 함
            entityCache.clear()
            TestEntity.testCache(entity.id).shouldBeNull()
        }
    }
}
