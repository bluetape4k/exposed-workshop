package exposed.multitenant.webflux.domain.repository


import exposed.multitenant.webflux.domain.model.MovieActorCountRecord
import exposed.multitenant.webflux.domain.model.MovieRecord
import exposed.multitenant.webflux.domain.model.MovieSchema
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorInMovieTable
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieEntity
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieTable
import exposed.multitenant.webflux.domain.model.MovieWithActorRecord
import exposed.multitenant.webflux.domain.model.MovieWithProducingActorRecord
import exposed.multitenant.webflux.domain.model.toActorRecord
import exposed.multitenant.webflux.domain.model.toMovieRecord
import exposed.multitenant.webflux.domain.model.toMovieWithActorRecord
import exposed.multitenant.webflux.domain.model.toMovieWithProducingActorRecord
import io.bluetape4k.exposed.jdbc.repository.JdbcRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * WebFlux 환경의 영화 도메인 Exposed 저장소입니다.
 */
@Repository
class MovieExposedRepository: JdbcRepository<Long, MovieTable, MovieRecord> {

    companion object: KLogging()

    override val table = MovieTable
    override fun ResultRow.toEntity() = toMovieRecord()

    /**
     * 검색 파라미터(식별자/이름/제작자/개봉일)로 영화 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    fun searchMovies(params: Map<String, String?>): List<MovieRecord> {
        log.debug { "Search Movie by params. params=$params" }

        val query = table.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                LongIdTable::id.name         -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }

                MovieTable::name.name        -> value?.run { query.andWhere { MovieTable.name eq value } }
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

    /**
     * 영화 정보를 저장하고 생성된 식별자를 포함한 레코드를 반환합니다.
     */
    fun create(movieRecord: MovieRecord): MovieRecord {
        log.debug { "Create new movie. movie: $movieRecord" }

        val id = MovieTable.insertAndGetId {
            it[name] = movieRecord.name
            it[producerName] = movieRecord.producerName
            it[releaseDate] = LocalDate.parse(movieRecord.releaseDate)
        }
        return movieRecord.copy(id = id.value)
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
    fun getMovieWithActors(movieId: Long): MovieWithActorRecord? {
        log.debug { "Get Movie with actors. movieId=$movieId" }

        return MovieSchema.MovieEntity.findById(movieId)
            ?.load(MovieEntity::actors)
            ?.toMovieWithActorRecord()
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
    fun getMovieActorsCount(): List<MovieActorCountRecord> {
        log.debug { "Get Movie actors count." }

        val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

        return join
            .select(MovieTable.id, MovieTable.name, ActorTable.id.count())
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
     *        INNER JOIN ACTORS_IN_MOVIES
     *          ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *        INNER JOIN ACTORS
     *          ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID AND (MOVIES.PRODUCER_NAME = ACTORS.FIRST_NAME)
     * ```
     */
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> {
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

        return query.map { it.toMovieWithProducingActorRecord() }
    }
}
