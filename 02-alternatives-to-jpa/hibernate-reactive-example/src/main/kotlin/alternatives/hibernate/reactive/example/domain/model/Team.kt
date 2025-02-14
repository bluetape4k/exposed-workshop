package alternatives.hibernate.reactive.example.domain.model

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.requireNotEmpty
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

fun teamOf(name: String): Team {
    name.requireNotEmpty("name")

    return Team().apply {
        this.name = name
    }
}

@Entity
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Team: AbstractValueObject() {

    companion object {
        /**
         * HINT: java static constructor 와 유사한 방식입니다.
         *
         * package method인 `teamOf` 를 사용하는 방법도 추천합니다.
         */
        operator fun invoke(name: String): Team {
            name.requireNotEmpty("name")

            return Team().apply {
                this.name = name
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

    var name: String = ""

    @OneToMany(mappedBy = "team", orphanRemoval = false)
    val members: MutableList<Member> = mutableListOf()

    fun addMember(member: Member) {
        if (members.add(member)) {
            member.team = this
        }
    }

    fun removeMember(member: Member) {
        if (members.remove(member)) {
            member.team = null
        }
    }

    override fun equalProperties(other: Any): Boolean =
        other is Team && name == other.name

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)
    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
            .add("name", name)
    }
}
