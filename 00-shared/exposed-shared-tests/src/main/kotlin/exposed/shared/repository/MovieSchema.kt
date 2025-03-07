package exposed.shared.repository

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

object MovieSchema: KLogging() {

    object MovieTable: LongIdTable("movies") {
        val name = varchar("name", 255)
        val producerName = varchar("producer_name", 255)
        val releaseDate = date("release_date")
    }

    object ActorTable: LongIdTable("actors") {
        val firstName = varchar("first_name", 255)
        val lastName = varchar("last_name", 255)
        val birthday = date("birthday").nullable()
    }

    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = CASCADE)

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
        override fun toString(): String =
            toStringBuilder().add("name", name).add("producerName", producerName).add("releaseDate", releaseDate)
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
        override fun toString(): String =
            toStringBuilder().add("firstName", firstName).add("lastName", lastName).add("birthday", birthday).toString()
    }

    fun AbstractExposedTest.withMovieAndActors(
        testDB: TestDB,
        statement: Transaction.() -> Unit,
    ) {
        withTables(testDB, MovieTable, ActorTable, ActorInMovieTable) {
            populateSampleData()
            statement()
        }
    }

    suspend fun AbstractExposedTest.withSuspendedMovieAndActors(
        testDB: TestDB,
        statement: suspend Transaction.() -> Unit,
    ) {
        withSuspendedTables(testDB, MovieTable, ActorTable, ActorInMovieTable) {
            populateSampleData()
            statement()
        }
    }

    private fun Transaction.populateSampleData() {
        log.info { "Inserting sample actors and movies ..." }

        val johnnyDepp = ActorDTO("Johnny", "Depp", "1979-10-28")
        val bradPitt = ActorDTO("Brad", "Pitt", "1982-05-16")
        val angelinaJolie = ActorDTO("Angelina", "Jolie", "1983-11-10")
        val jenniferAniston = ActorDTO("Jennifer", "Aniston", "1975-07-23")
        val angelinaGrace = ActorDTO("Angelina", "Grace", "1988-09-02")
        val craigDaniel = ActorDTO("Craig", "Daniel", "1970-11-12")
        val ellenPaige = ActorDTO("Ellen", "Paige", "1981-12-20")
        val russellCrowe = ActorDTO("Russell", "Crowe", "1970-01-20")
        val edwardNorton = ActorDTO("Edward", "Norton", "1975-04-03")

        val actors = listOf(
            johnnyDepp,
            bradPitt,
            angelinaJolie,
            jenniferAniston,
            angelinaGrace,
            craigDaniel,
            ellenPaige,
            russellCrowe,
            edwardNorton
        )

        val movies = listOf(
            MovieWithActorDTO(
                "Gladiator", johnnyDepp.firstName, "2000-05-01", mutableListOf(russellCrowe, ellenPaige, craigDaniel)
            ), MovieWithActorDTO(
                "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                mutableListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ), MovieWithActorDTO(
                "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                mutableListOf(bradPitt, jenniferAniston, edwardNorton)
            ), MovieWithActorDTO(
                "13 Reasons Why", "Suzuki", "2016-01-01", mutableListOf(angelinaJolie, jenniferAniston)
            )
        )

        ActorTable.batchInsert(actors) {
            this[ActorTable.firstName] = it.firstName
            this[ActorTable.lastName] = it.lastName
            it.birthday?.let { birthDay ->
                this[ActorTable.birthday] = LocalDate.parse(birthDay)
            }
        }

        MovieTable.batchInsert(movies) {
            this[MovieTable.name] = it.name
            this[MovieTable.producerName] = it.producerName
            this[MovieTable.releaseDate] = LocalDate.parse(it.releaseDate)
        }

        movies.forEach { movie ->
            val movieId =
                MovieTable.select(MovieTable.id).where { MovieTable.name eq movie.name }.first()[MovieTable.id]

            movie.actors.forEach { actor ->
                val actorId = ActorTable.select(ActorTable.id)
                    .where { (ActorTable.firstName eq actor.firstName) and (ActorTable.lastName eq actor.lastName) }
                    .first()[ActorTable.id]

                ActorInMovieTable.insert {
                    it[ActorInMovieTable.actorId] = actorId.value
                    it[ActorInMovieTable.movieId] = movieId.value
                }
            }
        }

        flushCache()
    }
}
