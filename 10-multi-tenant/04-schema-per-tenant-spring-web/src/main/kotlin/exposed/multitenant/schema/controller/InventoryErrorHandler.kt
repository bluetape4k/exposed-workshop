package exposed.multitenant.schema.controller

import exposed.multitenant.schema.tenant.TenantSchemaResetFailedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.io.Serializable

@RestControllerAdvice
class InventoryErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validationFailure(): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse("invalid_request"))

    @ExceptionHandler(TenantSchemaResetFailedException::class)
    fun schemaResetFailure(): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse("schema_reset_failed"))
}

data class ErrorResponse(
    val error: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
