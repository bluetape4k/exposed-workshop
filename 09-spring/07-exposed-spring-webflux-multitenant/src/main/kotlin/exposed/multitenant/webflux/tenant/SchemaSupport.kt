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

/**
 * [newSuspendedTransaction] 함수를 수행할 때, [tenant] 를 전달하도록 합니다.
 */
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

/**
 * [newSuspendedTransaction] 함수를 호출할 때, ReactorContext 에 있는 [TenantId]에 해당하는 Schema 를 사용하도록 합니다.
 */
suspend fun <T> newSuspendedTransactionWithCurrentReactorTenant(
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean? = null,
    statement: suspend Transaction.() -> T,
): T = newSuspendedTransactionWithTenant(
    currentReactorTenant(),
    db,
    transactionIsolation,
    readOnly,
    statement
)
