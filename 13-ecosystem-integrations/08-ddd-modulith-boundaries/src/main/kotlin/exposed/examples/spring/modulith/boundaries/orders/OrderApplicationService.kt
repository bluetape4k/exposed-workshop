package exposed.examples.spring.modulith.boundaries.orders

import exposed.examples.spring.modulith.boundaries.orders.events.OrderAcceptedEvent
import exposed.examples.spring.modulith.boundaries.orders.internal.ExposedOrderRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant

/**
 * Command used by the order bounded context to accept a new workshop order.
 */
data class AcceptOrderCommand(
    val orderKey: String,
    val customerId: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Read model returned after the order aggregate is persisted.
 */
data class OrderSummary(
    val id: Long,
    val orderKey: String,
    val customerId: String,
    val status: String,
    val acceptedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Application service that owns the order transaction and publishes a domain event.
 */
@Service
class OrderApplicationService(
    private val transactionTemplate: TransactionTemplate,
    private val orderRepository: ExposedOrderRepository,
    private val events: ApplicationEventPublisher,
) {

    fun accept(command: AcceptOrderCommand): OrderSummary {
        val summary = transactionTemplate.execute {
            orderRepository.accept(command)
        }

        events.publishEvent(
            OrderAcceptedEvent(
                orderKey = summary.orderKey,
                customerId = summary.customerId,
                acceptedAt = summary.acceptedAt,
            )
        )

        return summary
    }
}
