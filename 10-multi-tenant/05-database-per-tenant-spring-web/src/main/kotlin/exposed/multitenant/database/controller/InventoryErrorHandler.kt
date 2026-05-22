package exposed.multitenant.database.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class InventoryErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validationFailure(): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_FAILED", "Request body validation failed"))

    @ExceptionHandler(IllegalStateException::class)
    fun illegalState(): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("TENANT_CONTEXT_ERROR", "Tenant context error"))
}
