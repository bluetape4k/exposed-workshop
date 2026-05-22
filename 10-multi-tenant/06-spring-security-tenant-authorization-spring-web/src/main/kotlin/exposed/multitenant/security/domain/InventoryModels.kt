package exposed.multitenant.security.domain

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.io.Serializable

data class InventoryItemRecord(
    val sku: String,
    val name: String,
    val quantity: Int,
    val warehouse: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class CreateInventoryItemRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val sku: String,

    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:Min(0)
    val quantity: Int,

    @field:NotBlank
    @field:Size(max = 80)
    val warehouse: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
