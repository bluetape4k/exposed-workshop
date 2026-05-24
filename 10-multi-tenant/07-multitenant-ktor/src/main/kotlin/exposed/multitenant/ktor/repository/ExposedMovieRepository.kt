package exposed.multitenant.ktor.repository

import exposed.multitenant.ktor.model.CreateMovieRequest
import exposed.multitenant.ktor.model.MovieResponse
import exposed.multitenant.ktor.persistence.Movies
import exposed.multitenant.ktor.tenant.TenantContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedMovieRepository(
    private val database: Database,
) {
    fun findAll(): List<MovieResponse> = tenantTransaction {
        Movies.selectAll()
            .orderBy(Movies.id)
            .map {
                MovieResponse(
                    id = it[Movies.id].value,
                    title = it[Movies.title],
                    tenant = TenantContext.currentTenant().id,
                )
            }
    }

    fun create(request: CreateMovieRequest): MovieResponse = tenantTransaction {
        val normalizedTitle = request.title.trim()
        require(normalizedTitle.isNotBlank()) { "title must not be blank" }

        val id = Movies.insertAndGetId {
            it[title] = normalizedTitle
        }

        MovieResponse(
            id = id.value,
            title = normalizedTitle,
            tenant = TenantContext.currentTenant().id,
        )
    }

    private fun <T> tenantTransaction(block: () -> T): T {
        val tenant = TenantContext.currentTenant()
        return transaction(database) {
            exec("SET SCHEMA ${tenant.schema}")
            block()
        }
    }
}
