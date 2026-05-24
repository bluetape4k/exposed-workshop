package exposed.examples.ktor.routingdatasource.model

import exposed.examples.ktor.routingdatasource.routing.DataSourceRole
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class IndexResponse(
    val name: String = "routing-datasource-ktor",
    val overrideHeader: String = "X-Data-Source",
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class HealthResponse(
    val status: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class InventoryResponse(
    val sku: String,
    val quantity: Int,
    val selectedDataSource: DataSourceRole,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class UpdateInventoryRequest(
    val quantity: Int,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class RoutingStatsResponse(
    val readSelections: Int,
    val writeSelections: Int,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
