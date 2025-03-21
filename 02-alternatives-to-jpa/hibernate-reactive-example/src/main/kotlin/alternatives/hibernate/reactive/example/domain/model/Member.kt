package alternatives.hibernate.reactive.example.domain.model

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.requireNotEmpty
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

fun memberOf(name: String, age: Int? = null, team: Team? = null): Member {
    name.requireNotEmpty("name")
    return Member().apply {
        this.name = name
        this.age = age
        this.team = team
        team?.addMember(this)
    }
}

/**
 * 팀 구성원 정보
 *
 * ```sql
 * create table Member (
 *     age integer,
 *     id bigint generated by default as identity,
 *     team_id bigint not null,
 *     name varchar(255),
 *     primary key (id)
 * );
 * ```
 * ```sql
 * alter table if exists Member
 *    add constraint FK5nt1mnqvskefwe0nj9yjm4eav
 *    foreign key (team_id)
 *    references Team;
 * ```
 */
@Entity
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Member: AbstractValueObject() {

    companion object {
        /**
         * HINT: java static constructor 와 유사한 방식입니다.
         *
         * package method인 `memberOf` 를 사용하는 방법도 추천합니다.
         */
        operator fun invoke(name: String, age: Int? = null, team: Team? = null): Member {
            name.requireNotEmpty("name")

            return Member().apply {
                this.name = name
                this.age = age
                this.team = team
                team?.addMember(this)
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    var name: String = ""
    var age: Int? = null

    /**
     * NOTE: lazy fetch를 하기 위해서는 `kotlin("plugin.jpa")` 를 지정하고, allOpen task 에 @Entity 등의 annotation을 등록해야 합니다.
     *
     * NOTE: ManyToOne 의 FetchType을 LAZY 로 하면 Thread 범위를 벗어나 예외가 발생한다.
     * NOTE: 이럴 땐 LEFT JOIN FETCH 를 수행하던가 @FetchProfile 을 사용해야 한다
     */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    var team: Team? = null

    fun changeTeam(team: Team?) {
        this.team?.removeMember(this)
        team?.addMember(this)
    }

    override fun equalProperties(other: Any): Boolean {
        return other is Member &&
                name == other.name &&
                age == other.age
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: name.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
            .add("name", name)
            .add("age", age)
    }
}
