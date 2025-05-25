package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.domain.ActorDTO
import exposed.workshop.springwebflux.domain.MovieSchema.ActorEntity
import exposed.workshop.springwebflux.domain.MovieSchema.ActorTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ActorRepository {

    companion object: KLoggingChannel()

    suspend fun count(): Long =
        ActorTable.selectAll().count()

    suspend fun findById(id: Long): ActorEntity? {
        log.debug { "Find Actor by id. id: $id" }
        return ActorEntity.findById(id)
    }

    suspend fun findAll(): List<ActorEntity> {
        return ActorEntity.wrapRows(ActorTable.selectAll()).toList()
    }

    suspend fun searchActor(params: Map<String, String?>): List<ActorEntity> {
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

    suspend fun create(actor: ActorDTO): ActorEntity {
        log.debug { "Create Actor. actor: $actor" }

        return ActorEntity.new {
            firstName = actor.firstName
            lastName = actor.lastName
            actor.birthday?.let {
                birthday = runCatching { LocalDate.parse(actor.birthday) }.getOrNull()
            }
        }
    }

    suspend fun deleteById(actorId: Long): Int {
        log.debug { "Delete Actor by id. id: $actorId" }
        return ActorTable.deleteWhere { ActorTable.id eq actorId }
    }
}
