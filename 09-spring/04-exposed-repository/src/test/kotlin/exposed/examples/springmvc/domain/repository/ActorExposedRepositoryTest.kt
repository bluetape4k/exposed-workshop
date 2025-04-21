package exposed.examples.springmvc.domain.repository

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.dtos.ActorDTO
import exposed.examples.springmvc.domain.dtos.toActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class ActorExposedRepositoryTest(
    @Autowired private val actorRepo: ActorExposedRepository,
): AbstractExposedRepositoryTest() {

    companion object: KLogging() {
        fun newActorDTO(): ActorDTO = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    @Transactional(readOnly = true)
    fun `find actor by id`() {
        val actorId = 1L

        val actor = actorRepo.findByIdOrNull(actorId)?.toActorDTO()
        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo actorId
    }

    @Test
    @Transactional(readOnly = true)
    fun `search actors by lastName`() {
        val params = mapOf("lastName" to "Depp")
        val actors = actorRepo.searchActors(params).map { it.toActorDTO() }

        actors.shouldNotBeEmpty()
        actors.forEach {
            log.debug { "actor: $it" }
        }
    }

    @Test
    fun `create new actor`() {
        val actor = newActorDTO()

        val currentCount = actorRepo.count()

        val savedActor = actorRepo.create(actor).toActorDTO()
        savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

        val newCount = actorRepo.count()
        newCount shouldBeEqualTo currentCount + 1
    }

    @Test
    fun `delete actor by id`() {
        val actor = newActorDTO()
        val savedActor = actorRepo.create(actor).toActorDTO()
        savedActor.shouldNotBeNull()

        val deletedCount = actorRepo.deleteById(savedActor.id!!)
        deletedCount shouldBeEqualTo 1
    }
}
