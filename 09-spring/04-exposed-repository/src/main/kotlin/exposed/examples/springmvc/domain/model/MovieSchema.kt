package exposed.examples.springmvc.domain.model

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
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

object MovieSchema {

    object MovieTable: LongIdTable("movies") {
        val name: Column<String> = varchar("name", 255)
        val producerName: Column<String> = varchar("producer_name", 255)
        val releaseDate: Column<LocalDate> = date("release_date")
    }

    object ActorTable: LongIdTable("actors") {
        val firstName: Column<String> = varchar("first_name", 255)
        val lastName: Column<String> = varchar("last_name", 255)
        val birthday: Column<LocalDate?> = date("birthday").nullable()
    }

    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = CASCADE)

        override val primaryKey = PrimaryKey(movieId, actorId)
    }

    class MovieEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<MovieEntity>(MovieTable)

        var name: String by MovieTable.name
        var producerName: String by MovieTable.producerName
        var releaseDate: LocalDate by MovieTable.releaseDate

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
