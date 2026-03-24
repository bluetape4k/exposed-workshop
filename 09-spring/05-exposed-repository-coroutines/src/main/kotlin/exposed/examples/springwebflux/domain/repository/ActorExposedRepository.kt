package exposed.examples.springwebflux.domain.repository

import exposed.examples.springwebflux.domain.model.ActorRecord
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.toActorRecord
import io.bluetape4k.exposed.jdbc.repository.JdbcRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
/**
 * WebFlux 환경에서 배우 도메인을 조회/저장하는 Exposed 리포지토리입니다.
 */
class ActorExposedRepository: JdbcRepository<Long, ActorRecord> {

    companion object: KLoggingChannel()

    override val table = ActorTable
    override fun extractId(entity: ActorRecord): Long = entity.id
    override fun ResultRow.toEntity() = toActorRecord()

    /**
     * 검색 파라미터(식별자/이름/생일)로 배우 목록을 조회합니다.
     */
    fun searchActor(params: Map<String, String?>): List<ActorRecord> {
        log.debug { "Search Actor by params. params: $params" }

        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name       -> value?.run { val id = value.toLongOrNull() ?: return@forEach; query.andWhere { ActorTable.id eq id } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run {
                    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return@forEach
                    query.andWhere { ActorTable.birthday eq date }
                }
            }
        }
        return query.map { it.toEntity() }
    }

    /**
     * 신규 배우를 저장하고 생성된 식별자를 포함한 레코드를 반환합니다.
     */
    fun create(actor: ActorRecord): ActorRecord {
        log.debug { "Create Actor. actor: $actor" }

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
