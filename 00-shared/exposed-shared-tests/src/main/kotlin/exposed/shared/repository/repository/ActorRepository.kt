package exposed.shared.repository.repository

import exposed.shared.repository.model.ActorRecord
import exposed.shared.repository.model.MovieSchema
import exposed.shared.repository.model.toActorRecord
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

class ActorRepository: ExposedRepository<ActorRecord, Long> {

    companion object: KLogging()

    override val table = MovieSchema.ActorTable
    override fun ResultRow.toEntity(): ActorRecord = toActorRecord()

    fun searchActors(params: Map<String, String?>): List<ActorRecord> {
        val query = MovieSchema.ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieSchema.ActorTable::id.name -> value?.run { query.andWhere { MovieSchema.ActorTable.id eq value.toLong() } }
                MovieSchema.ActorTable::firstName.name -> value?.run { query.andWhere { MovieSchema.ActorTable.firstName eq value } }
                MovieSchema.ActorTable::lastName.name -> value?.run { query.andWhere { MovieSchema.ActorTable.lastName eq value } }
                MovieSchema.ActorTable::birthday.name -> value?.run {
                    query.andWhere {
                        MovieSchema.ActorTable.birthday eq LocalDate.parse(
                            value
                        )
                    }
                }
            }
        }

        return query.map { it.toEntity() }
    }

    fun save(actor: ActorRecord): ActorRecord {
        log.debug { "Create new actor. actor: $actor" }

        val id = MovieSchema.ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            actor.birthday?.let { day ->
                it[birthday] = runCatching { LocalDate.parse(day) }.getOrNull()
            }
        }
        return actor.copy(id = id.value)
    }
}
