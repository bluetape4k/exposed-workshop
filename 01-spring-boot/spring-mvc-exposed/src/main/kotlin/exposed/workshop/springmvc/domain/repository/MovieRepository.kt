package exposed.workshop.springmvc.domain.repository

import exposed.workshop.springmvc.domain.ActorDTO
import exposed.workshop.springmvc.domain.MovieActorCountDTO
import exposed.workshop.springmvc.domain.MovieDTO
import exposed.workshop.springmvc.domain.MovieSchema.ActorInMovieTable
import exposed.workshop.springmvc.domain.MovieSchema.ActorTable
import exposed.workshop.springmvc.domain.MovieSchema.MovieEntity
import exposed.workshop.springmvc.domain.MovieSchema.MovieTable
import exposed.workshop.springmvc.domain.MovieWithActorDTO
import exposed.workshop.springmvc.domain.MovieWithProducingActorDTO
import exposed.workshop.springmvc.domain.toActorDTO
import exposed.workshop.springmvc.domain.toMovieDTO
import exposed.workshop.springmvc.domain.toMovieWithActorDTO
import exposed.workshop.springmvc.domain.toMovieWithProducingActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.eclipse.collections.impl.factory.Multimaps
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
@Transactional(readOnly = true)
class MovieRepository(
    private val db: Database,
) {
    companion object: KLogging()

    /**
     * ```sql
     * SELECT MOVIES.ID,
     *        MOVIES."name",
     *        MOVIES.PRODUCER_NAME,
     *        MOVIES.RELEASE_DATE
     *   FROM MOVIES
     *  WHERE MOVIES.ID = 1
     * ```
     */
    fun findById(movieId: Long): MovieDTO? {
        log.debug { "Find Movie by id. id=$movieId" }

        return MovieTable
            .selectAll()
            .where { MovieTable.id eq movieId }
            .singleOrNull()
            ?.toMovieDTO()

        // DAO 방식으로 구현해도 됩니다.
        // return MovieEntity.findById(movieId)?.toMovieDTO()
    }

    fun searchMovies(params: Map<String, String?>): List<MovieDTO> {
        log.debug { "Search Movie by params. params=$params" }

        val query = MovieTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieTable::id.name -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }

                MovieTable::name.name -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run {
                    query.andWhere { MovieTable.producerName eq value }
                }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere { MovieTable.releaseDate eq LocalDateTime.parse(value) }
                }
            }
        }

        return query.map { it.toMovieDTO() }
    }

    @Transactional
    fun create(movieDto: MovieDTO): MovieDTO {
        log.debug { "Create Movie. movie=$movieDto" }

        val movidId = MovieTable.insertAndGetId {
            it[name] = movieDto.name
            it[producerName] = movieDto.producerName
            it[releaseDate] = LocalDate.parse(movieDto.releaseDate).atTime(0, 0)
        }
        return movieDto.copy(id = movidId.value)
    }

    @Transactional
    fun deleteById(movieId: Long): Int {
        log.debug { "Delete Movie by id. id=$movieId" }
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
     */
    fun getAllMoviesWithActors(): List<MovieWithActorDTO> {
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
     * `movieId` 에 대한 영화와 그 영화에 출연한 배우를 조회합니다.
     *
     * ```sql
     * -- H2
     * SELECT movies.id, movies."name", movies.producer_name, movies.release_date
     *   FROM movies WHERE movies.id = 1;
     *
     * SELECT actors.id, actors.first_name, actors.last_name, actors.birthday, actors_in_movies.movie_id, actors_in_movies.actor_id
     *   FROM actors
     *          INNER JOIN actors_in_movies ON actors_in_movies.actor_id = actors.id
     *  WHERE actors_in_movies.movie_id = 1;
     * ```
     */
    fun getMovieWithActors(movieId: Long): MovieWithActorDTO? {
        log.debug { "Get Movie with actors. movieId=$movieId" }

        return MovieEntity.findById(movieId)
            ?.load(MovieEntity::actors)
            ?.toMovieWithActorDTO()

//        return MovieTable.selectAll()
//            .where { MovieTable.id eq movieId }
//            .singleOrNull()?.let {
//                val actors = ActorTable.innerJoin(ActorInMovieTable)
//                    .select(ActorTable.columns)
//                    .where { ActorInMovieTable.movieId eq movieId }
//                    .map { it.toActorDTO() }
//                it.toMovieWithActorDTO(actors)
//            }
    }

    fun getMovieActorsCount(): List<MovieActorCountDTO> {
        log.debug { "Get Movie actors count." }

        val join = MovieTable.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

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
     *        INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *        INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID AND (MOVIES.PRODUCER_NAME = ACTORS.FIRST_NAME)
     * ```
     */
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
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

        return query.map { it.toMovieWithProducingActorDTO() }
    }
}
