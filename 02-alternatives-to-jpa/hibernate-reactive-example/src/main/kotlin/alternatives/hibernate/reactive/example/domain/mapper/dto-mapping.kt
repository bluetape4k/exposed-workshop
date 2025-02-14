package alternatives.hibernate.reactive.example.domain.mapper

import alternatives.hibernate.reactive.example.domain.dto.MemberDTO
import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberDTO
import alternatives.hibernate.reactive.example.domain.dto.TeamDTO
import alternatives.hibernate.reactive.example.domain.model.Member
import alternatives.hibernate.reactive.example.domain.model.Team

fun Member.toDto() = MemberDTO(
    id = this.id!!,
    name = this.name,
    age = this.age
)

fun Team.toDto() = TeamDTO(
    id = this.id!!,
    name = this.name
)

fun Team.toTeamAndMemberDTO() = TeamAndMemberDTO(
    teamId = this.id!!,
    teamName = this.name,
    members = this.members.map { it.toDto() }
)
