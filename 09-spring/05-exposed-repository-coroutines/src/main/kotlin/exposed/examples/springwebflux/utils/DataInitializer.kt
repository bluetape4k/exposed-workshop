package exposed.examples.springwebflux.utils

import exposed.examples.springwebflux.domain.dtos.ActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithActorDTO
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorInMovieTable
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieTable
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
        log.info { "데이터베이스 초기화 및 샘플 데이터 추가" }
        populateData()
    }

    /**
     * 데이터베이스에 샘플 데이터를 삽입하는 메서드
     * 이미 데이터가 존재하는 경우, 삽입하지 않음
     */
    private fun populateData() {
        val totalActors = ActorTable.selectAll().count()

        if (totalActors > 0) {
            log.info { "There appears to be data already present, not inserting test data!" }
            return
        }

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
            ),
            MovieWithActorDTO(
                0L,
                "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                mutableListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ),
            MovieWithActorDTO(
                0L,
                "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                mutableListOf(bradPitt, jenniferAniston, edwardNorton)
            ),
            MovieWithActorDTO(
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
