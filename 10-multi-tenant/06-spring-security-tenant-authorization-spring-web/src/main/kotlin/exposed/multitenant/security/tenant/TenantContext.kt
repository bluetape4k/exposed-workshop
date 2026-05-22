package exposed.multitenant.security.tenant

object TenantContext {

    private val currentTenant = ThreadLocal<TenantId?>()

    fun set(tenantId: TenantId) {
        currentTenant.set(tenantId)
    }

    fun current(): TenantId =
        currentTenant.get() ?: throw TenantContextException("TenantContext is not set")

    fun currentOrNull(): TenantId? =
        currentTenant.get()

    fun clear() {
        currentTenant.remove()
    }

    inline fun <T> withTenant(tenantId: TenantId, block: () -> T): T {
        val previous = currentOrNull()
        set(tenantId)
        return try {
            block()
        } finally {
            if (previous == null) clear() else set(previous)
        }
    }
}

class TenantContextException(message: String) : IllegalStateException(message)
