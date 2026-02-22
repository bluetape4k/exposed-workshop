package exposed.workshop.springmvc.domain.repository

import exposed.workshop.springmvc.domain.model.MovieActorCountRecord
import exposed.workshop.springmvc.domain.model.MovieRecord
import exposed.workshop.springmvc.domain.model.MovieSchema.ActorInMovieTable
import exposed.workshop.springmvc.domain.model.MovieSchema.ActorTable
import exposed.workshop.springmvc.domain.model.MovieSchema.MovieEntity
import exposed.workshop.springmvc.domain.model.MovieSchema.MovieTable
import exposed.workshop.springmvc.domain.model.MovieWithActorRecord
import exposed.workshop.springmvc.domain.model.MovieWithProducingActorRecord
import exposed.workshop.springmvc.domain.model.toActorRecord
import exposed.workshop.springmvc.domain.model.toMovieRecord
import exposed.workshop.springmvc.domain.model.toMovieWithActorRecord
import exposed.workshop.springmvc.domain.model.toMovieWithProducingActorRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
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
    fun findById(movieId: Long): MovieRecord? {
        log.debug { "Find Movie by id. id=$movieId" }

        return MovieTable
            .selectAll()
            .where { MovieTable.id eq movieId }
            .singleOrNull()
            ?.toMovieRecord()

        // DAO 방식으로 구현해도 됩니다.
        // return MovieEntity.findById(movieId)?.toMovieRecord()
    }

    fun searchMovies(params: Map<String, String?>): List<MovieRecord> {
        log.debug { "Search Movie by params. params=$params" }

        val query = MovieTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieTable::id.name          -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }

                MovieTable::name.name        -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run {
                    query.andWhere { MovieTable.producerName eq value }
                }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere { MovieTable.releaseDate eq LocalDateTime.parse(value) }
                }
            }
        }

        return query.map { it.toMovieRecord() }
    }

    @Transactional
    fun create(movieRecord: MovieRecord): MovieRecord {
        log.debug { "Create Movie. movie=$movieRecord" }

        val movidId = MovieTable.insertAndGetId {
            it[name] = movieRecord.name
            it[producerName] = movieRecord.producerName
            it[releaseDate] = LocalDate.parse(movieRecord.releaseDate).atTime(0, 0)
        }
        return movieRecord.copy(id = movidId.value)
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
    fun getAllMoviesWithActors(): List<MovieWithActorRecord> {
        log.debug { "Get all movies with actors." }

        val join = MovieTable.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

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
                val movie = rows.first().toMovieRecord()
                val actor = rows.map { it.toActorRecord() }

                movie.toMovieWithActorRecord(actor)
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
    fun getMovieWithActors(movieId: Long): MovieWithActorRecord? {
        log.debug { "Get Movie with actors. movieId=$movieId" }

        // DAO 방식
        return MovieEntity.findById(movieId)
            ?.load(MovieEntity::actors)
            ?.toMovieWithActorRecord()

        // SQL 방식
//        return MovieTable.selectAll()
//            .where { MovieTable.id eq movieId }
//            .singleOrNull()?.let {
//                val actors = ActorTable.innerJoin(ActorInMovieTable)
//                    .select(ActorTable.columns)
//                    .where { ActorInMovieTable.movieId eq movieId }
//                    .map { it.toActorRecord() }
//                it.toMovieWithActorRecord(actors)
//            }
    }

    fun getMovieActorsCount(): List<MovieActorCountRecord> {
        log.debug { "Get Movie actors count." }

        val join = MovieTable.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

        return join
            .select(
                MovieTable.id,
                MovieTable.name,
                ActorTable.id.count()
            )
            .groupBy(MovieTable.id)
            .map {
                MovieActorCountRecord(
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
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> {
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
            .select(
                MovieTable.name,
                ActorTable.firstName,
                ActorTable.lastName
            )

        return query.map { it.toMovieWithProducingActorRecord() }
    }
}
