package exposed.examples.springmvc.domain.model

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.LocalDate

/**
 * 영화 도메인의 Exposed 스키마 정의.
 *
 * 영화(Movie), 배우(Actor), 영화-배우 연관 테이블 및 해당 DAO 엔티티 클래스를 포함합니다.
 */
object MovieSchema {

    /**
     * 영화 정보를 저장하는 테이블.
     *
     * `movies` 테이블에 매핑되며, 영화 이름, 제작자, 개봉일을 관리합니다.
     */
    object MovieTable: LongIdTable("movies") {
        val name: Column<String> = varchar("name", 255)
        val producerName: Column<String> = varchar("producer_name", 255)
        val releaseDate: Column<LocalDate> = date("release_date")
    }

    /**
     * 배우 정보를 저장하는 테이블.
     *
     * `actors` 테이블에 매핑되며, 배우의 이름과 생년월일을 관리합니다.
     */
    object ActorTable: LongIdTable("actors") {
        val firstName: Column<String> = varchar("first_name", 255)
        val lastName: Column<String> = varchar("last_name", 255)
        val birthday: Column<LocalDate?> = date("birthday").nullable()
    }

    /**
     * 영화와 배우의 다대다 연관 관계를 저장하는 테이블.
     *
     * `actors_in_movies` 테이블에 매핑되며, 영화 삭제 시 연관 데이터도 함께 삭제(CASCADE)됩니다.
     */
    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = CASCADE)

        override val primaryKey = PrimaryKey(movieId, actorId)
    }

    /**
     * 영화 DAO 엔티티.
     *
     * [MovieTable]에 매핑되며, 출연 배우 목록을 [ActorInMovieTable]을 통해 참조합니다.
     *
     * @param id 엔티티 식별자
     */
    class MovieEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<MovieEntity>(MovieTable)

        var name: String by MovieTable.name
        var producerName: String by MovieTable.producerName
        var releaseDate: LocalDate by MovieTable.releaseDate

        val actors: SizedIterable<ActorEntity> by ActorEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("producerName", producerName)
            .add("releaseDate", releaseDate)
            .toString()
    }

    /**
     * 배우 DAO 엔티티.
     *
     * [ActorTable]에 매핑되며, 출연 영화 목록을 [ActorInMovieTable]을 통해 참조합니다.
     *
     * @param id 엔티티 식별자
     */
    class ActorEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<ActorEntity>(ActorTable)

        var firstName: String by ActorTable.firstName
        var lastName: String by ActorTable.lastName
        var birthday: LocalDate? by ActorTable.birthday

        val movies: SizedIterable<MovieEntity> by MovieEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("birthday", birthday)
            .toString()
    }
}
