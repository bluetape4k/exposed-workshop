package exposed.examples.spring.modulith.boundaries.orders

import exposed.examples.spring.modulith.boundaries.orders.events.OrderAcceptedEvent
import exposed.examples.spring.modulith.boundaries.orders.internal.ExposedOrderRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant

/**
 * order bounded context가 새 워크숍 주문을 접수할 때 사용하는 command이다.
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
 * order aggregate가 저장된 뒤 반환되는 조회 모델이다.
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
 * order 트랜잭션을 책임지고 domain event를 발행하는 애플리케이션 서비스이다.
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
