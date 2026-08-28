package exposed.multitenant.springweb.tenant

import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.tenant.MissingTenantContextException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.Callable
import java.util.concurrent.Executors

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
        TenantContexts.withTenant(Tenant.KOREAN) {
            TenantContexts.current() shouldBeEqualTo Tenant.KOREAN

            val failure = assertFailsWith<IllegalStateException> {
                TenantContexts.withTenant(Tenant.ENGLISH) {
                    TenantContexts.current() shouldBeEqualTo Tenant.ENGLISH
                    error("nested failure")
                }
            }

            failure.message shouldBeEqualTo "nested failure"
            TenantContexts.current() shouldBeEqualTo Tenant.KOREAN
        }

        TenantContexts.currentOrNull().shouldBeNull()
    }

    @EnabledOnJre(JRE.JAVA_25)
    @Test
    fun `parallel virtual threads keep scoped tenants isolated`() {
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            val tasks = List(20) { index ->
                Callable {
                    val tenant = if (index % 2 == 0) Tenant.KOREAN else Tenant.ENGLISH
                    TenantContexts.withTenant(tenant) {
                        TenantContexts.current()
                    }
                }
            }

            val tenants = executor.invokeAll(tasks).map { it.get() }
            tenants shouldBeEqualTo (0 until 20).map { index ->
                if (index % 2 == 0) Tenant.KOREAN else Tenant.ENGLISH
            }
        } finally {
            executor.close()
        }

        TenantContexts.currentOrNull().shouldBeNull()
    }
}
