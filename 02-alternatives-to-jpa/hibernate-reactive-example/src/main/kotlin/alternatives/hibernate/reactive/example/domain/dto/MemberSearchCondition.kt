package alternatives.hibernate.reactive.example.domain.dto

data class MemberSearchCondition(
    val memberName: String? = null,
    val teamName: String? = null,
    val ageGoe: Int? = null,
    val ageLoe: Int? = null,
)
