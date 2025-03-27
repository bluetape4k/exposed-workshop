package alternatives.hibernate.reactive.example.domain.dto

import alternatives.hibernate.reactive.example.domain.mapper.toDto
import alternatives.hibernate.reactive.example.domain.model.Member
import java.io.Serializable

data class MemberDTO(
    val id: Long,
    val name: String,
    val age: Int? = 0,
): Serializable {

    /**
     * 이렇게도 가능하지만, 성능 상 좋은 점이 없다
     *
     * 또한, DTO 모듈에 Entity 관련 기능이 포함되어 버리기 때문에 추천하지 않는다
     */
    constructor(member: Member): this(member.id, member.name, member.age)
}

data class MemberAndTeamDTO(
    val id: Long,
    val name: String,
    val age: Int? = 0,
    val team: TeamDTO,
): Serializable {
    constructor(member: Member): this(member.id, member.name, member.age, member.team!!.toDto())
}
