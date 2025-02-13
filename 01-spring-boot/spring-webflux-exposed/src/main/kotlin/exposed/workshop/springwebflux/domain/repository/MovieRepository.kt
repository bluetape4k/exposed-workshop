package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.domain.MovieActorCountDTO
import exposed.workshop.springwebflux.domain.MovieDTO
import exposed.workshop.springwebflux.domain.MovieSchema.ActorInMovieTable
import exposed.workshop.springwebflux.domain.MovieSchema.ActorTable
import exposed.workshop.springwebflux.domain.MovieSchema.MovieEntity
import exposed.workshop.springwebflux.domain.MovieSchema.MovieTable
import exposed.workshop.springwebflux.domain.MovieWithActorDTO
import exposed.workshop.springwebflux.domain.MovieWithProducingActorDTO
import exposed.workshop.springwebflux.domain.toActorDTO
import exposed.workshop.springwebflux.domain.toMovieDTO
import exposed.workshop.springwebflux.domain.toMovieWithActorDTO
import exposed.workshop.springwebflux.domain.toMovieWithProducingActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MovieRepository {

    companion object: KLogging() {
        private val MovieActorJoin by lazy {
            MovieTable
                .innerJoin(ActorInMovieTable)
                .innerJoin(ActorTable)
        }
    }

    suspend fun count(): Long = newSuspendedTransaction(readOnly = true) {
        MovieTable.selectAll().count()
    }

    suspend fun findById(movieId: Long) = newSuspendedTransaction(readOnly = true) {
        log.debug { "Find Movie by id. id: $movieId" }

        MovieTable.selectAll()
            .where { MovieTable.id eq movieId }
            .firstOrNull()
            ?.toMovieDTO()
    }

    suspend fun searchMovie(params: Map<String, String?>): Flow<MovieDTO> = newSuspendedTransaction(readOnly = true) {
        log.debug { "Search Movie by params. params: $params" }

        val query = MovieTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieTable::id.name -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }
                MovieTable::name.name -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run { query.andWhere { MovieTable.producerName eq value } }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere {
                        MovieTable.releaseDate eq LocalDateTime.parse(value)
                    }
                }
            }
        }

        query.map { it.toMovieDTO() }.asFlow()
    }

    suspend fun create(movie: MovieDTO): MovieDTO = newSuspendedTransaction {
        log.debug { "Create Movie. movie: $movie" }

        val newMovie = MovieEntity.new {
            name = movie.name
            producerName = movie.producerName
            if (movie.releaseDate.isNotBlank()) {
                releaseDate = LocalDateTime.parse(movie.releaseDate)
            }
        }

        newMovie.toMovieDTO()
    }

    suspend fun deleteById(movieId: Long): Int = newSuspendedTransaction {
        log.debug { "Delete Movie by id. id: $movieId" }
        MovieTable.deleteWhere { MovieTable.id eq movieId }
    }

    /**
     * ```sql
     * SELECT MOVIES.ID,
     *        MOVIES."name",
     *        MOVIES.PRODUCER_NAME,
     *        MOVIES.RELEASE_DATE,
     *        ACTORS.ID,
     *        ACTORS.FIRST_NAME,
     *        ACTORS.LAST_NAME,
     *        ACTORS.BIRTHDAY
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID
     * ```
     */
    suspend fun getAllMoviesWithActors(): List<MovieWithActorDTO> = newSuspendedTransaction(readOnly = true) {
        log.debug { "Get all movies with actors." }

        val movies = MovieActorJoin
            .select(
                MovieTable.id,
                MovieTable.name,
                MovieTable.producerName,
                MovieTable.releaseDate,
                ActorTable.id,
                ActorTable.firstName,
                ActorTable.lastName,
                ActorTable.birthday
            )
            .groupingBy { it[MovieTable.id] }
            .fold(mutableListOf<MovieWithActorDTO>()) { acc, row ->
                val lastMovieId = acc.lastOrNull()?.id
                if (lastMovieId != row[MovieTable.id].value) {
                    val movie = MovieWithActorDTO(
                        id = row[MovieTable.id].value,
                        name = row[MovieTable.name],
                        producerName = row[MovieTable.producerName],
                        releaseDate = row[MovieTable.releaseDate].toString(),
                    )
                    acc.add(movie)
                } else {
                    acc.lastOrNull()?.actors?.let {
                        val actor = row.toActorDTO()
                        it.add(actor)
                    }
                }
                acc
            }

        movies.values.flatten()
    }

    /**
     * ```sql
     * SELECT MOVIES.ID, MOVIES."name", MOVIES.PRODUCER_NAME, MOVIES.RELEASE_DATE
     *   FROM MOVIES
     *  WHERE MOVIES.ID = 1;
     *
     * SELECT ACTORS.ID,
     *        ACTORS.FIRST_NAME,
     *        ACTORS.LAST_NAME,
     *        ACTORS.BIRTHDAY
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID
     *  WHERE MOVIES.ID = 1
     * ```
     */
    suspend fun getMovieWithActors(movieId: Long): MovieWithActorDTO? = newSuspendedTransaction {
        log.debug { "Get Movie with actors. movieId=$movieId" }

        // FIXME: 이 것은 작동하지 않습니다. one-to-many 관계에서는 작동하는데, many-to-many 관계에서는 작동하지 않습니다.
        // MovieEntity.findById(movieId)?.load(MovieEntity::actors)?.toMovieWithActorDTO()

        val movie = MovieEntity.findById(movieId)
        movie?.let {
            val actors = MovieActorJoin
                .select(ActorTable.columns)
                .where { MovieTable.id eq movieId }
                .map { it.toActorDTO() }

            movie.toMovieDTO().toMovieWithActorDTO(actors)
        }
    }

    /**
     * ```sql
     * SELECT MOVIES.ID,
     *        MOVIES."name",
     *        COUNT(ACTORS.ID)
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID
     *  GROUP BY MOVIES.ID
     * ```
     */
    suspend fun getMovieActorsCount(): List<MovieActorCountDTO> = newSuspendedTransaction(readOnly = true) {
        log.debug { "Get Movie actors count." }

        MovieActorJoin
            .select(MovieTable.id, MovieTable.name, ActorTable.id.count())
            .groupBy(MovieTable.id)
            .map {
                MovieActorCountDTO(
                    movieName = it[MovieTable.name],
                    actorCount = it[ActorTable.id.count()].toInt()
                )
            }
    }

    /**
     * ```sql
     * SELECT MOVIES."name",
     *        ACTORS.FIRST_NAME,
     *        ACTORS.LAST_NAME
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID AND (MOVIES.PRODUCER_NAME = ACTORS.FIRST_NAME)
     * ```
     */
    suspend fun findMoviesWithActingProducers(): Flow<MovieWithProducingActorDTO> =
        newSuspendedTransaction(readOnly = true) {
            log.debug { "Find movies with acting producers." }

            val query = MovieTable
                .innerJoin(ActorInMovieTable)
                .innerJoin(
                    ActorTable,
                    onColumn = { ActorTable.id },
                    otherColumn = { ActorInMovieTable.actorId }
                ) {
                    MovieTable.producerName eq ActorTable.firstName
                }
                .select(MovieTable.name, ActorTable.firstName, ActorTable.lastName)

            query.map { it.toMovieWithProducingActorDTO() }.asFlow()
        }
}
