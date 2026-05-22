package exposed.examples.spring.observability.web

import exposed.examples.spring.observability.config.REQUEST_ID_ATTRIBUTE
import exposed.examples.spring.observability.model.ErrorResponse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.ErrorResponse as SpringErrorResponse
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
internal class DiagnosticErrorHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidation(
        request: HttpServletRequest,
        cause: IllegalArgumentException,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(
            ErrorResponse(
                code = "VALIDATION_FAILED",
                message = cause.message ?: "Validation failed",
                requestId = request.requestId(),
            )
        )

    @ExceptionHandler(
        HandlerMethodValidationException::class,
        HttpMessageConversionException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
    )
    fun handleBadRequest(
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(
            ErrorResponse(
                code = "BAD_REQUEST",
                message = "Malformed request",
                requestId = request.requestId(),
            )
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        request: HttpServletRequest,
        cause: Exception,
    ): ResponseEntity<ErrorResponse> {
        if (cause is SpringErrorResponse) {
            return ResponseEntity.status(cause.statusCode).body(
                ErrorResponse(
                    code = "HTTP_${cause.statusCode.value()}",
                    message = cause.body.detail ?: "Request failed",
                    requestId = request.requestId(),
                )
            )
        }

        val requestId = request.requestId()
        log.error(cause) { "Unhandled failure in Spring observability example" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "Internal server error",
                requestId = requestId,
            )
        )
    }

    private fun HttpServletRequest.requestId(): String =
        getAttribute(REQUEST_ID_ATTRIBUTE)?.toString().orEmpty()

    private companion object : KLogging()
}
