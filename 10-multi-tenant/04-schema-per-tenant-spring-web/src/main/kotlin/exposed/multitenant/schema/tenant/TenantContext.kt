package exposed.multitenant.schema.tenant

object TenantContext {

    private val currentTenant = ThreadLocal<TenantId?>()

    fun set(tenantId: TenantId) {
        currentTenant.set(tenantId)
    }

    fun current(): TenantId =
        currentTenant.get() ?: error("TenantContext is not set")

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
