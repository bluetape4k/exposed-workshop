package exposed.workshop.springmvc.domain.repository

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.model.ActorRecord
import exposed.workshop.springmvc.domain.model.MovieSchema.ActorTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional


/**
 * Spring MVC 환경에서 [ActorRepository]의 CRUD 기능을 검증하는 통합 테스트 클래스.
 *
 * Spring 트랜잭션(`@Transactional`)을 적용하여 읽기 전용 조회와 쓰기 연산을 분리 검증하며,
 * 잘못된 파라미터 입력 시 예외 없이 동작하는 방어 로직도 확인한다.
 */
@Transactional(readOnly = true)
class ActorRepositoryTest(
    @param:Autowired private val actorRepo: ActorRepository,
): AbstractSpringMvcTest() {

    companion object: KLogging() {
        fun newActorRecord(): ActorRecord = ActorRecord(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `find actor by id`() {
        val actorId = 1L

        val actor = actorRepo.findById(actorId)
        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo actorId
    }

    @Test
    fun `search actors by lastName`() {
        val params = mapOf("lastName" to "Depp")
        val actors = actorRepo.searchActors(params)

        actors.shouldNotBeEmpty()
        actors.forEach {
            log.debug { "actor: $it" }
        }
    }

    @Test
    fun `search actors ignores invalid birthday parameter`() {
        val params = mapOf("birthday" to "not-a-date")
        val actors = actorRepo.searchActors(params)

        actors.shouldNotBeEmpty()
    }

    @Test
    @Transactional
    fun `create new actor`() {
        val actor = newActorRecord()

        val currentCount = ActorTable.selectAll().count()

        val savedActor = actorRepo.create(actor)
        savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

        val newCount = ActorTable.selectAll().count()
        newCount shouldBeEqualTo currentCount + 1
    }

    @Test
    @Transactional
    fun `delete actor by id`() {
        val actor = newActorRecord()
        val savedActor = actorRepo.create(actor)
        savedActor.shouldNotBeNull()

        val deletedCount = actorRepo.deleteById(savedActor.id)
        deletedCount shouldBeEqualTo 1
    }
}
