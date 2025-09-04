package exposed.examples.springwebflux.domain.repository

import exposed.examples.springwebflux.domain.dtos.ActorDTO
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.toActorDTO
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ActorExposedRepository: ExposedRepository<ActorDTO, Long> {

    companion object: KLoggingChannel()

    override val table = ActorTable
    override fun ResultRow.toEntity() = toActorDTO()

    fun searchActor(params: Map<String, String?>): List<ActorDTO> {
        log.debug { "Search Actor by params. params: $params" }

        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name -> value?.run { query.andWhere { ActorTable.id eq value.toLong() } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run {
                    query.andWhere { ActorTable.birthday eq LocalDate.parse(value) }
                }
            }
        }
        return query.map { it.toEntity() }
    }

    fun create(actor: ActorDTO): ActorDTO {
        log.debug { "Create Actor. actor: $actor" }

        val id = ActorTable.insertAndGetId {
            it[ActorTable.firstName] = actor.firstName
            it[ActorTable.lastName] = actor.lastName
            actor.birthday?.let { birthday ->
                it[ActorTable.birthday] = runCatching { LocalDate.parse(birthday) }.getOrNull()
            }
        }

        return actor.copy(id = id.value)

    }
}
