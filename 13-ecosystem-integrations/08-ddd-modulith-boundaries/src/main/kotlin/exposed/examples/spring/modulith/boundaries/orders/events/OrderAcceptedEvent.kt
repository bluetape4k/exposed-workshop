package exposed.examples.spring.modulith.boundaries.orders.events

import java.io.Serializable
import java.time.Instant

/**
 * order context의 유일한 named interface로 공개되는 domain event이다.
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
