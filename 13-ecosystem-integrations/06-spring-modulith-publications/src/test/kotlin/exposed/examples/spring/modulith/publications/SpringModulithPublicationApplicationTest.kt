@file:OptIn(ExperimentalUuidApi::class)

package exposed.examples.spring.modulith.publications

import exposed.examples.spring.modulith.publications.fulfillment.FulfillmentReservationHandler
import exposed.examples.spring.modulith.publications.fulfillment.FulfillmentReservations
import exposed.examples.spring.modulith.publications.orders.ApproveOrderCommand
import exposed.examples.spring.modulith.publications.orders.OrderApprovedEvent
import exposed.examples.spring.modulith.publications.orders.OrderApplicationService
import exposed.examples.spring.modulith.publications.orders.OrderSummary
import exposed.examples.spring.modulith.publications.orders.WorkshopOrders
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.spring.modulith.exposed.ExposedEventPublicationTable
import io.bluetape4k.spring.modulith.exposed.UnloadableEventPublicationException
import org.awaitility.kotlin.await
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.modulith.events.EventPublication.Status
import org.springframework.modulith.events.IncompleteEventPublications
import org.springframework.modulith.events.core.EventPublicationRepository
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@SpringBootTest(
    classes = [SpringModulithPublicationApplication::class],
    properties = [
        "spring.datasource.url=jdbc:h2:mem:spring-modulith-publications-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.modulith.events.republish-outstanding-events-on-restart=false",
        "bluetape4k.spring.modulith.exposed.initialize-schema=true",
        "bluetape4k.spring.modulith.exposed.completion-mode=UPDATE",
    ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringModulithPublicationApplicationTest @Autowired constructor(
    private val orderService: OrderApplicationService,
    private val fulfillmentHandler: FulfillmentReservationHandler,
    private val transactionTemplate: TransactionTemplate,
    private val repository: EventPublicationRepository,
    private val incompletePublications: IncompleteEventPublications,
    private val eventPublicationTable: ExposedEventPublicationTable,
) {

    @BeforeEach
    fun resetTables() {
        transactionTemplate.executeWithoutResult {
            SchemaUtils.create(WorkshopOrders, FulfillmentReservations, eventPublicationTable)
            FulfillmentReservations.deleteAll()
            WorkshopOrders.deleteAll()
            eventPublicationTable.deleteAll()
        }
    }

    @Test
    fun `application module listener completes publication after order approval`() {
        val order = orderService.approve(
            ApproveOrderCommand(orderKey = "order-complete", customerId = "customer-a")
        )

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            fulfillmentHandler.findReservation(order.orderKey).shouldNotBeNull()
            repository.findIncompletePublications().size.shouldBeEqualTo(0)

            val completed = repository.findByStatus(Status.COMPLETED).single()
            completed.event.shouldBeEqualTo(
                order.toApprovedEventPayload()
            )
            completed.targetIdentifier.value.shouldBeEqualTo("fulfillment.reserve-stock")
        }
    }

    @Test
    fun `failed publication can be resubmitted through Spring Modulith API`() {
        fulfillmentHandler.failNextReservation()
        val order = orderService.approve(
            ApproveOrderCommand(orderKey = "order-retry", customerId = "customer-b")
        )

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            fulfillmentHandler.findReservation(order.orderKey).shouldBeEqualTo(null)

            val failed = repository.findByStatus(Status.FAILED).single()
            failed.targetIdentifier.value.shouldBeEqualTo("fulfillment.reserve-stock")
        }

        incompletePublications.resubmitIncompletePublications { true }

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            fulfillmentHandler.findReservation(order.orderKey).shouldNotBeNull()
            repository.findIncompletePublications().size.shouldBeEqualTo(0)

            val completed = repository.findByStatus(Status.COMPLETED).single()
            completed.completionAttempts.shouldBeEqualTo(2)
        }
    }

    @Test
    fun `unloadable publication rows stay inspectable until event access`() {
        val publicationId = UUID.randomUUID()
        val missingEventType = "example.missing.LegacyOrderApprovedEvent"

        transactionTemplate.executeWithoutResult {
            eventPublicationTable.insert { row ->
                row[id] = Uuid.parse(publicationId.toString())
                row[listenerId] = "fulfillment.reserve-stock"
                row[eventType] = missingEventType
                row[serializedEvent] = """{"orderKey":"legacy","customerId":"customer-c","approvedAt":"2026-06-29T00:00:00Z"}"""
                row[publicationDate] = Instant.parse("2026-06-29T00:00:00Z")
                row[completionDate] = null
                row[status] = Status.FAILED.name
                row[completionAttempts] = 1
                row[lastResubmissionDate] = Instant.parse("2026-06-29T00:00:00Z")
            }
        }

        val failed = repository.findByStatus(Status.FAILED).single()
        failed.identifier.shouldBeEqualTo(publicationId)
        failed.targetIdentifier.value.shouldBeEqualTo("fulfillment.reserve-stock")

        assertFailsWith<UnloadableEventPublicationException> {
            failed.event
        }
    }

    private fun OrderSummary.toApprovedEventPayload() =
        OrderApprovedEvent(
            orderKey = orderKey,
            customerId = customerId,
            approvedAt = approvedAt.shouldNotBeNull(),
        )
}
