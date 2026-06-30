package exposed.examples.spring.modulith.boundaries.shipping

import exposed.examples.spring.modulith.boundaries.orders.events.OrderAcceptedEvent
import exposed.examples.spring.modulith.boundaries.shipping.internal.ExposedShippingReservationRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant

/**
 * Read model owned by the shipping bounded context.
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
 * Consumes order events without depending on order repositories or tables.
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
