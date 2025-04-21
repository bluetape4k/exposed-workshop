package exposed.workshop.springwebflux.domain.repository

import exposed.workshop.springwebflux.domain.ActorDTO
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
import org.eclipse.collections.impl.factory.Multimaps
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
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

    suspend fun count(): Long =
        MovieTable.selectAll().count()

    suspend fun findById(movieId: Long): MovieEntity? {
        log.debug { "Find Movie by id. id: $movieId" }

        return MovieTable.selectAll()
            .where { MovieTable.id eq movieId }
            .firstOrNull()
            ?.let { MovieEntity.wrapRow(it) }
    }

    suspend fun findAll(): List<MovieEntity> {
        return MovieEntity.wrapRows(MovieTable.selectAll()).toList()
    }

    suspend fun searchMovie(params: Map<String, String?>): List<MovieEntity> {
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

        return MovieEntity.wrapRows(query).toList()
    }

    suspend fun create(movie: MovieDTO): MovieEntity {
        log.debug { "Create Movie. movie: $movie" }

        return MovieEntity.new {
            name = movie.name
            producerName = movie.producerName
            if (movie.releaseDate.isNotBlank()) {
                releaseDate = LocalDateTime.parse(movie.releaseDate)
            }
        }
    }

    suspend fun deleteById(movieId: Long): Int {
        log.debug { "Delete Movie by id. id: $movieId" }
        return MovieTable.deleteWhere { MovieTable.id eq movieId }
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

        val join = MovieTable.innerJoin(ActorInMovieTable).innerJoin(ActorTable)
        val movies = join
            .select(
                MovieTable.id,
                MovieTable.name,
                MovieTable.producerName,
                MovieTable.releaseDate,
                ActorTable.id,
                ActorTable.firstName,
                ActorTable.lastName,
                ActorTable.birthday
            ).toList()

        val movieDtos = hashMapOf<Long, MovieDTO>()
        val actorDtos = Multimaps.mutable.set.of<Long, ActorDTO>()

        movies.forEach { row ->
            val movieId = row[MovieTable.id].value

            if (!movieDtos.containsKey(movieId)) {
                movieDtos[movieId] = row.toMovieDTO()
            }
            actorDtos.getIfAbsentPutAll(movieId, mutableSetOf(row.toActorDTO()))
        }

        return movieDtos.map { (id, movie) ->
            movie.toMovieWithActorDTO(actorDtos.get(id) ?: emptySet())
        }
    }

    /**
     * `movieId`에 해당하는 [Movie] 와 출현한 [Actor]들의 정보를 eager loading 으로 가져온다.
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
        return MovieEntity
            .findById(movieId)
            ?.load(MovieEntity::actors)     // eager loading
            ?.toMovieWithActorDTO()
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
