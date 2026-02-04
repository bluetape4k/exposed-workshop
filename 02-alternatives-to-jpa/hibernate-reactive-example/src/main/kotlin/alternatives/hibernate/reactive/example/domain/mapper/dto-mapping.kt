package alternatives.hibernate.reactive.example.domain.mapper

import alternatives.hibernate.reactive.example.domain.dto.MemberAndTeamRecord
import alternatives.hibernate.reactive.example.domain.dto.MemberRecord
import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberRecord
import alternatives.hibernate.reactive.example.domain.dto.TeamRecord
import alternatives.hibernate.reactive.example.domain.model.Member
import alternatives.hibernate.reactive.example.domain.model.Team

fun Member.toRecord() = MemberRecord(this)
fun Member.toMemberAndTeamRecord() = MemberAndTeamRecord(this)

fun Team.toRecord() = TeamRecord(
    id = this.id,
    name = this.name
)

fun Team.toTeamAndMemberRecord() = TeamAndMemberRecord(
    teamId = this.id,
    teamName = this.name,
    members = this.members.map { it.toRecord() }
)
