package exposed.multitenant.webflux.tenant

import exposed.multitenant.webflux.tenant.Tenants.Tenant
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


internal fun getSchemaDefinition(tenant: Tenants.Tenant): Schema =
    Schema(
        tenant.id,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )


suspend fun <T> newSuspendedTransactionWithTenant(
    tenant: Tenant? = null,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean? = null,
    statement: suspend Transaction.() -> T,
): T {
    val currentTenant = tenant ?: currentTenant()
    val context = Dispatchers.IO + TenantId(currentTenant)
    return newSuspendedTransaction(context, db, transactionIsolation, readOnly) {
        SchemaUtils.setSchema(getSchemaDefinition(currentTenant))
        statement()
    }
}
