package exposed.examples.routing.domain

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed를 사용해 현재 라우팅된 데이터소스의 마커 값을 조회/적재하는 저장소입니다.
 */
class RoutingMarkerRepository(
    private val database: Database,
) {

    /**
     * 현재 라우팅된 데이터소스의 마커 값을 조회합니다.
     */
    fun findCurrentMarker(): String? = transaction(database) {
        MarkerTable
            .selectAll()
            .singleOrNull()
            ?.get(MarkerTable.name)
    }

    /**
     * 현재 라우팅된 데이터소스의 마커 값을 1건으로 초기화합니다.
     */
    fun resetAndInsert(marker: String) {
        transaction(database) {
            SchemaUtils.create(MarkerTable)
            MarkerTable.deleteAll()
            MarkerTable.insert {
                it[name] = marker
            }
        }
    }
}

