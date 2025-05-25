package exposed.multitenant.webflux.domain.repository

import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.multitenant.webflux.domain.model.toActorDTO
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
class ActorExposedRepository: ExposedRepository<ActorDTO, Long> {

    companion object: KLogging()

    override val table = ActorTable
    override fun ResultRow.toEntity() = toActorDTO()

    /**
     * 주어진 조건에 맞는 [ActorEntity]를 조회합니다.
     */
    @Transactional(readOnly = true)
    fun searchActors(params: Map<String, String?>): List<ActorDTO> {
        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                LongIdTable::id.name -> value?.run { query.andWhere { ActorTable.id eq value.toLong() } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run { query.andWhere { ActorTable.birthday eq LocalDate.parse(value) } }
            }
        }

        return query.map { it.toEntity() }
    }

    @Transactional
    fun create(actor: ActorDTO): ActorDTO {
        log.debug { "Create new actor. actor: $actor" }

        val id = ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            it[birthday] = actor.birthday?.let { LocalDate.parse(it) }
        }
        return actor.copy(id = id.value)
    }

}
