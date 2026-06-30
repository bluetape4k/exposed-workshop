package exposed.examples.spring.modulith.boundaries.orders.events

import java.io.Serializable
import java.time.Instant

/**
 * Domain event exported as the only named interface of the order context.
 */
data class OrderAcceptedEvent(
    val orderKey: String,
    val customerId: String,
    val acceptedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
