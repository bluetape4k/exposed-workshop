package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.domain.model.ActorRecord
import exposed.workshop.springwebflux.domain.model.MovieSchema.ActorEntity
import exposed.workshop.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.workshop.springwebflux.domain.model.toActorRecord
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.eq
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

    suspend fun searchActor(params: Map<String, String?>): List<ActorRecord> {
        log.debug { "Search Actor by params. params: $params" }

        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name -> value?.let { parseLongParam(key, it) }?.let { query.andWhere { ActorTable.id eq it } }
                ActorTable::firstName.name -> value?.let { query.andWhere { ActorTable.firstName eq it } }
                ActorTable::lastName.name -> value?.let { query.andWhere { ActorTable.lastName eq it } }
                ActorTable::birthday.name -> value?.let { parseLocalDateParam(key, it) }?.let { query.andWhere { ActorTable.birthday eq it } }
            }
        }
        return query.map { it.toActorRecord() }
    }

    suspend fun create(actor: ActorRecord): ActorEntity {
        log.debug { "Create Actor. actor: $actor" }

        return ActorEntity.new {
            firstName = actor.firstName
            lastName = actor.lastName
            actor.birthday?.let { day ->
                birthday = runCatching { LocalDate.parse(day) }.getOrNull()
            }
        }
    }

    suspend fun deleteById(actorId: Long): Int {
        log.debug { "Delete Actor by id. id: $actorId" }
        return ActorTable.deleteWhere { ActorTable.id eq actorId }
    }

    private fun parseLongParam(key: String, value: String): Long? =
        value.toLongOrNull().also {
            if (it == null) log.warn("Invalid numeric `$key` parameter: '$value', ignoring filter.")
        }

    private fun parseLocalDateParam(key: String, value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }
            .onFailure {
                log.warn("Invalid `$key` parameter: '$value', ignoring filter.")
            }
            .getOrNull()
}
