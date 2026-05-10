package alternative.r2dbc.example.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable

@Table("customer")
data class Customer(
    @Column("firstname")
    val firstname: String,
    @Column("lastname")
    val lastname: String,
    @Id
    @Column("id")
    var id: Long? = null,
): Serializable {
    val hasId: Boolean get() = id != null
}
