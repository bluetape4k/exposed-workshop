package exposed.shared.repository

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
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
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = ReferenceOption.CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = ReferenceOption.CASCADE)

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

    fun JdbcExposedTestBase.withMovieAndActors(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    ) {
        withTables(testDB, MovieTable, ActorTable, ActorInMovieTable) {
            populateSampleData()
            statement()
        }
    }

    suspend fun JdbcExposedTestBase.withSuspendedMovieAndActors(
        testDB: TestDB,
        statement: suspend JdbcTransaction.() -> Unit,
    ) {
        withSuspendedTables(testDB, MovieTable, ActorTable, ActorInMovieTable) {
            populateSampleData()
            statement()
        }
    }

    private fun Transaction.populateSampleData() {
        log.info { "Inserting sample actors and movies ..." }

        val johnnyDepp = ActorDTO(0L, "Johnny", "Depp", "1979-10-28")
        val bradPitt = ActorDTO(0L, "Brad", "Pitt", "1982-05-16")
        val angelinaJolie = ActorDTO(0L, "Angelina", "Jolie", "1983-11-10")
        val jenniferAniston = ActorDTO(0L, "Jennifer", "Aniston", "1975-07-23")
        val angelinaGrace = ActorDTO(0L, "Angelina", "Grace", "1988-09-02")
        val craigDaniel = ActorDTO(0L, "Craig", "Daniel", "1970-11-12")
        val ellenPaige = ActorDTO(0L, "Ellen", "Paige", "1981-12-20")
        val russellCrowe = ActorDTO(0L, "Russell", "Crowe", "1970-01-20")
        val edwardNorton = ActorDTO(0L, "Edward", "Norton", "1975-04-03")

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
                0L,
                "Gladiator",
                johnnyDepp.firstName,
                "2000-05-01",
                mutableListOf(russellCrowe, ellenPaige, craigDaniel)
            ), MovieWithActorDTO(
                0L,
                "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                mutableListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ), MovieWithActorDTO(
                0L,
                "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                mutableListOf(bradPitt, jenniferAniston, edwardNorton)
            ), MovieWithActorDTO(
                0L,
                "13 Reasons Why",
                "Suzuki",
                "2016-01-01",
                mutableListOf(angelinaJolie, jenniferAniston)
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
