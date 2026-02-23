package exposed.examples.routing.context

/**
 * 현재 스레드의 tenant 식별자를 보관하는 컨텍스트입니다.
 */
object TenantContext {
    private val currentTenant = ThreadLocal<String?>()

    /**
     * 현재 tenant 식별자를 반환합니다.
     */
    fun currentTenant(): String? = currentTenant.get()

    /**
     * 현재 tenant 컨텍스트를 정리합니다.
     */
    fun clear() {
        currentTenant.remove()
    }

    /**
     * 지정한 [tenantId]를 컨텍스트에 바인딩한 뒤 [block]을 실행합니다.
     */
    fun <T> withTenant(tenantId: String, block: () -> T): T {
        val previous = currentTenant.get()
        currentTenant.set(tenantId)
        return try {
            block()
        } finally {
            if (previous == null) {
                currentTenant.remove()
            } else {
                currentTenant.set(previous)
            }
        }
    }
}

