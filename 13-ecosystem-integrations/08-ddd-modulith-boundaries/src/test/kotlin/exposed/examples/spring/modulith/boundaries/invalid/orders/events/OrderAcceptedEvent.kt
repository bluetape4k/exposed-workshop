package exposed.examples.spring.modulith.boundaries.invalid.orders.events

import java.io.Serializable
import java.time.Instant

data class OrderAcceptedEvent(
    val orderKey: String,
    val customerId: String,
    val acceptedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
