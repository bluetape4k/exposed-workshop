package exposed.examples.ktor.architecture.model

import kotlinx.serialization.Serializable

@Serializable
internal data class IndexResponse(
    val service: String = "ktor-application-architecture",
    val endpoints: List<String> = listOf("/health", "/customers", "/customers/{id}"),
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class HealthResponse(
    val status: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class CreateCustomerRequest(
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CreateCustomerCommand(
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CustomerRecord(
    val id: Long,
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class CustomerResponse(
    val id: Long,
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class CustomersResponse(
    val customers: List<CustomerResponse>,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class ErrorResponse(
    val code: String,
    val message: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal class CustomerValidationException(message: String) : RuntimeException(message)

internal fun CustomerRecord.toResponse(): CustomerResponse =
    CustomerResponse(id = id, name = name, email = email)
