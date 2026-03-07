package exposed.examples.routing.web

import exposed.examples.routing.context.TenantContext
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TenantHeaderFilterTest {

    private val filter = TenantHeaderFilter()

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
    }

    @Test
    fun `tenant header가 없으면 이전 tenant context를 사용하지 않고 종료 후 복원한다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        TenantContext.withTenant("stale-tenant") {
            filter.doFilter(request, response, FilterChain { _, _ ->
                assertNull(TenantContext.currentTenant())
            })

            assertEquals("stale-tenant", TenantContext.currentTenant())
        }

        assertNull(TenantContext.currentTenant())
    }

    @Test
    fun `tenant header가 있으면 요청 동안만 해당 tenant를 바인딩한다`() {
        val request = MockHttpServletRequest().apply {
            addHeader(TenantHeaderFilter.TENANT_HEADER, "acme")
        }
        val response = MockHttpServletResponse()

        TenantContext.withTenant("outer-tenant") {
            filter.doFilter(request, response, FilterChain { _, _ ->
                assertEquals("acme", TenantContext.currentTenant())
            })

            assertEquals("outer-tenant", TenantContext.currentTenant())
        }

        assertNull(TenantContext.currentTenant())
    }
}
