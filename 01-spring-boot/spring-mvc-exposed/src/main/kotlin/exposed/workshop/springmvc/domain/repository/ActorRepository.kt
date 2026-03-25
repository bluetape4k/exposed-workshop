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
import java.time.LocalDate

/**
 * 배우(Actor) 데이터에 대한 저장소.
 *
 * Exposed DSL을 사용하여 배우 조회, 검색, 생성, 삭제 기능을 제공합니다.
 */
@Repository
class ActorRepository {

    companion object: KLogging()

    /**
     * ID로 배우를 조회합니다.
     *
     * @param actorId 조회할 배우 ID
     * @return 배우 레코드, 존재하지 않으면 null
     */
    fun findById(actorId: Long): ActorRecord? {
        log.debug { "Find Actor by id. id: $actorId" }

        return ActorTable.selectAll()
            .where { ActorTable.id eq actorId }
            .firstOrNull()
            ?.toActorRecord()

        // Entity로 조회하는 방법
        // ActorEntity.findById(actorId)?.toActorRecord()
    }

    /**
     * 파라미터 조건으로 배우를 검색합니다.
     *
     * @param params 검색 조건 맵 (필드명 → 값)
     * @return 조건에 맞는 배우 레코드 목록
     */
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

    /**
     * 새 배우를 데이터베이스에 저장합니다.
     *
     * @param actor 저장할 배우 정보
     * @return 생성된 배우 레코드 (ID 포함)
     */
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

    /**
     * ID로 배우를 삭제합니다.
     *
     * @param actorId 삭제할 배우 ID
     * @return 삭제된 행 수
     */
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
