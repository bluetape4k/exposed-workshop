package exposed.examples.routing.datasource

import exposed.examples.routing.context.TenantContext
import exposed.examples.routing.domain.RoutingMarkerRepository
import org.h2.Driver
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

/** 테넌트 컨텍스트와 읽기/쓰기 여부에 따라 적절한 데이터소스로 동적 라우팅하는 `DynamicRoutingDataSource`를 검증합니다. */
class DynamicRoutingDataSourceTest {

    private val registry = InMemoryDataSourceRegistry().apply {
        register("default:rw", markerDataSource("default-rw"))
        register("default:ro", markerDataSource("default-ro"))
        register("acme:rw", markerDataSource("acme-rw"))
        register("acme:ro", markerDataSource("acme-ro"))
    }
    private val resolver = ContextAwareRoutingKeyResolver(defaultTenant = "default")
    private val routingDataSource = DynamicRoutingDataSource(registry, resolver)
    private val routingRepository = RoutingMarkerRepository(Database.connect(routingDataSource))

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
    }

    @Test
    fun `기본 컨텍스트에서는 default-rw 데이터소스로 라우팅한다`() {
        val marker = routingRepository.findCurrentMarker()
        assertEquals("default-rw", marker)
    }

    @Test
    fun `tenant가 acme이면 acme-rw 데이터소스로 라우팅한다`() {
        val marker = TenantContext.withTenant("acme") {
            routingRepository.findCurrentMarker()
        }
        assertEquals("acme-rw", marker)
    }

    @Test
    fun `read-only 트랜잭션이면 ro 데이터소스로 라우팅한다`() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true)
        val marker = TenantContext.withTenant("acme") {
            routingRepository.findCurrentMarker()
        }
        assertEquals("acme-ro", marker)
    }

    @Test
    fun `등록되지 않은 키는 예외를 발생시킨다`() {
        val invalidResolver = RoutingKeyResolver { "missing:rw" }
        val invalidRoutingDataSource = DynamicRoutingDataSource(registry, invalidResolver)
        val invalidRepository = RoutingMarkerRepository(Database.connect(invalidRoutingDataSource))

        assertThrows(IllegalStateException::class.java) {
            invalidRepository.findCurrentMarker()
        }
    }

    private fun markerDataSource(marker: String): SimpleDriverDataSource {
        val dataSource = SimpleDriverDataSource(
            Driver(),
            "jdbc:h2:mem:routing_$marker;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            "",
        )
        val repository = RoutingMarkerRepository(Database.connect(dataSource))
        repository.resetAndInsert(marker)
        return dataSource
    }
}
