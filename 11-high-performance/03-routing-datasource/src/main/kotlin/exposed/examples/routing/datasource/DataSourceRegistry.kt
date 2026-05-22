package exposed.examples.routing.datasource

import javax.sql.DataSource

/**
 * 라우팅 키에 대응하는 [DataSource]를 등록/조회하는 레지스트리입니다.
 */
interface DataSourceRegistry: AutoCloseable {

    /**
     * [key]에 [dataSource]를 등록합니다.
     *
     * 같은 [key]가 이미 등록되어 있으면 새 값으로 교체하지만, 기존 [DataSource]는 닫지 않습니다.
     * 교체 전 리소스 해제 책임은 호출자에게 있습니다.
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

    /**
     * 레지스트리가 소유한 [DataSource] 리소스를 해제합니다.
     *
     * 이미 닫힌 레지스트리에 대해 다시 호출해도 안전해야 합니다. 호출 후 레지스트리는 비워지며 재사용하지 않는 것을
     * 권장합니다. 하나 이상의 리소스 해제에 실패하면 나머지 리소스 해제를 계속 시도한 뒤 실패 원인을 suppressed
     * exception으로 담은 [IllegalStateException]을 던집니다.
     */
    override fun close()
}
