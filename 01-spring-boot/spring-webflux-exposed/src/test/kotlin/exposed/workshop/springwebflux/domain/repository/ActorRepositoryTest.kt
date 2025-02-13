package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.ActorDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ActorRepositoryTest(
    @Autowired private val actorRepository: ActorRepository,
): AbstractSpringWebfluxTest() {

    companion object: KLogging() {
        fun newActorDTO() = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `find actor by id`() = runSuspendIO {
        val actorId = 1L

        val actor = actorRepository.findById(actorId)

        log.debug { "Actor: $actor" }
        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo actorId
    }

    /**
     * ```sql
     * SELECT ACTORS.ID, ACTORS.FIRST_NAME, ACTORS.LAST_NAME, ACTORS.BIRTHDAY
     *   FROM ACTORS
     *  WHERE ACTORS.LAST_NAME = 'Depp'
     * ```
     */
    @Test
    fun `search actors by lastName`() = runSuspendIO {
        val params = mapOf("lastName" to "Depp")
        val actors = actorRepository.searchActor(params).toList()

        actors.shouldNotBeEmpty()
        actors.forEach {
            log.debug { "actor: $it" }
        }
    }

    /**
     * ```sql
     * SELECT ACTORS.ID, ACTORS.FIRST_NAME, ACTORS.LAST_NAME, ACTORS.BIRTHDAY
     *   FROM ACTORS
     *  WHERE ACTORS.FIRST_NAME = 'Angelina'
     * ```
     */
    @Test
    fun `search actors by firstName`() = runSuspendIO {
        val params = mapOf("firstName" to "Angelina")
        val actors = actorRepository.searchActor(params).toList()

        actors.shouldNotBeEmpty()
        actors.forEach {
            log.debug { "actor: $it" }
        }
    }

    @Test
    fun `create new actor`() = runSuspendIO {
        val prevCount = actorRepository.count()

        val actor = newActorDTO()

        val savedActor = actorRepository.create(actor)
        savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

        val newActor = actorRepository.findById(savedActor.id!!)
        newActor shouldBeEqualTo savedActor

        actorRepository.count() shouldBeEqualTo prevCount + 1L
    }

    @Test
    fun `delete actor by id`() = runSuspendIO {
        val actor = newActorDTO()
        val savedActor = actorRepository.create(actor)
        savedActor.shouldNotBeNull()
        savedActor.id.shouldNotBeNull()

        val deletedCount = actorRepository.deleteById(savedActor.id!!)
        deletedCount shouldBeEqualTo 1
    }
}
