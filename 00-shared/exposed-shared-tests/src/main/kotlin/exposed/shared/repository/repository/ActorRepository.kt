package exposed.shared.repository.repository

import exposed.shared.repository.model.ActorRecord
import exposed.shared.repository.model.MovieSchema.ActorTable
import exposed.shared.repository.model.toActorRecord
import io.bluetape4k.exposed.jdbc.repository.JdbcRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

/**
 * 배우(Actor) 데이터에 대한 JDBC 기반 리포지토리 구현체.
 *
 * [ActorTable]을 통해 배우 정보를 조회하고 저장하는 기능을 제공합니다.
 */
class ActorRepository: JdbcRepository<Long, ActorRecord> {

    companion object: KLogging()

    override val table = ActorTable
    override fun extractId(entity: ActorRecord): Long = entity.id
    override fun ResultRow.toEntity(): ActorRecord = toActorRecord()

    /**
     * 주어진 검색 파라미터를 기반으로 배우 목록을 조회합니다.
     *
     * 지원하는 파라미터 키: `id`, `firstName`, `lastName`, `birthday`
     *
     * @param params 검색 조건을 담은 파라미터 맵 (키: 컬럼명, 값: 검색값)
     * @return 검색 조건에 맞는 [ActorRecord] 목록
     */
    fun searchActors(params: Map<String, String?>): List<ActorRecord> {
        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name        -> value?.run { query.andWhere { ActorTable.id eq value.toLong() } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name  -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name  -> value?.run {
                    query.andWhere {
                        ActorTable.birthday eq LocalDate.parse(
                            value
                        )
                    }
                }
            }
        }

        return query.map { it.toEntity() }
    }

    /**
     * 새로운 배우 레코드를 데이터베이스에 저장합니다.
     *
     * @param actor 저장할 배우 정보
     * @return 저장된 배우 정보 (생성된 ID 포함)
     */
    fun save(actor: ActorRecord): ActorRecord {
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
