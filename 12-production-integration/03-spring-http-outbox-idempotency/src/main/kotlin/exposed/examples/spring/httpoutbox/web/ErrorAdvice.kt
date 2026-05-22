package exposed.examples.spring.httpoutbox.web

import exposed.examples.spring.httpoutbox.model.ErrorResponse
import exposed.examples.spring.httpoutbox.model.PaymentValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
internal class ErrorAdvice {

    @ExceptionHandler(PaymentValidationException::class)
    fun validationError(error: PaymentValidationException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_ERROR", error.message ?: "Request validation failed"))

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(error: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NOT_FOUND", error.message ?: "Resource was not found"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidArgument(): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_ERROR", "Request validation failed"))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedJson(): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("MALFORMED_JSON", "Request body is not valid JSON"))

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected payment outbox error", error)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "Unexpected server error"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ErrorAdvice::class.java)
    }
}
