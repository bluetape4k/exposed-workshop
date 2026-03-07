package exposed.examples.routing.datasource

import exposed.examples.routing.context.TenantContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager

class ContextAwareRoutingKeyResolverTest {

    private val resolver = ContextAwareRoutingKeyResolver(defaultTenant = "default")

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
    }

    @Test
    fun `tenant 컨텍스트가 없고 read-write이면 default-rw 키를 반환한다`() {
        val key = resolver.currentLookupKey()
        assertEquals("default:rw", key)
    }

    @Test
    fun `tenant가 acme이고 read-only이면 acme-ro 키를 반환한다`() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true)

        val key = TenantContext.withTenant("acme") {
            resolver.currentLookupKey()
        }

        assertEquals("acme:ro", key)
    }

    @Test
    fun `tenant가 공백이면 default tenant로 fallback 한다`() {
        val resolver = ContextAwareRoutingKeyResolver(
            defaultTenant = "default",
            tenantSupplier = { "   " },
        )

        assertEquals("default:rw", resolver.currentLookupKey())
    }
}
