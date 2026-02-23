package exposed.examples.routing.datasource

import javax.sql.DataSource

/**
 * 라우팅 키에 대응하는 [DataSource]를 등록/조회하는 레지스트리입니다.
 */
interface DataSourceRegistry {

    /**
     * [key]에 [dataSource]를 등록합니다.
     */
    fun register(key: String, dataSource: DataSource)

    /**
     * [key]에 해당하는 [DataSource]를 반환합니다.
     */
    fun get(key: String): DataSource?

    /**
     * [key]가 레지스트리에 존재하면 `true`를 반환합니다.
     */
    fun contains(key: String): Boolean

    /**
     * 현재 등록된 키 목록을 반환합니다.
     */
    fun keys(): Set<String>
}

