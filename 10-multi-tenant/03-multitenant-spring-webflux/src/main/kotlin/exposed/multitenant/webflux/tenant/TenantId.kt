package exposed.multitenant.webflux.tenant

import exposed.multitenant.webflux.tenant.Tenants.Tenant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.ReactorContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * 코루틴의 [CoroutineContext] 에 [TenantId] 를 설정합니다.
 */
data class TenantId(val value: Tenants.Tenant): CoroutineContext.Element {
    companion object Key: CoroutineContext.Key<TenantId> {
        val DEFAULT = TenantId(Tenants.DEFAULT_TENANT)

        const val TENANT_ID_KEY = "TenantId"
    }

    override val key: CoroutineContext.Key<*> = Key

    override fun toString(): String {
        return "TenantId(value='$value')"
    }
}

/**
 * [ReactorContext] 에서 `TenantId` 의 정보를 읽어옵니다. 없으면 [TenantId.DEFAULT] 를 사용합니다.
 *
 * Webflux 에서 사용되는 코루틴의 [CoroutineContext] 에서 [TenantId] 를 읽어옵니다.
 */
suspend fun currentReactorTenant(): Tenants.Tenant =
    coroutineContext[ReactorContext]?.context?.getOrDefault(TenantId.TENANT_ID_KEY, TenantId.DEFAULT)?.value
        ?: Tenants.DEFAULT_TENANT


/**
 * 현재 코루틴의 [TenantId] 를 읽어옵니다. 없으면 [Tenants.DEFAULT_TENANT] 를 사용합니다.
 */
suspend fun currentTenant(): Tenants.Tenant =
    coroutineContext[TenantId]?.value ?: Tenants.DEFAULT_TENANT

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
