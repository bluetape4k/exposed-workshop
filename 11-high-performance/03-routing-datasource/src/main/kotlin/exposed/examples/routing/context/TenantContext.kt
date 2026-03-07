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
     *
     * [tenantId] 가 `null` 이거나 공백이면 현재 요청 구간에서는 tenant 컨텍스트를 비운 상태로 실행하고,
     * 종료 후에는 이전 컨텍스트를 복원합니다.
     */
    fun <T> withTenant(tenantId: String?, block: () -> T): T {
        val previous = currentTenant.get()
        if (tenantId.isNullOrBlank()) {
            currentTenant.remove()
        } else {
            currentTenant.set(tenantId)
        }
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
