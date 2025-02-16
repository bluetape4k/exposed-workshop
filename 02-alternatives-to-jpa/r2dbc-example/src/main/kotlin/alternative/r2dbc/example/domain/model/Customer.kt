package alternative.r2dbc.example.domain.model

import org.springframework.data.annotation.Id
import java.io.Serializable

data class Customer(
    val firstname: String,
    val lastname: String,
    @Id
    var id: Long? = null,
): Serializable {
    val hasId: Boolean get() = id != null
}
