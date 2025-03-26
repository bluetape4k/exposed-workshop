package exposed.shared.repository

import exposed.shared.repository.MovieSchema.ActorEntity
import exposed.shared.repository.MovieSchema.ActorTable
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate

class ActorRepository: ExposedRepository<ActorEntity, Long> {

    companion object: KLogging()

    override val table = ActorTable
    override fun ResultRow.toEntity(): ActorEntity = ActorEntity.wrapRow(this)

    fun searchActors(params: Map<String, String?>): List<ActorEntity> {
        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name -> value?.run { query.andWhere { ActorTable.id eq value.toLong() } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run { query.andWhere { ActorTable.birthday eq LocalDate.parse(value) } }
            }
        }

        return ActorEntity.wrapRows(query).toList()
    }

    fun save(actor: ActorDTO): ActorEntity {
        log.debug { "Create new actor. actor: $actor" }

        return ActorEntity.new {
            firstName = actor.firstName
            lastName = actor.lastName
            birthday = actor.birthday?.let { LocalDate.parse(it) }
        }
    }


}
