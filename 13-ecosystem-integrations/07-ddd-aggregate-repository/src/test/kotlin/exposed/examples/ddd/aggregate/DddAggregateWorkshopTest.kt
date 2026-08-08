package exposed.examples.ddd.aggregate

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldHaveSize
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class DddAggregateWorkshopTest {

    private val db: Database = Database.connect(
        url = "jdbc:h2:mem:ddd-aggregate-workshop;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        driver = "org.h2.Driver",
    )

    private val repository = OrderRepository(db)

    @BeforeEach
    fun setUp() {
        repository.bootstrapSchema()
    }

    @Test
    fun `aggregate rejects invalid commands before Exposed tables are involved`() {
        assertFailsWith<IllegalArgumentException> {
            PlaceOrderCommand(
                orderNumber = OrderNumber("ORD-1000"),
                customerId = CustomerId("customer-1"),
                lines = emptyList(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OrderLine(
                sku = Sku("book"),
                quantity = 0,
                unitPrice = Money.dollars("15.00"),
            )
        }
    }

    @Test
    fun `repository persists aggregate state and records the initial domain event`() {
        val order = PurchaseOrder.place(newOrderCommand(), occurredAtEpochMillis = 1_000L)

        val persisted = repository.save(order)

        persisted.status shouldBeEqualTo OrderStatus.PLACED
        persisted.version shouldBeEqualTo 0L
        persisted.lineCount shouldBeEqualTo 2
        persisted.total shouldBeEqualTo Money.dollars("65.00")
        order.pendingEvents shouldHaveSize 0

        val events = repository.capturedEvents(persisted.id)
        events shouldHaveSize 1
        events.single().eventType shouldBeEqualTo "OrderPlaced"
        events.single().sequence shouldBeEqualTo 1
        events.single().payload shouldBeEqualTo "customer=customer-1;lines=2;total=65.00"
    }

    @Test
    fun `aggregate uses typed id and domain event metadata from bluetape core`() {
        val order = PurchaseOrder.place(newOrderCommand(), occurredAtEpochMillis = 1_000L)

        (order.id > 0L) shouldBeEqualTo true
        val event = order.pendingEvents.single()
        event.aggregateId shouldBeEqualTo order.id
        event.occurredAt shouldBeEqualTo Instant.ofEpochMilli(1_000L)
    }

    @Test
    fun `loaded aggregate command appends approval event after the placed event`() {
        val placed = PurchaseOrder.place(newOrderCommand(), occurredAtEpochMillis = 1_000L)
        val placedSnapshot = repository.save(placed)

        val loaded = repository.findAggregate(OrderNumber("ORD-1000"))!!
        loaded.approve(OperatorId("ops-1"), occurredAtEpochMillis = 2_000L)
        val approvedSnapshot = repository.save(loaded)

        approvedSnapshot.id shouldBeEqualTo placedSnapshot.id
        approvedSnapshot.status shouldBeEqualTo OrderStatus.APPROVED
        approvedSnapshot.version shouldBeEqualTo 1L

        val events = repository.capturedEvents(approvedSnapshot.id)
        events.map { it.sequence } shouldBeEqualTo listOf(1, 2)
        events.map { it.eventType } shouldBeEqualTo listOf("OrderPlaced", "OrderApproved")
        events.map { it.payload } shouldContainAll listOf(
            "customer=customer-1;lines=2;total=65.00",
            "approvedBy=ops-1;total=65.00",
        )
    }

    @Test
    fun `repository rollback leaves aggregate rows and event rows uncommitted`() {
        val order = PurchaseOrder.place(newOrderCommand(), occurredAtEpochMillis = 1_000L)

        assertFailsWith<IllegalStateException> {
            repository.save(order) {
                error("simulate failure after domain event insert")
            }
        }

        repository.orderCount() shouldBeEqualTo 0L
        repository.eventCount() shouldBeEqualTo 0L
        order.pendingEvents shouldHaveSize 1
    }

    @Test
    fun `aggregate prevents an approval command from running twice`() {
        val placed = PurchaseOrder.place(newOrderCommand(), occurredAtEpochMillis = 1_000L)
        repository.save(placed)
        val loaded = repository.findAggregate(OrderNumber("ORD-1000"))!!

        loaded.approve(OperatorId("ops-1"), occurredAtEpochMillis = 2_000L)
        assertFailsWith<IllegalStateException> {
            loaded.approve(OperatorId("ops-2"), occurredAtEpochMillis = 3_000L)
        }
    }

    private fun newOrderCommand(): PlaceOrderCommand =
        PlaceOrderCommand(
            orderNumber = OrderNumber("ORD-1000"),
            customerId = CustomerId("customer-1"),
            lines = listOf(
                OrderLine(
                    sku = Sku("book"),
                    quantity = 2,
                    unitPrice = Money.dollars("15.00"),
                ),
                OrderLine(
                    sku = Sku("course"),
                    quantity = 1,
                    unitPrice = Money.dollars("35.00"),
                ),
            ),
        )
}
