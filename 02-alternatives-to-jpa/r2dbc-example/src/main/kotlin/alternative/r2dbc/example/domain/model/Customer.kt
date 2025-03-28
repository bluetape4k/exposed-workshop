package alternative.r2dbc.example.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable

@Table("customer")
data class Customer(
    val firstname: String,
    val lastname: String,
    @Id
    var id: Long? = null,
): Serializable {
    val hasId: Boolean get() = id != null
}
