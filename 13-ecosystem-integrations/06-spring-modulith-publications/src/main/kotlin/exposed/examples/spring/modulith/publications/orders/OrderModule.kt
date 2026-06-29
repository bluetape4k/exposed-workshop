package exposed.examples.spring.modulith.publications.orders

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant

object WorkshopOrders: LongIdTable("modulith_orders") {
    val orderKey = varchar("order_key", 80).uniqueIndex()
    val customerId = varchar("customer_id", 80)
    val status = varchar("status", 30)
    val approvedAt = timestamp("approved_at").nullable()
}

data class ApproveOrderCommand(
    val orderKey: String,
    val customerId: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class OrderApprovedEvent(
    val orderKey: String,
    val customerId: String,
    val approvedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class OrderSummary(
    val id: Long,
    val orderKey: String,
    val customerId: String,
    val status: String,
    val approvedAt: Instant?,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Service
class OrderApplicationService(
    private val transactionTemplate: TransactionTemplate,
    private val events: ApplicationEventPublisher,
) {

    fun approve(command: ApproveOrderCommand): OrderSummary =
        transactionTemplate.execute {
            val approvedAt = Instant.now()
            val id = WorkshopOrders.insertAndGetId { row ->
                row[orderKey] = command.orderKey
                row[customerId] = command.customerId
                row[status] = "APPROVED"
                row[WorkshopOrders.approvedAt] = approvedAt
            }
            val summary = WorkshopOrders
                .selectAll()
                .where { WorkshopOrders.id eq id }
                .single()
                .toOrderSummary()

            events.publishEvent(
                OrderApprovedEvent(
                    orderKey = summary.orderKey,
                    customerId = summary.customerId,
                    approvedAt = summary.approvedAt ?: approvedAt,
                )
            )
            summary
        }
}

private fun ResultRow.toOrderSummary(): OrderSummary =
    OrderSummary(
        id = this[WorkshopOrders.id].value,
        orderKey = this[WorkshopOrders.orderKey],
        customerId = this[WorkshopOrders.customerId],
        status = this[WorkshopOrders.status],
        approvedAt = this[WorkshopOrders.approvedAt],
    )
