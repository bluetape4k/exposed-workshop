package exposed.multitenant.springweb.tenant

import exposed.multitenant.springweb.domain.dtos.ActorRecord
import exposed.multitenant.springweb.domain.dtos.MovieWithActorRecord
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorInMovieTable
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import exposed.multitenant.springweb.domain.model.MovieSchema.MovieTable
import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Application 시작 시 DB 스키마 생성 및 샘플 데이터를 삽입하는 클래스
 */
@Component
@Transactional
class DataInitializer {

    companion object: KLogging()

    fun initialize() {
        log.info { "데이터베이스 초기화 및 샘플 데이터 추가" }

        val tenant = TenantContext.getCurrentTenant()
        createSchema(tenant)
        populateData(tenant)
    }

    private fun createSchema(tenant: Tenants.Tenant) {
        log.debug { "Creating schema and test data ..." }

        val currentSchema = getSchemaDefinition(tenant)
        SchemaUtils.createSchema(currentSchema)
        SchemaUtils.setSchema(currentSchema)

        @Suppress("DEPRECATION")
        SchemaUtils.createMissingTablesAndColumns(ActorTable, MovieTable, ActorInMovieTable)
    }

    private fun populateData(tenant: Tenants.Tenant) {

        val totalActors = ActorTable.selectAll().count()

        if (totalActors > 0) {
            log.info { "There appears to be data already present, not inserting test data!" }
            return
        }

        log.info { "Inserting sample actors and movies ..." }

        val johnnyDepp = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Johnny", "Depp", "1973-06-09")
            else -> ActorRecord(0L, "조니", "뎁", "1979-10-28")
        }
        val bradPitt = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Brad", "Pitt", "1970-12-18")
            else -> ActorRecord(0L, "브래드", "피트", "1982-05-16")
        }
        val angelinaJolie = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Angelina", "Jolie", "1983-11-10")
            else -> ActorRecord(0L, "안제리나", "졸리", "1983-11-10")
        }
        val jenniferAniston = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Jennifer", "Aniston", "1975-07-23")
            else -> ActorRecord(0L, "제니퍼", "애니스톤", "1975-07-23")
        }
        val angelinaGrace = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Angelina", "Grace", "1988-09-02")
            else -> ActorRecord(0L, "안젤리나", "그레이스", "1988-09-02")
        }
        val craigDaniel = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Craig", "Daniel", "1970-11-12")
            else -> ActorRecord(0L, "다니엘", "크레이그", "1970-11-12")
        }
        val ellenPaige = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Ellen", "Paige", "1981-12-20")
            else -> ActorRecord(0L, "엘렌", "페이지", "1981-12-20")
        }
        val russellCrowe = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Russell", "Crowe", "1970-01-20")
            else -> ActorRecord(0L, "러셀", "크로우", "1970-01-20")
        }
        val edwardNorton = when (tenant) {
            Tenant.ENGLISH -> ActorRecord(0L, "Edward", "Norton", "1975-04-03")
            else -> ActorRecord(0L, "에드워드", "노튼", "1975-04-03")
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
            MovieWithActorRecord(
                0L,
                if (tenant == Tenant.KOREAN) "글래디에이터" else "Gladiator",
                johnnyDepp.firstName,
                "2000-05-01",
                listOf(russellCrowe, ellenPaige, craigDaniel)
            ),
            MovieWithActorRecord(
                0L,
                if (tenant == Tenant.KOREAN) "가디언스 오브 갤럭시" else "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                listOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ),
            MovieWithActorRecord(
                0L,
                if (tenant == Tenant.KOREAN) "싸움 클럽" else "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                listOf(bradPitt, jenniferAniston, edwardNorton)
            ),
            MovieWithActorRecord(
                0L,
                if (tenant == Tenant.KOREAN) "13가지 이유" else "13 Reasons Why",
                "Suzuki",
                "2016-01-01",
                listOf(angelinaJolie, jenniferAniston)
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

            val actorIds = movie.actors.map { actor ->
                ActorTable
                    .select(ActorTable.id)
                    .where { ActorTable.firstName eq actor.firstName }
                    .andWhere { ActorTable.lastName eq actor.lastName }
                    .first()[ActorTable.id]
            }
            val movieActorIds = actorIds.map { movieId to it }
            ActorInMovieTable.batchInsert(movieActorIds) {
                this[ActorInMovieTable.movieId] = it.first.value
                this[ActorInMovieTable.actorId] = it.second.value
            }
        }
    }
}
