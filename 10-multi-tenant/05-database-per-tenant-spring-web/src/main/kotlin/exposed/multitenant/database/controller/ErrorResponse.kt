package exposed.multitenant.database.controller

import java.io.Serializable

data class ErrorResponse(
    val code: String,
    val message: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
