package exposed.examples.routing.web

import exposed.examples.routing.context.TenantContext
import exposed.examples.routing.domain.RoutingMarkerRepository
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.transaction.support.TransactionSynchronizationManager

@SpringBootTest
@AutoConfigureMockMvc
class RoutingMarkerControllerTest(
    @param:Autowired private val mockMvc: MockMvc,
    @param:Autowired private val markerRepository: RoutingMarkerRepository,
) {

    @BeforeEach
    fun resetMarkers() {
        seedMarker("default", false, "default-rw")
        seedMarker("default", true, "default-ro")
        seedMarker("acme", false, "acme-rw")
        seedMarker("acme", true, "acme-ro")
    }

    @Test
    fun `기본 tenant 요청은 default-rw 마커를 반환한다`() {
        mockMvc.get("/routing/marker")
            .andExpect {
                status { isOk() }
                jsonPath("$.tenant", equalTo("default"))
                jsonPath("$.readOnly", equalTo(false))
                jsonPath("$.marker", equalTo("default-rw"))
            }
    }

    @Test
    fun `acme tenant read-write 요청은 acme-rw 마커를 반환한다`() {
        mockMvc.get("/routing/marker") {
            header(TenantHeaderFilter.TENANT_HEADER, "acme")
        }.andExpect {
            status { isOk() }
            jsonPath("$.tenant", equalTo("acme"))
            jsonPath("$.readOnly", equalTo(false))
            jsonPath("$.marker", equalTo("acme-rw"))
        }
    }

    @Test
    fun `acme tenant read-only 요청은 acme-ro 마커를 반환한다`() {
        mockMvc.get("/routing/marker/readonly") {
            header(TenantHeaderFilter.TENANT_HEADER, "acme")
        }.andExpect {
            status { isOk() }
            jsonPath("$.tenant", equalTo("acme"))
            jsonPath("$.readOnly", equalTo(true))
            jsonPath("$.marker", equalTo("acme-ro"))
        }
    }

    @Test
    fun `마커를 갱신하면 같은 tenant의 read-write 경로에서 변경값이 조회된다`() {
        mockMvc.patch("/routing/marker") {
            header(TenantHeaderFilter.TENANT_HEADER, "acme")
            contentType = MediaType.APPLICATION_JSON
            content = """{"marker":"acme-rw-updated"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.marker", equalTo("acme-rw-updated"))
        }

        mockMvc.get("/routing/marker") {
            header(TenantHeaderFilter.TENANT_HEADER, "acme")
        }.andExpect {
            status { isOk() }
            jsonPath("$.marker", equalTo("acme-rw-updated"))
        }
    }

    private fun seedMarker(tenant: String, readOnly: Boolean, marker: String) {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly)
        try {
            TenantContext.withTenant(tenant) {
                markerRepository.resetAndInsert(marker)
            }
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
            TenantContext.clear()
        }
    }
}
