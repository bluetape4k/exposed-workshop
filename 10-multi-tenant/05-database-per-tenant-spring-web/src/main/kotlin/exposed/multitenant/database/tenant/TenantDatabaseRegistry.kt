package exposed.multitenant.database.tenant

import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.database.config.TenantDataSourceProperties
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.EnumMap
import javax.sql.DataSource

class TenantDatabaseRegistry private constructor(
    private val entries: Map<TenantId, TenantDatabaseEntry>,
) : AutoCloseable {

    companion object {
        fun from(properties: TenantDataSourceProperties): TenantDatabaseRegistry {
            val configured = properties.tenants.mapKeys { (key, _) -> TenantId.fromHeader(key) }
            val missing = TenantId.entries.filterNot(configured::containsKey)
            require(missing.isEmpty()) {
                "Missing tenant datasource configuration: ${missing.joinToString { it.headerValue }}"
            }

            configured.forEach { (tenantId, jdbc) ->
                jdbc.validate("tenant-${tenantId.headerValue}")
            }

            val entries = EnumMap<TenantId, TenantDatabaseEntry>(TenantId::class.java)
            val dataSources = mutableListOf<HikariDataSource>()
            try {
                configured.forEach { (tenantId, jdbc) ->
                    val dataSource = jdbc.toHikariDataSource("tenant-${tenantId.headerValue}")
                    dataSources += dataSource
                    entries[tenantId] = TenantDatabaseEntry(
                        tenantId = tenantId,
                        dataSource = dataSource,
                        database = Database.connect(dataSource),
                    )
                }
            } catch (e: Exception) {
                try {
                    closeDataSources(dataSources)
                } catch (closeFailure: RuntimeException) {
                    e.addSuppressed(closeFailure)
                }
                throw e
            }
            return TenantDatabaseRegistry(entries)
        }

        private fun closeDataSources(dataSources: Iterable<HikariDataSource>) {
            var closeFailure: RuntimeException? = null
            dataSources.forEach { dataSource ->
                try {
                    dataSource.close()
                } catch (failure: Exception) {
                    if (closeFailure == null) {
                        closeFailure = IllegalStateException("Failed to close tenant datasource", failure)
                    } else {
                        closeFailure.addSuppressed(failure)
                    }
                }
            }
            closeFailure?.let { throw it }
        }
    }

    fun databaseFor(tenantId: TenantId): Database =
        entryFor(tenantId).database

    fun dataSourceFor(tenantId: TenantId): DataSource =
        entryFor(tenantId).dataSource

    fun configuredTenants(): Set<TenantId> =
        entries.keys

    private fun entryFor(tenantId: TenantId): TenantDatabaseEntry =
        entries[tenantId] ?: error("No datasource configured for tenant ${tenantId.headerValue}")

    override fun close() {
        closeDataSources(entries.values.map(TenantDatabaseEntry::dataSource))
    }
}

class TenantDatabaseEntry(
    val tenantId: TenantId,
    val dataSource: HikariDataSource,
    val database: Database,
)
