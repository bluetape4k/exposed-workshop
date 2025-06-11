package exposed.examples.springmvc.domain.repository

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
import io.bluetape4k.coroutines.flow.extensions.bufferUntilChanged
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
class MovieExposedRepository: ExposedRepository<MovieDTO, Long> {

    companion object: KLogging()

    override val table = MovieTable
    override fun ResultRow.toEntity(): MovieDTO = toMovieDTO()

    @Transactional(readOnly = true)
    fun searchMovies(params: Map<String, String?>): List<MovieDTO> {
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

        return query.map { it.toEntity() }
    }

    fun create(movieDto: MovieDTO): MovieDTO {
        log.debug { "Create new movie. movie: $movieDto" }

        val id = MovieTable.insertAndGetId {
            it[name] = movieDto.name
            it[producerName] = movieDto.producerName
            it[releaseDate] = LocalDate.parse(movieDto.releaseDate)
        }

        return movieDto.copy(id = id.value)
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

        // TODO: Iterable 의 확장함수로 bufferUntilChanged 함수를 추가해야 합니다.
        return runBlocking {
            val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)
            join
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
                .map { row ->
                    val movie = row.toMovieDTO()
                    val actor = row.toActorDTO()

                    movie to actor
                }
                .asFlow()
                .bufferUntilChanged { it.first.id }
                .mapNotNull { pairs ->
                    val movie = pairs.first().first
                    val actors = pairs.map { it.second }
                    movie.toMovieWithActorDTO(actors)
                }
                .toList()
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
