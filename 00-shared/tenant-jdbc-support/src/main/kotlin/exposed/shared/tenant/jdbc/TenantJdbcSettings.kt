package exposed.shared.tenant.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.Serializable

/**
 * Spring binding과 무관한 tenant JDBC 연결 설정입니다.
 *
 * 이 타입은 설정값 검증과 Hikari factory를 공용화하지만, tenant 식별은
 * consumer에, resource lifecycle은 provider registry에 남깁니다.
 */
data class TenantJdbcSettings(
    val jdbcUrl: String = "",
    val username: String = "sa",
    val password: String = "",
    val driverClassName: String = "org.h2.Driver",
    val maximumPoolSize: Int? = null,
    val minimumIdle: Int? = null,
    val connectionTimeoutMs: Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    /**
     * Hikari pool을 만들기 전에 필수 JDBC 설정과 인메모리 H2 lifecycle 옵션을 확인합니다.
     */
    fun validate(poolName: String) {
        require(jdbcUrl.isNotBlank()) { "jdbcUrl must not be blank for $poolName" }
        require(!jdbcUrl.startsWith("jdbc:h2:", ignoreCase = true) || jdbcUrl.contains("DB_CLOSE_DELAY=-1")) {
            "H2 tenant URL must include DB_CLOSE_DELAY=-1 for $poolName"
        }
    }

    /**
     * 검증된 설정으로 Hikari datasource를 만들고, 반환된 datasource의 소유권은 caller에게 넘깁니다.
     */
    fun toHikariDataSource(poolName: String): HikariDataSource {
        validate(poolName)
        val config = HikariConfig().apply {
            this.poolName = poolName
            this.jdbcUrl = this@TenantJdbcSettings.jdbcUrl
            this.username = this@TenantJdbcSettings.username
            this.password = this@TenantJdbcSettings.password
            this.driverClassName = this@TenantJdbcSettings.driverClassName
            this.maximumPoolSize = this@TenantJdbcSettings.maximumPoolSize ?: 4
            this.minimumIdle = this@TenantJdbcSettings.minimumIdle ?: 1
            this.connectionTimeout = this@TenantJdbcSettings.connectionTimeoutMs ?: 5_000L
        }
        return HikariDataSource(config)
    }
}
