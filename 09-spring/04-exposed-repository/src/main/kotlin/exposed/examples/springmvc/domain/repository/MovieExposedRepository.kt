package exposed.examples.springmvc.domain.repository

import exposed.examples.springmvc.domain.dtos.ActorDTO
import exposed.examples.springmvc.domain.dtos.MovieActorCountDTO
import exposed.examples.springmvc.domain.dtos.MovieDTO
import exposed.examples.springmvc.domain.dtos.MovieWithActorDTO
import exposed.examples.springmvc.domain.dtos.MovieWithProducingActorDTO
import exposed.examples.springmvc.domain.dtos.toActorDTO
import exposed.examples.springmvc.domain.dtos.toMovieDTO
import exposed.examples.springmvc.domain.dtos.toMovieWithActorDTO
import exposed.examples.springmvc.domain.dtos.toMovieWithProducingActorDTO
import exposed.examples.springmvc.domain.model.MovieSchema.ActorInMovieTable
import exposed.examples.springmvc.domain.model.MovieSchema.ActorTable
import exposed.examples.springmvc.domain.model.MovieSchema.MovieEntity
import exposed.examples.springmvc.domain.model.MovieSchema.MovieTable
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.eclipse.collections.impl.factory.Multimaps
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
class MovieExposedRepository: ExposedRepository<MovieEntity, Long> {

    companion object: KLogging()

    override val table = MovieTable
    override fun ResultRow.toEntity(): MovieEntity = MovieEntity.wrapRow(this)

    @Transactional(readOnly = true)
    fun searchMovies(params: Map<String, String?>): List<MovieEntity> {
        log.debug { "Search Movie by params. params=$params" }

        val query = table.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieTable::id.name -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }

                MovieTable::name.name -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run {
                    query.andWhere { MovieTable.producerName eq value }
                }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere { MovieTable.releaseDate eq LocalDate.parse(value) }
                }
            }
        }

        return MovieEntity.wrapRows(query).toList()
    }

    fun create(movieDto: MovieDTO): MovieEntity {
        log.debug { "Create new movie. movie: $movieDto" }

        return MovieEntity.new {
            name = movieDto.name
            producerName = movieDto.producerName
            releaseDate = LocalDate.parse(movieDto.releaseDate)
        }
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
     */
    fun getAllMoviesWithActors(): List<MovieWithActorDTO> {
        log.debug { "Get all movies with actors." }

        val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

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
            )
            .toList()

        val movieDtos = hashMapOf<Long, MovieDTO>()
        val actorsInMovie = Multimaps.mutable.set.of<Long, ActorDTO>()

        movies.forEach { row ->
            val movieId = row[MovieTable.id].value

            if (!movieDtos.containsKey(movieId)) {
                movieDtos[movieId] = row.toMovieDTO()
            }
            actorsInMovie.getIfAbsentPutAll(movieId, mutableListOf(row.toActorDTO()))
        }

        return movieDtos.map { (id, movie) ->
            movie.toMovieWithActorDTO(actorsInMovie.get(id) ?: emptySet())
        }
    }


    /**
     * `movieId` 에 대한 영화와 그 영화에 출연한 배우를 조회합니다.
     *
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
     *  WHERE ACTORS_IN_MOVIES.MOVIE_ID = 1;
     * ```
     */
    fun getMovieWithActors(movieId: Long): MovieWithActorDTO? {
        log.debug { "Get Movie with actors. movieId=$movieId" }

        return MovieEntity.findById(movieId)
            ?.load(MovieEntity::actors)
            ?.toMovieWithActorDTO()
    }


    /**
     * ```sql
     * -- H2
     * SELECT MOVIES.ID,
     *        MOVIES."name",
     *        COUNT(ACTORS.ID)
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID
     *  GROUP BY MOVIES.ID
     * ```
     */
    fun getMovieActorsCount(): List<MovieActorCountDTO> {
        log.debug { "Get Movie actors count." }

        val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

        return join
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
     *        ACTORS.LAST_NAME,
     *   FROM MOVIES
     *        INNER JOIN ACTORS_IN_MOVIES
     *          ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *        INNER JOIN ACTORS
     *          ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID AND (MOVIES.PRODUCER_NAME = ACTORS.FIRST_NAME)
     * ```
     */
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
        log.debug { "Find movies with acting producers." }

        val query = table
            .innerJoin(ActorInMovieTable)
            .innerJoin(
                ActorTable,
                onColumn = { ActorTable.id },
                otherColumn = { ActorInMovieTable.actorId }
            ) {
                MovieTable.producerName eq ActorTable.firstName
            }
            .select(MovieTable.name, ActorTable.firstName, ActorTable.lastName)

        return query.map { it.toMovieWithProducingActorDTO() }
    }
}
