package exposed.shared.repository

import exposed.shared.repository.model.ActorRecord
import exposed.shared.repository.model.MovieSchema.ActorTable
import exposed.shared.repository.model.MovieSchema.withSuspendedMovieAndActors
import exposed.shared.repository.repository.ActorRepository
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ActorRepositoryCoroutineTest: AbstractExposedTest() {

    companion object: KLoggingChannel() {
        fun newActorRecord(): ActorRecord =
            ActorRecord(
                firstName = faker.name().firstName(),
                lastName = faker.name().lastName(),
                birthday = faker.timeAndDate().birthday(20, 80).toString(),
            )
    }

    private val repository = ActorRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find actor by id`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
            val actorId = 1L
            val actor = repository.findById(actorId)
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search actors by lastName`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
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
    fun `create new actor`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
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
    fun `delete actor by id`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            val deletedCount = repository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count of actors`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
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
    fun `count with predicate`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
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
    fun `isEmpty with Actor`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
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
    fun `exists with Actor`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
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
    fun `findAll with limit and offset`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
            repository.findAll(limit = 2) shouldHaveSize 2
            repository.findAll { ActorTable.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3) { ActorTable.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3, offset = 1) { ActorTable.lastName eq "Depp" } shouldHaveSize 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete entity`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // 저장한 배우 레코드를 삭제합니다.
            repository.deleteById(savedActor.id) shouldBeEqualTo 1
            // 이미 삭제된 레코드는 다시 삭제되지 않습니다.
            repository.deleteById(savedActor.id) shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete entity by id`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // 저장한 배우 레코드를 삭제합니다.
            repository.deleteById(savedActor.id) shouldBeEqualTo 1

            // 이미 삭제된 레코드는 다시 삭제되지 않습니다.
            repository.deleteById(savedActor.id) shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with limit`(testDB: TestDB) = runSuspendIO {
        withSuspendedMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAll { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // 조건으로 삭제한 1건을 제외한 배우 레코드를 모두 삭제합니다.
            repository.deleteAll() shouldBeEqualTo count.toInt() - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with ignore`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withSuspendedMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAllIgnore { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // 조건으로 삭제한 1건을 제외한 배우 레코드를 모두 삭제합니다.
            repository.deleteAllIgnore() shouldBeEqualTo count.toInt() - 1
        }
    }
}
