package exposed.multitenant.springweb.domain.repository

import exposed.multitenant.springweb.AbstractMultitenantTest
import exposed.multitenant.springweb.tenant.TenantContext
import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

/** 가상 스레드 기반 멀티테넌트 환경에서 테넌트별 스키마 격리를 검증하는 `MovieExposedRepository` 통합 테스트. */
class MovieRepositoryTest(
    @param:Autowired private val movieRepo: MovieExposedRepository,
): AbstractMultitenantTest() {

    companion object: KLoggingChannel()

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `테넌트별 모든 영화 조회`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val movies = movieRepo.searchMovies(emptyMap())
            log.debug { "tenant=${tenant.id}, movies.size=${movies.size}" }
            movies shouldHaveSize 4
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `영화 이름으로 검색하면 해당 영화만 반환한다`(tenant: Tenant) {
        val movieName = when (tenant) {
            Tenant.ENGLISH -> "Gladiator"
            Tenant.KOREAN  -> "글래디에이터"
        }
        TenantContext.withTenant(tenant) {
            val movies = movieRepo.searchMovies(mapOf("name" to movieName))
            movies shouldHaveSize 1
            movies.first().name shouldBeEqualTo movieName
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `제작자 이름으로 검색하면 해당 제작자 영화만 반환한다`(tenant: Tenant) {
        val producerName = when (tenant) {
            Tenant.ENGLISH -> "Johnny"
            Tenant.KOREAN  -> "조니"
        }
        TenantContext.withTenant(tenant) {
            val movies = movieRepo.searchMovies(mapOf("producerName" to producerName))
            movies.shouldNotBeEmpty()
            movies.forEach { log.debug { "tenant=${tenant.id}, movie=$it" } }
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `모든 영화와 출연 배우를 조인하여 조회한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val moviesWithActors = movieRepo.getAllMoviesWithActors()
            moviesWithActors.shouldNotBeEmpty()
            moviesWithActors.forEach {
                log.debug { "tenant=${tenant.id}, movie=${it.name}, actors=${it.actors.size}" }
                it.actors.shouldNotBeEmpty()
            }
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `특정 영화 id로 배우 목록을 함께 조회한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val movieWithActors = movieRepo.getMovieWithActors(1L)
            movieWithActors.shouldNotBeNull()
            movieWithActors.actors.shouldNotBeEmpty()
            log.debug { "tenant=${tenant.id}, movie=${movieWithActors.name}, actorCount=${movieWithActors.actors.size}" }
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `영화별 출연 배우 수를 집계한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val counts = movieRepo.getMovieActorsCount()
            counts.shouldNotBeEmpty()
            counts.forEach {
                log.debug { "tenant=${tenant.id}, movie=${it.movieName}, actorCount=${it.actorCount}" }
            }
            counts shouldHaveSize 4
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `제작에 참여한 배우가 있는 영화를 조회한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val results = movieRepo.findMoviesWithActingProducers()
            results.shouldNotBeEmpty()
            results.forEach {
                log.debug { "tenant=${tenant.id}, movieWithProducingActor=$it" }
            }
        }
    }

    @ParameterizedTest(name = "tenant={0}")
    @EnumSource(Tenant::class)
    fun `존재하지 않는 영화 id 조회 시 null을 반환한다`(tenant: Tenant) {
        TenantContext.withTenant(tenant) {
            val movie = movieRepo.findByIdOrNull(-1L)
            movie.shouldBeNull()
        }
    }
}
