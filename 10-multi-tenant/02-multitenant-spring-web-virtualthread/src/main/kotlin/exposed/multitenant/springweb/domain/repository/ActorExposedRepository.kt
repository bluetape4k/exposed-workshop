package exposed.multitenant.springweb.domain.repository

import exposed.multitenant.springweb.domain.dtos.ActorRecord
import exposed.multitenant.springweb.domain.dtos.toActorRecord
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorEntity
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.jdbc.repository.JdbcRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 배우 도메인에 대한 Exposed 기반 저장소입니다.
 */
@Repository
class ActorExposedRepository: JdbcRepository<Long, ActorRecord> {

    companion object: KLogging()

    override val table = ActorTable
    override fun extractId(entity: ActorRecord): Long = entity.id

    override fun ResultRow.toEntity() = toActorRecord()

    /**
     * 주어진 조건에 맞는 [ActorEntity]를 조회합니다.
     */
    @Transactional(readOnly = true)
    fun searchActors(params: Map<String, String?>): List<ActorRecord> {
        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name       -> value?.run { val id = value.toLongOrNull() ?: return@forEach; query.andWhere { ActorTable.id eq id } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run { val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return@forEach; query.andWhere { ActorTable.birthday eq date } }
            }
        }

        return query.map { it.toEntity() }
    }

    /**
     * 배우 정보를 저장하고 생성된 식별자를 포함한 레코드를 반환합니다.
     */
    fun create(actor: ActorRecord): ActorRecord {
        log.debug { "Create new actor. actor: $actor" }

        val id = ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            actor.birthday?.let { day ->
                it[birthday] = runCatching { LocalDate.parse(day) }.getOrNull()
            }
        }
        return actor.copy(id = id.value)
    }
}
