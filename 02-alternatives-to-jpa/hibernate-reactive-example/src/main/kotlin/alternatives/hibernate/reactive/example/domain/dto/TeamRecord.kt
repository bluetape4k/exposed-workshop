package alternatives.hibernate.reactive.example.domain.dto

import java.io.Serializable

data class TeamRecord(
    val id: Long,
    val name: String,
): Serializable {
    fun withId(id: Long) = copy(id = id)
}
