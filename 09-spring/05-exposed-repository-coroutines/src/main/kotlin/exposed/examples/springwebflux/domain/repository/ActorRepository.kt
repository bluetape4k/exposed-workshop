package exposed.examples.springwebflux.domain.repository

import exposed.examples.springwebflux.domain.dtos.ActorDTO
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorEntity
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ActorRepository: ExposedRepository<ActorEntity, Long> {

    companion object: KLogging()

    override val table = ActorTable
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
