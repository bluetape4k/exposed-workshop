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
import exposed.examples.springwebflux.domain.model.toMovieWithActorDTO
import exposed.examples.springwebflux.domain.model.toMovieWithProducingActorDTO
import exposed.shared.repository.AbstractCoroutineExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MovieRepository: AbstractCoroutineExposedRepository<MovieEntity, Long>(MovieTable) {

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

    override fun ResultRow.toEntity(): MovieEntity = MovieEntity.wrapRow(this)

    fun searchMovie(params: Map<String, String?>): List<MovieEntity> {
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

        return MovieEntity.wrapRows(query).toList()
    }

    suspend fun create(movie: MovieDTO): MovieEntity {
        log.debug { "Create Movie. movie: $movie" }

        val newMovie = MovieEntity.new {
            name = movie.name
            producerName = movie.producerName
            if (movie.releaseDate.isNotBlank()) {
                releaseDate = LocalDate.parse(movie.releaseDate)
            }
        }

        return newMovie
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

        return movies.values.flatten()
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
