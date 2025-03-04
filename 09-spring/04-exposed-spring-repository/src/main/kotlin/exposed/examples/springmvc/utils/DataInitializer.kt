package exposed.examples.springmvc.utils

import exposed.examples.springmvc.domain.dtos.ActorDTO
import exposed.examples.springmvc.domain.dtos.MovieWithActorDTO
import exposed.examples.springmvc.domain.model.MovieSchema.ActorInMovieTable
import exposed.examples.springmvc.domain.model.MovieSchema.ActorTable
import exposed.examples.springmvc.domain.model.MovieSchema.MovieTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Application 시작 시 DB 스키마 생성 및 샘플 데이터를 삽입하는 클래스
 */
@Component
@Transactional
class DataInitializer: ApplicationRunner {

    companion object: KLogging()

    override fun run(args: ApplicationArguments?) {
        log.info { "데이터베이스 초기화 및 샘플 데이터 추가" }
        createSchema()
        populateData()
    }

    private fun createSchema() {
        log.debug { "Creating schema and test data ..." }

        @Suppress("DEPRECATION")
        SchemaUtils.createMissingTablesAndColumns(ActorTable, MovieTable, ActorInMovieTable)
    }

    private fun populateData() {
        val totalActors = ActorTable.selectAll().count()

        if (totalActors > 0) {
            log.info { "There appears to be data already present, not inserting test data!" }
            return
        }

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
                "Gladiator",
                johnnyDepp.firstName,
                "2000-05-01",
                mutableListOf(russellCrowe, ellenPaige, craigDaniel)
            ),
            MovieWithActorDTO(
                "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                mutableListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ),
            MovieWithActorDTO(
                "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                mutableListOf(bradPitt, jenniferAniston, edwardNorton)
            ),
            MovieWithActorDTO(
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
