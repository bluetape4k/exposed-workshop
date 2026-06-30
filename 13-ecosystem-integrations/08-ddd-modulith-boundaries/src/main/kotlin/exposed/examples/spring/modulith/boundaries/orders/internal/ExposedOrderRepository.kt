package exposed.examples.spring.modulith.boundaries.orders.internal

import exposed.examples.spring.modulith.boundaries.orders.AcceptOrderCommand
import exposed.examples.spring.modulith.boundaries.orders.OrderSummary
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.Instant

object WorkshopOrders: LongIdTable("ddd_modulith_orders") {
    val orderKey = varchar("order_key", 80).uniqueIndex()
    val customerId = varchar("customer_id", 80)
    val status = varchar("status", 30)
    val acceptedAt = timestamp("accepted_at")
}

@Repository
class ExposedOrderRepository {

    fun accept(command: AcceptOrderCommand): OrderSummary {
        val acceptedAt = Instant.now()
        val id = WorkshopOrders.insertAndGetId { row ->
            row[orderKey] = command.orderKey
            row[customerId] = command.customerId
            row[status] = "ACCEPTED"
            row[WorkshopOrders.acceptedAt] = acceptedAt
        }

        return WorkshopOrders
            .selectAll()
            .where { WorkshopOrders.id eq id }
            .single()
            .toOrderSummary()
    }
}

private fun ResultRow.toOrderSummary(): OrderSummary =
    OrderSummary(
        id = this[WorkshopOrders.id].value,
        orderKey = this[WorkshopOrders.orderKey],
        customerId = this[WorkshopOrders.customerId],
        status = this[WorkshopOrders.status],
        acceptedAt = this[WorkshopOrders.acceptedAt],
    )
