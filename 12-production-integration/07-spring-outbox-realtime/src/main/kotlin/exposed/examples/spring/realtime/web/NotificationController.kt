package exposed.examples.spring.realtime.web

import exposed.examples.spring.realtime.model.CreateNotificationRequest
import exposed.examples.spring.realtime.model.NotificationResponse
import exposed.examples.spring.realtime.model.OutboxResponse
import exposed.examples.spring.realtime.model.PublishResponse
import exposed.examples.spring.realtime.model.RealtimeEvent
import exposed.examples.spring.realtime.service.NotificationService
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
internal class NotificationController(
    private val service: NotificationService,
    private val realtimeHub: RealtimeHub,
) {

    @PostMapping("/notifications")
    fun create(@RequestBody request: CreateNotificationRequest): NotificationResponse =
        service.create(request)

    @PostMapping("/outbox/publish")
    fun publish(): PublishResponse =
        service.publishPending()

    @GetMapping("/outbox")
    fun outbox(): OutboxResponse =
        service.outbox()

    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(@RequestParam(defaultValue = "0") after: Long): Flux<ServerSentEvent<RealtimeEvent>> {
        val replay = Flux.fromIterable(service.replayAfter(after))
        return Flux.concat(replay, realtimeHub.live())
            .filter { event -> event.id > after }
            .distinct { event -> event.id }
            .map { event ->
                ServerSentEvent.builder(event)
                    .id(event.id.toString())
                    .event(event.type)
                    .build()
            }
    }
}
