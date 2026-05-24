package exposed.multitenant.ktor.tenant

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.path
import io.ktor.server.request.header
import io.ktor.util.AttributeKey
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private val TenantAttributeKey = AttributeKey<Tenant>("Tenant")

val TenantPlugin = createApplicationPlugin(name = "TenantPlugin") {
    onCall { call ->
        if (call.request.path() in publicPaths) {
            return@onCall
        }

        val tenantId = call.request.header(Tenant.HEADER_NAME)
            ?: throw IllegalArgumentException("${Tenant.HEADER_NAME} header is required")
        val tenant = Tenant.fromHeader(tenantId)
            ?: throw IllegalArgumentException("Unknown tenant: $tenantId")
        call.attributes.put(TenantAttributeKey, tenant)
    }
}

object TenantContext {
    private val current = ThreadLocal<Tenant?>()

    fun currentTenant(): Tenant =
        current.get() ?: throw IllegalStateException("Tenant context is not bound")

    fun currentTenantOrNull(): Tenant? = current.get()

    fun asContextElement(tenant: Tenant): TenantContextElement =
        TenantContextElement(tenant)

    internal fun bind(tenant: Tenant?): Tenant? {
        val previous = current.get()
        if (tenant == null) {
            current.remove()
        } else {
            current.set(tenant)
        }
        return previous
    }
}

class TenantContextElement(
    private val tenant: Tenant,
) : ThreadContextElement<Tenant?> {
    companion object Key: CoroutineContext.Key<TenantContextElement>

    override val key: CoroutineContext.Key<TenantContextElement> = Key

    override fun updateThreadContext(context: CoroutineContext): Tenant? =
        TenantContext.bind(tenant)

    override fun restoreThreadContext(context: CoroutineContext, oldState: Tenant?) {
        TenantContext.bind(oldState)
    }
}

suspend fun ApplicationCall.withTenantContext(block: suspend () -> Unit) {
    val tenant = attributes[TenantAttributeKey]
    withContext(TenantContext.asContextElement(tenant)) {
        block()
    }
}

private val publicPaths = setOf("/", "/health")
