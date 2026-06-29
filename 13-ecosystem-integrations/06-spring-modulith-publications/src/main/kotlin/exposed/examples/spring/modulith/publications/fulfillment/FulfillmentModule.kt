package exposed.examples.spring.modulith.publications.fulfillment

import exposed.examples.spring.modulith.publications.orders.OrderApprovedEvent
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

object FulfillmentReservations: LongIdTable("modulith_fulfillment_reservations") {
    val orderKey = varchar("order_key", 80).uniqueIndex()
    val customerId = varchar("customer_id", 80)
    val reservedAt = timestamp("reserved_at")
}

data class FulfillmentReservation(
    val id: Long,
    val orderKey: String,
    val customerId: String,
    val reservedAt: Instant,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Service
class FulfillmentReservationHandler(
    private val transactionTemplate: TransactionTemplate,
) {
    private val failNextReservation = AtomicBoolean(false)

    fun failNextReservation() {
        failNextReservation.set(true)
    }

    @ApplicationModuleListener(id = "fulfillment.reserve-stock")
    fun reserveStock(event: OrderApprovedEvent) {
        if (failNextReservation.compareAndSet(true, false)) {
            error("Simulated downstream reservation failure for ${event.orderKey}")
        }

        transactionTemplate.executeWithoutResult {
            FulfillmentReservations.insert { row ->
                row[orderKey] = event.orderKey
                row[customerId] = event.customerId
                row[reservedAt] = Instant.now()
            }
        }
    }

    fun findReservation(orderKey: String): FulfillmentReservation? =
        transactionTemplate.execute {
            FulfillmentReservations
                .selectAll()
                .where { FulfillmentReservations.orderKey eq orderKey }
                .firstOrNull()
                ?.toFulfillmentReservation()
        }
}

private fun ResultRow.toFulfillmentReservation(): FulfillmentReservation =
    FulfillmentReservation(
        id = this[FulfillmentReservations.id].value,
        orderKey = this[FulfillmentReservations.orderKey],
        customerId = this[FulfillmentReservations.customerId],
        reservedAt = this[FulfillmentReservations.reservedAt],
    )
