package exposed.workshop.springmvc.domain.repository

import exposed.workshop.springmvc.domain.model.ActorRecord
import exposed.workshop.springmvc.domain.model.MovieSchema.ActorTable
import exposed.workshop.springmvc.domain.model.toActorRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class ActorRepository {

    companion object: KLogging()

    fun findById(actorId: Long): ActorRecord? {
        log.debug { "Find Actor by id. id: $actorId" }

        return ActorTable.selectAll()
            .where { ActorTable.id eq actorId }
            .firstOrNull()
            ?.toActorRecord()

        // Entity로 조회하는 방법
        // ActorEntity.findById(actorId)?.toActorRecord()
    }

    fun searchActors(params: Map<String, String?>): List<ActorRecord> {
        val query: Query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name        -> value?.let { parseLongParam(key, it) }
                    ?.let { query.andWhere { ActorTable.id eq it } }
                ActorTable::firstName.name -> value?.let { query.andWhere { ActorTable.firstName eq it } }
                ActorTable::lastName.name  -> value?.let { query.andWhere { ActorTable.lastName eq it } }
                ActorTable::birthday.name  -> value?.let { parseLocalDateParam(key, it) }
                    ?.let { query.andWhere { ActorTable.birthday eq it } }
            }
        }

        return query.map { it.toActorRecord() }
    }

    @Transactional
    fun create(actor: ActorRecord): ActorRecord {
        log.debug { "Create Actor. actor: $actor" }

        val actorId = ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            actor.birthday?.let { day ->
                it[birthday] = runCatching { LocalDate.parse(day) }.getOrNull()
            }
        }
        return actor.copy(id = actorId.value)
    }

    @Transactional
    fun deleteById(actorId: Long): Int {
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
