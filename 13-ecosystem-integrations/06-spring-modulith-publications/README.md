# Spring Modulith Publication Store with Exposed

English | [한국어](README.ko.md)

This example shows how a Spring Modulith application can persist event
publications through `bluetape4k-exposed-spring-modulith` instead of using the
official JDBC store. The module stays local-first: Spring Boot runs against
in-memory H2, Exposed creates the workshop tables, and the tests drive the
publication lifecycle without Docker or external services.

![Spring Modulith publication store with Exposed](../../docs/images/readme-diagrams/06-spring-modulith-publications-flow-01.png)

The diagram separates the normal application event path from the publication
store and recovery path:

- the orders module publishes `OrderApprovedEvent` inside a Spring transaction.
- Spring Modulith stores a publication row for the `@ApplicationModuleListener`.
- `ExposedEventPublicationRepository` writes status, listener id, attempts, and
  serialized event data to `EVENT_PUBLICATION`.
- the fulfillment listener marks the publication completed when stock
  reservation succeeds.
- failed rows stay queryable and can be resubmitted through
  `IncompleteEventPublications`.

## Purpose

Spring Modulith publications are a local durability layer for module-to-module
application events. They are useful when one module emits an event and another
module handles it asynchronously after the original transaction commits. The
publication table records which listener still needs the event.

This is not a replacement for every outbox pattern:

| Use Spring Modulith publications when | Use an outbox when |
|---|---|
| the consumer is another module in the same Spring Boot application. | another process, service, broker, or language runtime must consume the event. |
| listener retry can happen inside the same application deployment. | events must be delivered to Kafka, RabbitMQ, SNS/SQS, or another external channel. |
| the main risk is losing local async listener work after commit. | the main risk is cross-service delivery, backpressure, or integration replay. |

## Application Flow

The orders module writes a local order and publishes a serializable event in the
same transaction:

```kotlin
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
```

The fulfillment module listens through Spring Modulith:

```kotlin
@ApplicationModuleListener(id = "fulfillment.reserve-stock")
fun reserveStock(event: OrderApprovedEvent) {
    FulfillmentReservations.insert { row ->
        row[orderKey] = event.orderKey
        row[customerId] = event.customerId
        row[reservedAt] = Instant.now()
    }
}
```

`@ApplicationModuleListener` is a transactional, asynchronous listener. Spring
Modulith captures the publication before the listener runs, then marks the row
completed only after the listener finishes successfully.

## Exposed Publication Store

The module enables the bluetape4k Exposed-backed repository with local
configuration:

```yaml
bluetape4k:
  spring:
    modulith:
      exposed:
        initialize-schema: true
        completion-mode: UPDATE
```

The repository implements Spring Modulith's `EventPublicationRepository` SPI and
uses the application `springTransactionManager`. The table shape follows Spring
Modulith JDBC schema v2, so rows contain listener id, event type, serialized
payload, status, completion attempts, publication date, and completion date.

## Recovery Paths

The tests cover three operational cases:

- a successful listener writes a fulfillment reservation and leaves no
  incomplete publication.
- a simulated listener failure leaves a `FAILED` publication row; calling
  `IncompleteEventPublications.resubmitIncompletePublications(...)` replays the
  event and completes the row.
- a legacy row with a missing event class stays visible for inspection, but
  accessing `publication.event` raises `UnloadableEventPublicationException`.

## Run

```bash
./gradlew :06-spring-modulith-publications:test
```

Expected result: tests run against in-memory H2, publish Spring Modulith events,
verify completion and retry state through the Exposed repository, and do not
contact external services.

## Tested Behavior

The test suite verifies that:

- Spring Modulith creates a durable publication for the fulfillment listener.
- successful listener execution completes the publication row.
- failed listener execution leaves a queryable failed publication.
- resubmission retries the failed publication and increments completion
  attempts.
- unloadable event classes are surfaced as explicit operational errors only
  when the stored event is materialized.
