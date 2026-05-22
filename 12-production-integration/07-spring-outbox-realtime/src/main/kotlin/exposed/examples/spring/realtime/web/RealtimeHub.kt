package exposed.examples.spring.realtime.web

import exposed.examples.spring.realtime.model.RealtimeEvent
import exposed.examples.spring.realtime.service.RealtimeDelivery
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

internal class RealtimeHub : RealtimeDelivery {

    private val sink = Sinks.many().replay().limit<RealtimeEvent>(MAX_REPLAY_BUFFER)

    override fun deliver(event: RealtimeEvent): Boolean =
        sink.tryEmitNext(event).isSuccess

    fun live(): Flux<RealtimeEvent> =
        sink.asFlux()

    private companion object {
        private const val MAX_REPLAY_BUFFER = 100
    }
}
