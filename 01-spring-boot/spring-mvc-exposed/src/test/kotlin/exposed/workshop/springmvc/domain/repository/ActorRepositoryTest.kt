package exposed.workshop.springmvc.domain.repository

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.ActorDTO
import exposed.workshop.springmvc.domain.MovieSchema.ActorTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional


class ActorRepositoryTest(
    @Autowired private val actorRepo: ActorRepository,
): AbstractSpringMvcTest() {

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

        val actor = actorRepo.findById(actorId)
        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo actorId
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
    @Transactional
    fun `create new actor`() {
        val actor = newActorDTO()

        val currentCount = ActorTable.selectAll().count()

        val savedActor = actorRepo.create(actor)
        savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

        val newCount = ActorTable.selectAll().count()
        newCount shouldBeEqualTo currentCount + 1
    }

    @Test
    @Transactional
    fun `delete actor by id`() {
        val actor = newActorDTO()
        val savedActor = actorRepo.create(actor)
        savedActor.shouldNotBeNull()

        val deletedCount = actorRepo.deleteById(savedActor.id!!)
        deletedCount shouldBeEqualTo 1
    }
}
