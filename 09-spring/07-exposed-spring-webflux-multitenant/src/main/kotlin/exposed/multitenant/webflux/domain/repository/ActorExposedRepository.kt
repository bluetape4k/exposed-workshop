package exposed.multitenant.webflux.domain.repository

import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorEntity
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.shared.repository.AbstractExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ActorExposedRepository: AbstractExposedRepository<ActorEntity, Long>(ActorTable) {

    companion object: KLogging()

    override fun ResultRow.toEntity(): ActorEntity = ActorEntity.wrapRow(this)

    fun searchActor(params: Map<String, String?>): List<ActorEntity> {
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
        return ActorEntity.wrapRows(query).toList()
    }

    fun create(actor: ActorDTO): ActorEntity {
        log.debug { "Create Actor. actor: $actor" }

        val actorEntity = ActorEntity.new {
            firstName = actor.firstName
            lastName = actor.lastName
            actor.birthday?.let {
                birthday = runCatching { LocalDate.parse(actor.birthday) }.getOrNull()
            }
        }

        return actorEntity
    }
}
