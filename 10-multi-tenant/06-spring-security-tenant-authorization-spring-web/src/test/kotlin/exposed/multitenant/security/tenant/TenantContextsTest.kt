package exposed.multitenant.security.tenant

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.tenant.MissingTenantContextException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class TenantContextsTest {

    @AfterEach
    fun tearDown() {
        TenantContexts.currentOrNull().shouldBeNull()
    }

    @Test
    fun `missing tenant is not replaced with an application default`() {
        TenantContexts.currentOrNull().shouldBeNull()

        val failure = assertFailsWith<MissingTenantContextException> {
            TenantContexts.current()
        }

        failure.message shouldBeEqualTo "Tenant context is not bound"
    }

    @Test
    fun `nested and failing scopes restore the previous tenant`() {
        TenantContexts.withTenant(TenantId.ACME) {
            TenantContexts.current() shouldBeEqualTo TenantId.ACME

            val failure = assertFailsWith<IllegalStateException> {
                TenantContexts.withTenant(TenantId.GLOBEX) {
                    TenantContexts.current() shouldBeEqualTo TenantId.GLOBEX
                    error("nested failure")
                }
            }

            failure.message shouldBeEqualTo "nested failure"
            TenantContexts.current() shouldBeEqualTo TenantId.ACME
        }

        TenantContexts.currentOrNull().shouldBeNull()
    }
}
