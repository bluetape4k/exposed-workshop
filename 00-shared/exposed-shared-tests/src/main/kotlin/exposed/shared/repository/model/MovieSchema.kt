package exposed.shared.repository.model

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import exposed.shared.tests.withTablesSuspending
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import java.time.LocalDate

/**
 * 영화(Movie)와 배우(Actor) 엔티티 및 테이블을 포함하는 스키마 정의 객체.
 *
 * 영화와 배우 간의 다대다(Many-to-Many) 관계를 [ActorInMovieTable]을 통해 표현합니다.
 */
object MovieSchema: KLogging() {
    /** 영화 테이블 정의. */
    object MovieTable: LongIdTable("movies") {
        val name = varchar("name", 255)
        val producerName = varchar("producer_name", 255)
        val releaseDate = date("release_date")
    }

    /** 배우 테이블 정의. */
    object ActorTable: LongIdTable("actors") {
        val firstName = varchar("first_name", 255)
        val lastName = varchar("last_name", 255)
        val birthday = date("birthday").nullable()
    }

    /** 영화-배우 간 다대다 매핑 테이블 정의. */
    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = ReferenceOption.CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = ReferenceOption.CASCADE)

        override val primaryKey = PrimaryKey(movieId, actorId)
    }

    /** 영화 엔티티. [MovieTable]과 매핑됩니다. */
    class MovieEntity(
        id: EntityID<Long>,
    ): LongEntity(id) {
        companion object: LongEntityClass<MovieEntity>(MovieTable)

        var name by MovieTable.name
        var producerName by MovieTable.producerName
        var releaseDate by MovieTable.releaseDate

        val actors: SizedIterable<ActorEntity> by ActorEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)

        override fun hashCode(): Int = idHashCode()

        override fun toString(): String =
            entityToStringBuilder()
                .add("name", name)
                .add("producerName", producerName)
                .add("releaseDate", releaseDate)
                .toString()
    }

    /** 배우 엔티티. [ActorTable]과 매핑됩니다. */
    class ActorEntity(
        id: EntityID<Long>,
    ): LongEntity(id) {
        companion object: LongEntityClass<ActorEntity>(ActorTable)

        var firstName by ActorTable.firstName
        var lastName by ActorTable.lastName
        var birthday by ActorTable.birthday

        val movies: SizedIterable<MovieEntity> by MovieEntity via ActorInMovieTable

        override fun equals(other: Any?): Boolean = idEquals(other)

        override fun hashCode(): Int = idHashCode()

        override fun toString(): String =
            entityToStringBuilder()
                .add("firstName", firstName)
                .add("lastName", lastName)
                .add("birthday", birthday)
                .toString()
    }

    /**
     * 영화, 배우, 매핑 테이블을 생성하고 샘플 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param testDB 사용할 테스트 데이터베이스
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withMovieAndActors(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    ) {
        withTables(testDB, MovieTable, ActorTable, ActorInMovieTable) {
            populateSampleData()
            statement()
        }
    }

    /**
     * 코루틴 환경에서 영화, 배우, 매핑 테이블을 생성하고 샘플 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param testDB 사용할 테스트 데이터베이스
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 서스펜딩 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    suspend fun AbstractExposedTest.withSuspendedMovieAndActors(
        testDB: TestDB,
        statement: suspend JdbcTransaction.() -> Unit,
    ) {
        withTablesSuspending(
            testDB,
            MovieTable, ActorTable, ActorInMovieTable,
            context = Dispatchers.IO,
            configure = { }
        ) {
            populateSampleData()
            statement()
        }
    }

    private fun Transaction.populateSampleData() {
        log.info { "Inserting sample actors and movies ..." }

        val johnnyDepp = ActorRecord(0L, "Johnny", "Depp", "1979-10-28")
        val bradPitt = ActorRecord(0L, "Brad", "Pitt", "1982-05-16")
        val angelinaJolie = ActorRecord(0L, "Angelina", "Jolie", "1983-11-10")
        val jenniferAniston = ActorRecord(0L, "Jennifer", "Aniston", "1975-07-23")
        val angelinaGrace = ActorRecord(0L, "Angelina", "Grace", "1988-09-02")
        val craigDaniel = ActorRecord(0L, "Craig", "Daniel", "1970-11-12")
        val ellenPaige = ActorRecord(0L, "Ellen", "Paige", "1981-12-20")
        val russellCrowe = ActorRecord(0L, "Russell", "Crowe", "1970-01-20")
        val edwardNorton = ActorRecord(0L, "Edward", "Norton", "1975-04-03")

        val actors =
            listOf(
                johnnyDepp,
                bradPitt,
                angelinaJolie,
                jenniferAniston,
                angelinaGrace,
                craigDaniel,
                ellenPaige,
                russellCrowe,
                edwardNorton,
            )

        val movies =
            listOf(
                MovieWithActorRecord(
                    0L,
                    "Gladiator",
                    johnnyDepp.firstName,
                    "2000-05-01",
                    mutableListOf(russellCrowe, ellenPaige, craigDaniel),
                ),
                MovieWithActorRecord(
                    0L,
                    "Guardians of the galaxy",
                    johnnyDepp.firstName,
                    "2014-07-21",
                    mutableListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp),
                ),
                MovieWithActorRecord(
                    0L,
                    "Fight club",
                    craigDaniel.firstName,
                    "1999-09-13",
                    mutableListOf(bradPitt, jenniferAniston, edwardNorton),
                ),
                MovieWithActorRecord(
                    0L,
                    "13 Reasons Why",
                    "Suzuki",
                    "2016-01-01",
                    mutableListOf(angelinaJolie, jenniferAniston),
                ),
            )

        ActorTable.batchInsert(actors) {
            this[ActorTable.firstName] = it.firstName
            this[ActorTable.lastName] = it.lastName
            it.birthday?.let { birthDay ->
                this[ActorTable.birthday] = LocalDate.parse(birthDay)
            }
        }

        MovieTable.batchInsert(movies) {
            this[MovieTable.name] = it.name
            this[MovieTable.producerName] = it.producerName
            this[MovieTable.releaseDate] = LocalDate.parse(it.releaseDate)
        }

        movies.forEach { movie ->
            val movieId = MovieTable
                .select(MovieTable.id)
                .where { MovieTable.name eq movie.name }
                .first()[MovieTable.id]

            val actorIds = movie.actors.map { actor ->
                ActorTable
                    .select(ActorTable.id)
                    .where { (ActorTable.firstName eq actor.firstName) and (ActorTable.lastName eq actor.lastName) }
                    .first()[ActorTable.id]
            }

            val movieActorIds = actorIds.map { movieId to it }
            ActorInMovieTable.batchInsert(movieActorIds) {
                this[ActorInMovieTable.movieId] = it.first.value
                this[ActorInMovieTable.actorId] = it.second.value
            }
        }

        flushCache()
    }
}
