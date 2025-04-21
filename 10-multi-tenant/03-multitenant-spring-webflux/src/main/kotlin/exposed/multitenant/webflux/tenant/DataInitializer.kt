package exposed.multitenant.webflux.tenant

import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.domain.dtos.MovieWithActorDTO
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorInMovieTable
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieTable
import exposed.multitenant.webflux.tenant.Tenants.Tenant
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Application 시작 시 DB 스키마 생성 및 샘플 데이터를 삽입하는 클래스
 */
@Component
class DataInitializer {

    companion object: KLogging()

    suspend fun initialize(tenant: Tenants.Tenant) {
        log.info { "데이터베이스 초기화 및 샘플 데이터 추가" }

        createSchema(tenant)
        populateData(tenant)
    }

    private suspend fun createSchema(tenant: Tenants.Tenant) {
        log.debug { "Creating schema and test data ..." }

        newSuspendedTransaction {
            val currentSchema = getSchemaDefinition(tenant)
            SchemaUtils.createSchema(currentSchema)
            SchemaUtils.setSchema(currentSchema)

            @Suppress("DEPRECATION")
            SchemaUtils.createMissingTablesAndColumns(ActorTable, MovieTable, ActorInMovieTable)
        }
    }

    private suspend fun populateData(tenant: Tenants.Tenant) {

        newSuspendedTransactionWithTenant(tenant) {

            val totalActors = ActorTable.selectAll().count()

            if (totalActors > 0) {
                log.info { "There appears to be data already present, not inserting test data!" }
                return@newSuspendedTransactionWithTenant
            }

            log.info { "Inserting sample actors and movies ..." }

            val johnnyDepp = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Johnny", "Depp", "1973-06-09")
                else -> ActorDTO("조니", "뎁", "1979-10-28")
            }
            val bradPitt = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Brad", "Pitt", "1970-12-18")
                else -> ActorDTO("브래드", "피트", "1982-05-16")
            }
            val angelinaJolie = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Angelina", "Jolie", "1983-11-10")
                else -> ActorDTO("안제리나", "졸리", "1983-11-10")
            }
            val jenniferAniston = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Jennifer", "Aniston", "1975-07-23")
                else -> ActorDTO("제니퍼", "애니스톤", "1975-07-23")
            }
            val angelinaGrace = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Angelina", "Grace", "1988-09-02")
                else -> ActorDTO("안젤리나", "그레이스", "1988-09-02")
            }
            val craigDaniel = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Craig", "Daniel", "1970-11-12")
                else -> ActorDTO("다니엘", "크레이그", "1970-11-12")
            }
            val ellenPaige = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Ellen", "Paige", "1981-12-20")
                else -> ActorDTO("엘렌", "페이지", "1981-12-20")
            }
            val russellCrowe = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Russell", "Crowe", "1970-01-20")
                else -> ActorDTO("러셀", "크로우", "1970-01-20")
            }
            val edwardNorton = when (tenant) {
                Tenant.ENGLISH -> ActorDTO("Edward", "Norton", "1975-04-03")
                else -> ActorDTO("에드워드", "노튼", "1975-04-03")
            }

            val actors = listOf(
                johnnyDepp,
                bradPitt,
                angelinaJolie,
                jenniferAniston,
                angelinaGrace,
                craigDaniel,
                ellenPaige,
                russellCrowe,
                edwardNorton
            )

            val movies = listOf(
                MovieWithActorDTO(
                    if (tenant == Tenant.KOREAN) "글래디에이터" else "Gladiator",
                    johnnyDepp.firstName,
                    "2000-05-01",
                    mutableListOf(russellCrowe, ellenPaige, craigDaniel)
                ),
                MovieWithActorDTO(
                    if (tenant == Tenant.KOREAN) "가디언스 오브 갤럭시" else "Guardians of the galaxy",
                    johnnyDepp.firstName,
                    "2014-07-21",
                    mutableListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
                ),
                MovieWithActorDTO(
                    if (tenant == Tenant.KOREAN) "싸움 클럽" else "Fight club",
                    craigDaniel.firstName,
                    "1999-09-13",
                    mutableListOf(bradPitt, jenniferAniston, edwardNorton)
                ),
                MovieWithActorDTO(
                    if (tenant == Tenant.KOREAN) "13가지 이유" else "13 Reasons Why",
                    "Suzuki",
                    "2016-01-01",
                    mutableListOf(angelinaJolie, jenniferAniston)
                )
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

                movie.actors.forEach { actor ->
                    val actorId = ActorTable
                        .select(ActorTable.id)
                        .where { (ActorTable.firstName eq actor.firstName) and (ActorTable.lastName eq actor.lastName) }
                        .first()[ActorTable.id]

                    ActorInMovieTable.insert {
                        it[ActorInMovieTable.actorId] = actorId.value
                        it[ActorInMovieTable.movieId] = movieId.value
                    }
                }
            }
        }
    }
}
