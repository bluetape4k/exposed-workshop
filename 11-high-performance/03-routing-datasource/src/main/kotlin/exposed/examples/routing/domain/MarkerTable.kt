package exposed.examples.routing.domain

import org.jetbrains.exposed.v1.core.Table

/**
 * 현재 라우팅된 데이터소스를 검증하기 위한 마커 테이블입니다.
 */
object MarkerTable: Table("marker") {

    /**
     * 데이터소스 식별 문자열입니다.
     */
    val name = varchar("name", 100)
}

