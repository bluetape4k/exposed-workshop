package exposed.workshop.springwebflux.utils

import exposed.workshop.springwebflux.domain.model.ActorRecord
import exposed.workshop.springwebflux.domain.model.MovieSchema.ActorInMovieTable
import exposed.workshop.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.workshop.springwebflux.domain.model.MovieSchema.MovieTable
import exposed.workshop.springwebflux.domain.model.MovieWithActorRecord
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
@Transactional
class DataInitializer: ApplicationListener<ApplicationReadyEvent> {

    companion object: KLoggingChannel()

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        log.info { "샘플 데이터 추가" }
        populateData()
    }

    private fun populateData() {
        val totalActors = ActorTable.selectAll().count()

        if (totalActors > 0) {
            log.info { "There appears to be data already present, not inserting test data!" }
            return
        }

        log.info { "Inserting sample actors and movies ..." }

        val johnnyDepp = ActorRecord(0L, "Johnny", "Depp", "1979-10-28")
        val bradPitt = ActorRecord(0L, "Brad", "Pitt", "1982-05-16")
        val angelinaJolie = ActorRecord(0L, "Angelina", "Jolie", "1983-11-10")
        val jenniferAniston = ActorRecord(0L, "Jennifer", "Aniston", "1975-07-23")
        val angelinaGrace = ActorRecord(0L, "Angelina", "Grace", "1988-09-02")
        val craigDaniel = ActorRecord(0L, "Craig", "Daniel", "1970-11-12")
        val ellenPaige = ActorRecord(0L, "Ellen", "Paige", "1981-12-20")
        val russellCrowe = ActorRecord(0L, "Russell", "Crowe", "1970-01-20")
        val edwardNorton = ActorRecord(0L, "Edward", "Norton", "1975-04-03")

        val actors = fastListOf(
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

        val movies = fastListOf(
            MovieWithActorRecord(
                0L,
                "Gladiator",
                johnnyDepp.firstName,
                "2000-05-01",
                fastListOf(russellCrowe, ellenPaige, craigDaniel)
            ),
            MovieWithActorRecord(
                0L,
                "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                fastListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ),
            MovieWithActorRecord(
                0L,
                "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                fastListOf(bradPitt, jenniferAniston, edwardNorton)
            ),
            MovieWithActorRecord(
                0L,
                "13 Reasons Why",
                "Suzuki",
                "2016-01-01",
                fastListOf(angelinaJolie, jenniferAniston)
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
            this[MovieTable.releaseDate] = LocalDate.parse(it.releaseDate).atTime(0, 0)
        }

        movies.forEach { movie ->
            val movieId = MovieTable
                .select(MovieTable.id)
                .where { MovieTable.name eq movie.name }
                .first()[MovieTable.id]

            movie.actors.forEach { actor ->
                val actorId = ActorTable
                    .select(ActorTable.id)
                    .where { (ActorTable.firstName eq actor.firstName) and (ActorTable.lastName eq actor.lastName) }
                    .first()[ActorTable.id]

                ActorInMovieTable.insert {
                    it[ActorInMovieTable.actorId] = actorId.value
                    it[ActorInMovieTable.movieId] = movieId.value
                }
            }
        }
    }
}
