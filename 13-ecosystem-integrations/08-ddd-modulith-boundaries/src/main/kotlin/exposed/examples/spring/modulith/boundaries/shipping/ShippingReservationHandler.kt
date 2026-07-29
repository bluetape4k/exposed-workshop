package exposed.examples.spring.modulith.boundaries.shipping

import exposed.examples.spring.modulith.boundaries.orders.events.OrderAcceptedEvent
import exposed.examples.spring.modulith.boundaries.shipping.internal.ExposedShippingReservationRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant

/**
 * shipping bounded context가 소유하는 조회 모델이다.
 */
data class ShippingReservation(
    val id: Long,
    val orderKey: String,
    val customerId: String,
    val reservedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * order 저장소나 테이블에 의존하지 않고 order event를 소비한다.
 */
@Service
class ShippingReservationHandler(
    private val transactionTemplate: TransactionTemplate,
    private val shippingRepository: ExposedShippingReservationRepository,
) {

    @EventListener
    fun reserveShipment(event: OrderAcceptedEvent) {
        transactionTemplate.executeWithoutResult {
            shippingRepository.reserve(event)
        }
    }

    fun findReservation(orderKey: String): ShippingReservation? =
        transactionTemplate.execute {
            shippingRepository.findByOrderKey(orderKey)
        }
}
