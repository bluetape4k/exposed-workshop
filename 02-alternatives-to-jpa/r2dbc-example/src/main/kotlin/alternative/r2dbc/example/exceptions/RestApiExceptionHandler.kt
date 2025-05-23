package alternative.r2dbc.example.exceptions

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange

@RestControllerAdvice
class RestApiExceptionHandler(private val applicationEventPublisher: ApplicationEventPublisher) {

    companion object: KLoggingChannel()

    @ExceptionHandler(PostNotFoundException::class)
    suspend fun handle(ex: PostNotFoundException, exchange: ServerWebExchange) {
        log.warn(ex) { "Post[${ex.postId}] not found." }

        exchange.response.statusCode = HttpStatus.NOT_FOUND
        exchange.response.setComplete().awaitSingleOrNull()
    }
}
