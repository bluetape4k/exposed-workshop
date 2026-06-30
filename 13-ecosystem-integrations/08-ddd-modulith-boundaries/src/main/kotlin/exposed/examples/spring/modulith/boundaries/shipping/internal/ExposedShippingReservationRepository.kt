package exposed.examples.spring.modulith.boundaries.shipping.internal

import exposed.examples.spring.modulith.boundaries.orders.events.OrderAcceptedEvent
import exposed.examples.spring.modulith.boundaries.shipping.ShippingReservation
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.Instant

object ShippingReservations: LongIdTable("ddd_modulith_shipping_reservations") {
    val orderKey = varchar("order_key", 80).uniqueIndex()
    val customerId = varchar("customer_id", 80)
    val reservedAt = timestamp("reserved_at")
}

@Repository
class ExposedShippingReservationRepository {

    fun reserve(event: OrderAcceptedEvent) {
        ShippingReservations.insert { row ->
            row[orderKey] = event.orderKey
            row[customerId] = event.customerId
            row[reservedAt] = Instant.now()
        }
    }

    fun findByOrderKey(orderKey: String): ShippingReservation? =
        ShippingReservations
            .selectAll()
            .where { ShippingReservations.orderKey eq orderKey }
            .firstOrNull()
            ?.toShippingReservation()
}

private fun ResultRow.toShippingReservation(): ShippingReservation =
    ShippingReservation(
        id = this[ShippingReservations.id].value,
        orderKey = this[ShippingReservations.orderKey],
        customerId = this[ShippingReservations.customerId],
        reservedAt = this[ShippingReservations.reservedAt],
    )
