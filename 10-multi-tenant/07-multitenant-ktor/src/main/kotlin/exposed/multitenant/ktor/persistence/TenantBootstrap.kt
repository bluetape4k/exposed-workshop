package exposed.multitenant.ktor.persistence

import exposed.multitenant.ktor.tenant.Tenant
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TenantBootstrap(
    private val database: Database,
) {
    fun initialize() {
        Tenant.entries.forEach { tenant ->
            transaction(database) {
                exec("CREATE SCHEMA IF NOT EXISTS ${tenant.schema}")
                exec("SET SCHEMA ${tenant.schema}")
                SchemaUtils.create(Movies)
                if (Movies.selectAll().empty()) {
                    tenant.seedTitles.forEach { title ->
                        Movies.insert {
                            it[Movies.title] = title
                        }
                    }
                }
            }
        }
    }
}
