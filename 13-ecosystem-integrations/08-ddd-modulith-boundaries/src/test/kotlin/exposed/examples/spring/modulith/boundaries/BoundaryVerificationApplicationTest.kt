package exposed.examples.spring.modulith.boundaries

import com.tngtech.archunit.core.importer.ImportOption
import exposed.examples.spring.modulith.boundaries.invalid.InvalidBoundaryApplication
import exposed.examples.spring.modulith.boundaries.orders.AcceptOrderCommand
import exposed.examples.spring.modulith.boundaries.orders.OrderApplicationService
import exposed.examples.spring.modulith.boundaries.orders.internal.WorkshopOrders
import exposed.examples.spring.modulith.boundaries.shipping.ShippingReservationHandler
import exposed.examples.spring.modulith.boundaries.shipping.internal.ShippingReservations
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import org.awaitility.kotlin.await
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.core.Violations
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration

@SpringBootTest(
    classes = [BoundaryVerificationApplication::class],
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ddd-modulith-boundaries-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoundaryVerificationApplicationTest @Autowired constructor(
    private val orderService: OrderApplicationService,
    private val shippingHandler: ShippingReservationHandler,
    private val transactionTemplate: TransactionTemplate,
) {

    @BeforeEach
    fun resetTables() {
        transactionTemplate.executeWithoutResult {
            SchemaUtils.create(WorkshopOrders, ShippingReservations)
            ShippingReservations.deleteAll()
            WorkshopOrders.deleteAll()
        }
    }

    @Test
    fun `application modules allow shipping to depend only on order events`() {
        ApplicationModules.of(BoundaryVerificationApplication::class.java).verify()
    }

    @Test
    fun `boundary verifier rejects shipping dependency on order internals`() {
        val violations = assertFailsWith<Violations> {
            ApplicationModules
                .of(InvalidBoundaryApplication::class.java, ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .verify()
        }

        violations.messages.joinToString("\n") shouldContain "orders"
        violations.messages.joinToString("\n") shouldContain "internal"
    }

    @Test
    fun `domain event hands off order acceptance to shipping context`() {
        val order = orderService.accept(
            AcceptOrderCommand(orderKey = "order-ddd-001", customerId = "customer-42")
        )

        order.status.shouldBeEqualTo("ACCEPTED")

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val reservation = shippingHandler.findReservation(order.orderKey).shouldNotBeNull()

            reservation.orderKey.shouldBeEqualTo(order.orderKey)
            reservation.customerId.shouldBeEqualTo(order.customerId)
        }
    }
}
