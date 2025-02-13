package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.domain.ActorDTO
import exposed.workshop.springwebflux.domain.MovieSchema.ActorEntity
import exposed.workshop.springwebflux.domain.MovieSchema.ActorTable
import exposed.workshop.springwebflux.domain.toActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ActorRepository {

    companion object: KLogging()

    suspend fun count(): Long = newSuspendedTransaction(readOnly = true) {
        ActorTable.selectAll().count()
    }

    suspend fun findById(id: Long): ActorDTO? = newSuspendedTransaction(readOnly = true) {
        log.debug { "Find Actor by id. id: $id" }
        ActorEntity.findById(id)?.toActorDTO()
    }

    suspend fun searchActor(params: Map<String, String?>): Flow<ActorDTO> = newSuspendedTransaction(readOnly = true) {
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
        query.map { it.toActorDTO() }.asFlow()
    }

    suspend fun create(actor: ActorDTO): ActorDTO = newSuspendedTransaction {
        log.debug { "Create Actor. actor: $actor" }

        val actorEntity = ActorEntity.new {
            firstName = actor.firstName
            lastName = actor.lastName
            actor.birthday?.let {
                birthday = runCatching { LocalDate.parse(actor.birthday) }.getOrNull()
            }
        }

        actorEntity.toActorDTO()
    }

    suspend fun deleteById(actorId: Long): Int = newSuspendedTransaction {
        log.debug { "Delete Actor by id. id: $actorId" }
        ActorTable.deleteWhere { ActorTable.id eq actorId }
    }
}
