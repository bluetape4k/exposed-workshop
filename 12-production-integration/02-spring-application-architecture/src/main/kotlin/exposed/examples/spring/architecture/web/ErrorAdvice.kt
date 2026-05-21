package exposed.examples.spring.architecture.web

import exposed.examples.spring.architecture.model.CustomerValidationException
import exposed.examples.spring.architecture.model.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
internal class ErrorAdvice {

    private val log = LoggerFactory.getLogger(ErrorAdvice::class.java)

    @ExceptionHandler(CustomerValidationException::class, MethodArgumentNotValidException::class)
    fun validation(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(code = "VALIDATION_ERROR", message = ex.message ?: "Invalid request"))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedRequest(): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(code = "MALFORMED_JSON", message = "Request body is not valid JSON"))

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(code = "NOT_FOUND", message = "Resource was not found"))

    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected request handling failure", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(code = "INTERNAL_ERROR", message = "Unexpected server error"))
    }
}
