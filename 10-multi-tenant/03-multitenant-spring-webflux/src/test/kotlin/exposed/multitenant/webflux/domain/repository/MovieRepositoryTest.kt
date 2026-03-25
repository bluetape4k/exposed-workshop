package exposed.multitenant.webflux.domain.repository

import exposed.multitenant.webflux.AbstractMultitenantTest
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorInMovieTable
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieTable
import exposed.multitenant.webflux.domain.model.toActorRecord
import exposed.multitenant.webflux.domain.model.toMovieRecord
import exposed.multitenant.webflux.tenant.Tenants
import exposed.multitenant.webflux.tenant.newSuspendedTransactionWithTenant
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Spring WebFlux 멀티테넌트 환경에서 테넌트별 영화 데이터 스키마 격리를 검증합니다. */
class MovieRepositoryTest: AbstractMultitenantTest() {

    companion object: KLoggingChannel()

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `테넌트별 모든 영화 조회`(tenant: Tenants.Tenant) = runSuspendIO {
        newSuspendedTransactionWithTenant(tenant) {
            val movies = MovieTable.selectAll().map { it.toMovieRecord() }
            log.debug { "tenant=${tenant.id}, movies.size=${movies.size}" }
            movies shouldHaveSize 4
        }
    }

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `영화 이름으로 검색하면 해당 영화만 반환한다`(tenant: Tenants.Tenant) = runSuspendIO {
        val movieName = when (tenant) {
            Tenants.Tenant.ENGLISH -> "Gladiator"
            Tenants.Tenant.KOREAN  -> "글래디에이터"
        }
        newSuspendedTransactionWithTenant(tenant) {
            val movies = MovieTable.selectAll()
                .where { MovieTable.name eq movieName }
                .map { it.toMovieRecord() }
            movies shouldHaveSize 1
            movies.first().name shouldBeEqualTo movieName
        }
    }

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `모든 영화와 출연 배우를 조인하여 조회한다`(tenant: Tenants.Tenant) = runSuspendIO {
        newSuspendedTransactionWithTenant(tenant) {
            val join = MovieTable.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

            val results = join
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
                    val actors = rows.map { it.toActorRecord() }
                    movie to actors
                }

            results.shouldNotBeEmpty()
            results.forEach { (movie, actors) ->
                log.debug { "tenant=${tenant.id}, movie=${movie.name}, actors=${actors.size}" }
                actors.shouldNotBeEmpty()
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `영화별 출연 배우 수를 집계한다`(tenant: Tenants.Tenant) = runSuspendIO {
        newSuspendedTransactionWithTenant(tenant) {
            val join = MovieTable.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

            val counts = join
                .select(MovieTable.id, MovieTable.name, ActorTable.id.count())
                .groupBy(MovieTable.id)
                .toList()

            counts.shouldNotBeEmpty()
            counts.forEach {
                log.debug { "tenant=${tenant.id}, movie=${it[MovieTable.name]}, actorCount=${it[ActorTable.id.count()]}" }
            }
            counts shouldHaveSize 4
        }
    }

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `제작자 이름으로 영화를 검색한다`(tenant: Tenants.Tenant) = runSuspendIO {
        val producerName = when (tenant) {
            Tenants.Tenant.ENGLISH -> "Johnny"
            Tenants.Tenant.KOREAN  -> "조니"
        }
        newSuspendedTransactionWithTenant(tenant) {
            val movies = MovieTable.selectAll()
                .where { MovieTable.producerName eq producerName }
                .map { it.toMovieRecord() }
            movies.shouldNotBeEmpty()
            movies.forEach { log.debug { "tenant=${tenant.id}, movie=$it" } }
        }
    }

    @ParameterizedTest
    @EnumSource(Tenants.Tenant::class)
    fun `모든 테넌트의 영화 데이터가 서로 독립적으로 격리된다`(tenant: Tenants.Tenant) = runSuspendIO {
        newSuspendedTransactionWithTenant(tenant) {
            val movies = MovieTable.selectAll().map { it.toMovieRecord() }
            val actors = ActorTable.selectAll().map { it.toActorRecord() }

            movies.shouldNotBeNull()
            actors.shouldNotBeNull()

            val expectedFirstName = when (tenant) {
                Tenants.Tenant.KOREAN  -> "조니"
                Tenants.Tenant.ENGLISH -> "Johnny"
            }
            actors.any { it.firstName == expectedFirstName }.shouldBeEqualTo(true)
            log.debug { "tenant=${tenant.id}: ${movies.size} movies, ${actors.size} actors" }
        }
    }
}
