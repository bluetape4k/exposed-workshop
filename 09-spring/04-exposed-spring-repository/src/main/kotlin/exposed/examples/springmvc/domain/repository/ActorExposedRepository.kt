package exposed.examples.springmvc.domain.repository

import exposed.examples.springmvc.domain.dtos.ActorDTO
import exposed.examples.springmvc.domain.model.MovieSchema.ActorEntity
import exposed.examples.springmvc.domain.model.MovieSchema.ActorTable
import exposed.shared.repository.AbstractExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
class ActorExposedRepository: AbstractExposedRepository<ActorEntity, Long>(ActorTable) {

    companion object: KLogging()

    override fun ResultRow.toEntity(): ActorEntity = ActorEntity.wrapRow(this)

    /**
     * 주어진 조건에 맞는 [ActorEntity]를 조회합니다.
     */
    @Transactional(readOnly = true)
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

    fun create(actor: ActorDTO): ActorEntity {
        log.debug { "Create new actor. actor: $actor" }

        return ActorEntity.new {
            firstName = actor.firstName
            lastName = actor.lastName
            birthday = actor.birthday?.let { LocalDate.parse(it) }
        }
    }

}
