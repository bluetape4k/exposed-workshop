package alternatives.hibernate.reactive.example.domain.dto

data class TeamAndMemberRecord(
    val teamId: Long,
    val teamName: String,
    val members: List<MemberRecord>,
)
