package exposed.examples.springwebflux.domain.repository

import exposed.examples.springwebflux.domain.dtos.MovieActorCountDTO
import exposed.examples.springwebflux.domain.dtos.MovieDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithProducingActorDTO
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorInMovieTable
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieEntity
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieTable
import exposed.examples.springwebflux.domain.model.toActorDTO
import exposed.examples.springwebflux.domain.model.toMovieDTO
import exposed.examples.springwebflux.domain.model.toMovieWithActorDTO
import exposed.examples.springwebflux.domain.model.toMovieWithProducingActorDTO
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MovieExposedRepository: ExposedRepository<MovieDTO, Long> {

    companion object: KLoggingChannel() {
        private val MovieActorJoin by lazy {
            MovieTable
                .innerJoin(ActorInMovieTable)
                .innerJoin(ActorTable)
        }

        private val moviesWithActingProducersJoin: Join by lazy {
            MovieTable
                .innerJoin(ActorInMovieTable)
                .innerJoin(
                    ActorTable,
                    onColumn = { ActorTable.id },
                    otherColumn = { ActorInMovieTable.actorId }
                ) {
                    MovieTable.producerName eq ActorTable.firstName
                }
        }
    }

    override val table = MovieTable
    override fun ResultRow.toEntity() = toMovieDTO()

    fun searchMovie(params: Map<String, String?>): List<MovieDTO> {
        log.debug { "Search Movie by params. params: $params" }

        val query = MovieTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieTable::id.name -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }
                MovieTable::name.name -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run { query.andWhere { MovieTable.producerName eq value } }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere {
                        MovieTable.releaseDate eq LocalDate.parse(value)
                    }
                }
            }
        }

        return query.map { it.toEntity() }
    }

    suspend fun create(movie: MovieDTO): MovieDTO {
        log.debug { "Create Movie. movie: $movie" }

        val id = MovieTable.insertAndGetId {
            it[name] = movie.name
            it[producerName] = movie.producerName
            if (movie.releaseDate.isNotBlank()) {
                it[releaseDate] = LocalDate.parse(movie.releaseDate)
            }
        }
        return movie.copy(id = id.value)
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
    suspend fun getAllMoviesWithActors(): List<MovieWithActorDTO> {
        log.debug { "Get all movies with actors." }

        val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

        return join
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
            .groupBy { it[MovieTable.id] }
            .map { (_, rows) ->
                val movie = rows.first().toMovieDTO()
                val actor = rows.map { it.toActorDTO() }

                movie.toMovieWithActorDTO(actor)
            }
    }

    /**
     * `movieId`에 해당하는 [MovieDTO] 와 출현한 [ActorDTO]들의 정보를 eager loading 으로 가져온다.
     * ```sql
     * -- H2
     * SELECT MOVIES.ID, MOVIES."name", MOVIES.PRODUCER_NAME, MOVIES.RELEASE_DATE
     *   FROM MOVIES
     *  WHERE MOVIES.ID = 1;
     *
     * SELECT ACTORS.ID,
     *        ACTORS.FIRST_NAME,
     *        ACTORS.LAST_NAME,
     *        ACTORS.BIRTHDAY,
     *        ACTORS_IN_MOVIES.MOVIE_ID,
     *        ACTORS_IN_MOVIES.ACTOR_ID
     *   FROM ACTORS INNER JOIN ACTORS_IN_MOVIES ON ACTORS_IN_MOVIES.ACTOR_ID = ACTORS.ID
     *  WHERE ACTORS_IN_MOVIES.MOVIE_ID = 1
     * ```
     */
    suspend fun getMovieWithActors(movieId: Long): MovieWithActorDTO? {
        log.debug { "Get Movie with actors. movieId=$movieId" }
        return MovieEntity.findById(movieId)?.load(MovieEntity::actors)?.toMovieWithActorDTO()
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
    suspend fun getMovieActorsCount(): List<MovieActorCountDTO> {
        log.debug { "Get Movie actors count." }

        return MovieActorJoin
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
    suspend fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
        log.debug { "Find movies with acting producers." }

        val query: Query = moviesWithActingProducersJoin
            .select(
                MovieTable.name,
                ActorTable.firstName,
                ActorTable.lastName
            )

        return query.map { it.toMovieWithProducingActorDTO() }
    }
}
