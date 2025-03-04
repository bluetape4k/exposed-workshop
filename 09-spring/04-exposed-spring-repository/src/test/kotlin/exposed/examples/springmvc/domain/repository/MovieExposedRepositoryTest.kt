package exposed.examples.springmvc.domain.repository

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.dtos.MovieDTO
import exposed.examples.springmvc.domain.dtos.toMovieDTO
import exposed.examples.springmvc.domain.model.MovieSchema.MovieTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class MovieExposedRepositoryTest(
    @Autowired private val movieRepository: MovieExposedRepository,
): AbstractExposedRepositoryTest() {

    companion object: KLogging() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `find movie by id`() {
        val movieId = 1L

        val movie = movieRepository.findById(movieId).toMovieDTO()

        log.debug { "movie: $movie" }
        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo movieId
    }

    @Test
    fun `search movies`() {
        val params = mapOf("producerName" to "Johnny")

        val movies = movieRepository.searchMovies(params)
        movies.forEach {
            log.debug { "movie: $it" }
        }
        movies shouldHaveSize 2
    }

    @Test
    @Transactional
    fun `create movie`() {
        val movie = newMovieDTO()

        val currentCount = MovieTable.selectAll().count()

        val savedMovie = movieRepository.create(movie).toMovieDTO()
        savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)

        val newCount = MovieTable.selectAll().count()
        newCount shouldBeEqualTo currentCount + 1
    }

    @Test
    @Transactional
    fun `delete movie`() {
        val newMovie = newMovieDTO()
        val saved = movieRepository.create(newMovie).toMovieDTO()

        val deletedCount = movieRepository.deleteById(saved.id!!)
        deletedCount shouldBeEqualTo 1
    }

    @Test
    fun `get all movies and actors`() {
        val movieWithActors = movieRepository.getAllMoviesWithActors()

        movieWithActors.shouldNotBeNull()
        movieWithActors.forEach { movie ->
            log.debug { "movie: ${movie.name}" }
            movie.actors.shouldNotBeEmpty()
            movie.actors.forEach { actor ->
                log.debug { "  actor: ${actor.firstName} ${actor.lastName}" }
            }
        }
    }

    @Test
    fun `get movie and actors`() {
        val movieId = 1L

        val movieWithActors = movieRepository.getMovieWithActors(movieId)

        log.debug { "movieWithActors: $movieWithActors" }

        movieWithActors.shouldNotBeNull()
        movieWithActors.id shouldBeEqualTo movieId
        movieWithActors.actors shouldHaveSize 3
    }

    @Test
    fun `get movie and actor count`() {
        val movieActorsCount = movieRepository.getMovieActorsCount()
        movieActorsCount.shouldNotBeEmpty()
        movieActorsCount.forEach {
            log.debug { "movie=${it.movieName}, actor count=${it.actorCount}" }
        }
    }

    @Test
    fun `find movies with acting producers`() {
        val results = movieRepository.findMoviesWithActingProducers()

        results shouldHaveSize 1
        results.forEach {
            log.debug { "movie=${it.movieName}, producer=${it.producerActorName}" }
        }
    }
}
