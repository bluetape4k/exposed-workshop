package exposed.examples.spring.modulith.boundaries.invalid.shipping

import exposed.examples.spring.modulith.boundaries.invalid.orders.events.OrderAcceptedEvent
import exposed.examples.spring.modulith.boundaries.invalid.orders.internal.LeakyOrderRepository

class ShippingBoundaryLeak(
    private val leakyOrderRepository: LeakyOrderRepository,
) {
    fun reserveAfter(event: OrderAcceptedEvent): Boolean =
        leakyOrderRepository.exists(event.orderKey)
}
