package exposed.examples.spring.modulith.boundaries.invalid.orders.internal

class LeakyOrderRepository {
    fun exists(orderKey: String): Boolean =
        orderKey.isNotBlank()
}
