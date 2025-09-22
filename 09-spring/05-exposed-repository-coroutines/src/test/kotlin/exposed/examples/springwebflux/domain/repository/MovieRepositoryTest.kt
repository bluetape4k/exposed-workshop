package exposed.examples.springwebflux.domain.repository

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.dtos.MovieDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("DEPRECATION")
class MovieRepositoryTest(
    @param:Autowired private val movieRepository: MovieExposedRepository,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel() {
        private fun newMovieDTO() = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `find movie by id`() = runSuspendIO {
        val movieId = 1L

        val movie = newSuspendedTransaction(readOnly = true) {
            movieRepository.findById(movieId)
        }

        log.debug { "movie: $movie" }
        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo movieId
    }

    /**
     * ```sql
     * SELECT MOVIES.ID, MOVIES."name", MOVIES.PRODUCER_NAME, MOVIES.RELEASE_DATE
     *   FROM MOVIES
     *  WHERE MOVIES.PRODUCER_NAME = 'Johnny'
     * ```
     * ```
     * MovieDTO(name=Gladiator, producerName=Johnny, releaseDate=2000-05-01T00:00, id=1)
     * MovieDTO(name=Guardians of the galaxy, producerName=Johnny, releaseDate=2014-07-21T00:00, id=2)
     * ```
     */
    @Test
    fun `search movies`() = runSuspendIO {
        val params = mapOf("producerName" to "Johnny")

        val movies = newSuspendedTransaction(readOnly = true) {
            movieRepository.searchMovie(params)
        }

        movies.forEach {
            log.debug { "movie: $it" }
        }
        movies.shouldNotBeEmpty() shouldHaveSize 2
    }

    @Test
    fun `create movie`() = runSuspendIO {
        newSuspendedTransaction {
            val prevCount = movieRepository.count()

            val newMovie = newMovieDTO()
            val saved = movieRepository.create(newMovie)

            saved.shouldNotBeNull()
            saved shouldBeEqualTo newMovie.copy(id = saved.id)

            movieRepository.count() shouldBeEqualTo prevCount + 1
        }
    }

    @Test
    fun `delete movie`() = runSuspendIO {
        newSuspendedTransaction {

            val newMovie = newMovieDTO()
            val saved = movieRepository.create(newMovie)

            val prevCount = movieRepository.count()

            val deletedCount = movieRepository.deleteById(saved.id)
            deletedCount shouldBeEqualTo 1

            movieRepository.count() shouldBeEqualTo prevCount - 1
        }
    }

    @Test
    fun `get all movies and actors`() = runSuspendIO {
        newSuspendedTransaction {
            val movieWithActors = movieRepository.getAllMoviesWithActors()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach { movie ->
                log.debug { "movie: ${movie.name}" }
                movie.actors.shouldNotBeEmpty()
                movie.actors.forEach { actor ->
                    log.debug { "  actor: ${actor.firstName} ${actor.lastName}" }
                }
            }
        }
    }

    @Test
    fun `get movie and actors`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val movieId = 1L

            val movieWithActors = movieRepository.getMovieWithActors(movieId)
            log.debug { "movieWithActors: $movieWithActors" }

            movieWithActors.shouldNotBeNull()
            movieWithActors.id shouldBeEqualTo movieId
            movieWithActors.actors.shouldNotBeEmpty()
        }
    }

    @Test
    fun `get movie and actors count`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val movieActorsCount = movieRepository.getMovieActorsCount()
            movieActorsCount.shouldNotBeEmpty()
            movieActorsCount.forEach {
                log.debug { "movie=${it.movieName}, actor count=${it.actorCount}" }
            }
        }
    }

    @Test
    fun `find movies with acting producers`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val movies = movieRepository.findMoviesWithActingProducers().toList()

            movies.forEach {
                log.debug { "movie: ${it.movieName}, actor: ${it.producerActorName}" }
            }
            movies.shouldNotBeEmpty() shouldHaveSize 1
        }
    }
}
