package alternatives.hibernate.reactive.example.domain.dto

import alternatives.hibernate.reactive.example.domain.mapper.toRecord
import alternatives.hibernate.reactive.example.domain.model.Member
import java.io.Serializable

data class MemberRecord(
    val id: Long,
    val name: String,
    val age: Int? = 0,
) : Serializable {
    /**
     * 이렇게도 가능하지만, 성능 상 좋은 점이 없다
     *
     * 또한, Record 모듈에 Entity 관련 기능이 포함되어 버리기 때문에 추천하지 않는다
     */
    constructor(member: Member) : this(member.id, member.name, member.age)

    fun withId(id: Long) = copy(id = id)
}

data class MemberAndTeamRecord(
    val id: Long,
    val name: String,
    val age: Int? = 0,
    val team: TeamRecord?,
) : Serializable {
    constructor(member: Member) : this(member.id, member.name, member.age, member.team?.toRecord())
}
