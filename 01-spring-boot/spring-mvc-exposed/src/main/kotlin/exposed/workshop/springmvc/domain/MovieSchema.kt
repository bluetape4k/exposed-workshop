package exposed.workshop.springmvc.domain

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.LocalDate
import java.time.LocalDateTime

object MovieSchema {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS movies (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      producer_name VARCHAR(255) NOT NULL,
     *      release_date TIMESTAMP NOT NULL
     * )
     * ```
     */
    object MovieTable: LongIdTable("movies") {
        val name: Column<String> = varchar("name", 255)
        val producerName: Column<String> = varchar("producer_name", 255)
        val releaseDate: Column<LocalDateTime> = datetime("release_date")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS actors (
     *      id BIGSERIAL PRIMARY KEY,
     *      first_name VARCHAR(255) NOT NULL,
     *      last_name VARCHAR(255) NOT NULL,
     *      birthday DATE NULL
     * )
     * ```
     */
    object ActorTable: LongIdTable("actors") {
        val firstName: Column<String> = varchar("first_name", 255)
        val lastName: Column<String> = varchar("last_name", 255)
        val birthday: Column<LocalDate?> = date("birthday").nullable()
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS actors_in_movies (
     *      movie_id BIGINT,
     *      actor_id BIGINT,
     *
     *      CONSTRAINT pk_actors_in_movies PRIMARY KEY (movie_id, actor_id),
     *      CONSTRAINT fk_actors_in_movies_movie_id__id FOREIGN KEY (movie_id)
     *          REFERENCES movies(id) ON DELETE CASCADE ON UPDATE RESTRICT,
     *      CONSTRAINT fk_actors_in_movies_actor_id__id FOREIGN KEY (actor_id)
     *          REFERENCES actors(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * )
     * ```
     */
    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = ReferenceOption.CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = ReferenceOption.CASCADE)

        override val primaryKey = PrimaryKey(movieId, actorId)
    }

    class MovieEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<MovieEntity>(MovieTable)

        var name: String by MovieTable.name
        var producerName: String by MovieTable.producerName
        var releaseDate: LocalDateTime by MovieTable.releaseDate

        val actors: SizedIterable<ActorEntity> by ActorEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("producerName", producerName)
            .add("releaseDate", releaseDate)
            .toString()
    }

    class ActorEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<ActorEntity>(ActorTable)

        var firstName: String by ActorTable.firstName
        var lastName: String by ActorTable.lastName
        var birthday: LocalDate? by ActorTable.birthday

        val movies: SizedIterable<MovieEntity> by MovieEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("birthday", birthday)
            .toString()
    }
}
