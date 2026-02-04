package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.model.ActorRecord
import exposed.workshop.springwebflux.domain.model.toActorRecord
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("DEPRECATION")
class ActorRepositoryTest(
    @param:Autowired private val actorRepository: ActorRepository,
): AbstractSpringWebfluxTest() {

    companion object: KLoggingChannel() {
        fun newActorRecord() = ActorRecord(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `find actor by id`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val actorId = 1L

            val actor = actorRepository.findById(actorId)?.toActorRecord()

            log.debug { "Actor: $actor" }
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
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
        newSuspendedTransaction(readOnly = true) {
            val params = mapOf("lastName" to "Depp")
            val actors = actorRepository.searchActor(params)

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
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
        newSuspendedTransaction(readOnly = true) {
            val params = mapOf("firstName" to "Angelina")
            val actors = actorRepository.searchActor(params)

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @Test
    fun `create new actor`() = runSuspendIO {
        newSuspendedTransaction {
            val prevCount = actorRepository.count()

            val actor = newActorRecord()

            val savedActor = actorRepository.create(actor).toActorRecord()
            savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

            val newActor = actorRepository.findById(savedActor.id)?.toActorRecord()
            newActor shouldBeEqualTo savedActor

            actorRepository.count() shouldBeEqualTo prevCount + 1L
        }
    }

    @Test
    fun `delete actor by id`() = runSuspendIO {
        newSuspendedTransaction {
            val actor = newActorRecord()
            val savedActor = actorRepository.create(actor).toActorRecord()
            savedActor.shouldNotBeNull()
            savedActor.id.shouldNotBeNull()

            val deletedCount = actorRepository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }
}
