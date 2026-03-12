package exposed.shared.repository

import exposed.shared.repository.model.ActorRecord
import exposed.shared.repository.model.MovieSchema.ActorTable
import exposed.shared.repository.model.MovieSchema.withMovieAndActors
import exposed.shared.repository.repository.ActorRepository
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [ActorRepository]의 CRUD 및 조회 기능을 다양한 DB Dialect에서 검증하는 테스트 클래스.
 *
 * [withMovieAndActors] 헬퍼를 사용해 테스트용 영화·배우 데이터를 사전에 구성하며,
 * 저장·삭제·카운트·존재 여부 확인 등 Repository 공통 연산의 정확성을 확인한다.
 */
class ActorRepositoryTest: AbstractExposedTest() {

    companion object: KLoggingChannel() {
        fun newActorRecord(): ActorRecord = ActorRecord(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository = ActorRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find actor by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actorId = 1L
            val actor = repository.findById(actorId)
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search actors by lastName`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val params = mapOf("lastName" to "Depp")
            val actors = repository.searchActors(params)

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create new actor`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()

            val currentCount = repository.count()

            val savedActor = repository.save(actor)
            savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete actor by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            val deletedCount = repository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count of actors`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val count = repository.count()
            log.debug { "count: $count" }
            count shouldBeGreaterThan 0L

            repository.save(newActorRecord())

            val newCount = repository.count()
            newCount shouldBeEqualTo count + 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count with predicate`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val count = repository.countBy { ActorTable.lastName eq "Depp" }
            log.debug { "count: $count" }
            count shouldBeEqualTo 1L

            val op = ActorTable.lastName eq "Depp"
            val count2 = repository.countBy(op)
            log.debug { "count2: $count2" }
            count2 shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isEmpty with Actor`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val isEmpty = repository.isEmpty()
            log.debug { "isEmpty: $isEmpty" }
            isEmpty.shouldBeFalse()

            repository.deleteAll { ActorTable.id greaterEq 0L }

            val isEmpty2 = repository.isEmpty()
            log.debug { "isEmpty2: $isEmpty2" }
            isEmpty2.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists with Actor`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val exists = repository.exists(ActorTable.selectAll())
            log.debug { "exists: $exists" }
            exists.shouldBeTrue()

            val exists2 = repository.exists(ActorTable.selectAll().limit(1))
            log.debug { "exists2: $exists2" }
            exists2.shouldBeTrue()

            val op = ActorTable.firstName eq "Not-Exists"
            val query = ActorTable.select(ActorTable.id).where(op).limit(1)
            val exists3 = repository.exists(query)
            log.debug { "exists3: $exists3" }
            exists3.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll with limit and offset`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            repository.findAll(limit = 2) shouldHaveSize 2
            repository.findAll { ActorTable.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3) { ActorTable.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3, offset = 1) { ActorTable.lastName eq "Depp" } shouldHaveSize 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete entity`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // Delete savedActor
            repository.deleteById(savedActor.id) shouldBeEqualTo 1
            // Already deleted
            repository.deleteById(savedActor.id) shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete entity by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // Delete savedActor
            repository.deleteById(savedActor.id) shouldBeEqualTo 1

            // Already deleted
            repository.deleteById(savedActor.id) shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with limit`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAll { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAll() shouldBeEqualTo count.toInt() - 1
        }
    }

    /**
     * `deleteAllIgnore`를 사용한 조건부 일괄 삭제를 검증하는 테스트.
     *
     * `DELETE IGNORE` 구문은 MySQL/MariaDB 에서만 지원되므로 해당 Dialect에서만 실행된다.
     * 조건 일치 레코드 삭제 후 나머지 전체 삭제까지 순차적으로 수행하여 결과를 검증한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with ignore`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAllIgnore { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAllIgnore() shouldBeEqualTo count.toInt() - 1
        }
    }
}
