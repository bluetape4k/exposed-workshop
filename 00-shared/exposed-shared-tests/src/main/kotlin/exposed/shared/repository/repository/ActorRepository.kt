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
 * [JdbcRepository]를 구현하며 [ActorTable]을 대상으로
 * 배우 조회, 검색, 저장 기능을 제공합니다.
 *
 * 트랜잭션 컨텍스트 내에서 사용해야 합니다.
 *
 * @see ActorTable
 * @see ActorRecord
 * @see JdbcRepository
 */
class ActorRepository: JdbcRepository<Long, ActorTable, ActorRecord> {

    companion object: KLogging()

    /** 이 리포지토리가 대상으로 하는 [ActorTable] 참조 */
    override val table = ActorTable

    /**
     * [ResultRow]를 [ActorRecord]로 변환합니다.
     *
     * @return 변환된 [ActorRecord] 인스턴스
     */
    override fun ResultRow.toEntity(): ActorRecord = toActorRecord()

    /**
     * 주어진 파라미터 맵을 기반으로 배우를 동적으로 검색합니다.
     *
     * 지원되는 파라미터 키:
     * - `id`: 배우 ID로 필터링
     * - `firstName`: 배우 이름으로 필터링
     * - `lastName`: 배우 성으로 필터링
     * - `birthday`: 생년월일로 필터링 (ISO 날짜 형식: `yyyy-MM-dd`)
     *
     * 파라미터 값이 `null`인 경우 해당 조건은 무시됩니다.
     *
     * @param params 검색 조건을 담은 맵 (컬럼 이름 -> 값)
     * @return 검색 조건에 일치하는 [ActorRecord] 목록
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
     * 새로운 배우 정보를 데이터베이스에 저장합니다.
     *
     * 저장 후 데이터베이스에서 생성된 ID를 포함한 [ActorRecord]를 반환합니다.
     * `birthday` 값이 유효하지 않은 날짜 형식인 경우 `null`로 저장됩니다.
     *
     * @param actor 저장할 배우 정보 ([ActorRecord])
     * @return 데이터베이스에서 생성된 ID가 설정된 [ActorRecord]
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
