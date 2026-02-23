package exposed.examples.springmvc.domain.repository

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.model.ActorRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class ActorExposedRepositoryTest(
    @param:Autowired private val actorRepo: ActorExposedRepository,
): AbstractExposedRepositoryTest() {

    companion object: KLogging() {
        fun newActorRecord(): ActorRecord = ActorRecord(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    @Transactional(readOnly = true)
    fun `find actor by id`() {
        val actorId = 1L

        val actor = actorRepo.findByIdOrNull(actorId)
        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo actorId
    }

    @Test
    @Transactional(readOnly = true)
    fun `존재하지 않는 배우 ID를 조회하면 null을 반환한다`() {
        actorRepo.findByIdOrNull(Long.MIN_VALUE).shouldBeNull()
    }

    @Test
    @Transactional(readOnly = true)
    fun `search actors by lastName`() {
        val params = mapOf("lastName" to "Depp")
        val actors = actorRepo.searchActors(params)

        actors.shouldNotBeEmpty()
        actors.forEach {
            log.debug { "actor: $it" }
        }
    }

    @Test
    @Transactional(readOnly = true)
    fun `존재하지 않는 lastName으로 검색하면 빈 목록을 반환한다`() {
        val params = mapOf("lastName" to "NO_SUCH_LASTNAME")
        actorRepo.searchActors(params).shouldBeEmpty()
    }

    @Test
    fun `create new actor`() {
        val actor = newActorRecord()

        val currentCount = actorRepo.count()

        val savedActor = actorRepo.create(actor)
        savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

        val newCount = actorRepo.count()
        newCount shouldBeEqualTo currentCount + 1
    }

    @Test
    fun `delete actor by id`() {
        val actor = newActorRecord()
        val savedActor = actorRepo.create(actor)
        savedActor.shouldNotBeNull()

        val deletedCount = actorRepo.deleteById(savedActor.id)
        deletedCount shouldBeEqualTo 1
    }
}
