package exposed.workshop.springwebflux.domain

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object MovieSchema {

    object MovieTable: LongIdTable("movies") {
        val name = varchar("name", 255).index()
        val producerName = varchar("producer_name", 255).index()
        val releaseDate = datetime("release_date")
    }

    object ActorTable: LongIdTable("actors") {
        val firstName = varchar("first_name", 255).index()
        val lastName = varchar("last_name", 255).index()
        val birthday = date("birthday").nullable()
    }

    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId = long("movie_id").references(MovieTable.id, onDelete = CASCADE)
        val actorId = long("actor_id").references(ActorTable.id, onDelete = CASCADE)

        override val primaryKey = PrimaryKey(movieId, actorId)
    }

    class MovieEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<MovieEntity>(MovieTable)

        var name by MovieTable.name
        var producerName by MovieTable.producerName
        var releaseDate by MovieTable.releaseDate

        val actors by ActorEntity via ActorInMovieTable

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

        var firstName by ActorTable.firstName
        var lastName by ActorTable.lastName
        var birthday by ActorTable.birthday

        val movies by MovieEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("birthday", birthday)
            .toString()
    }
}
