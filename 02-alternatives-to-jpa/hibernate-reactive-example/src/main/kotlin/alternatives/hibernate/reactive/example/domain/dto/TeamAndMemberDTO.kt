package alternatives.hibernate.reactive.example.domain.dto

data class TeamAndMemberDTO(
    val teamId: Long,
    val teamName: String,
    val members: List<MemberDTO>,
)
