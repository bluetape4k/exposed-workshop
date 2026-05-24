package exposed.multitenant.ktor.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Movies: LongIdTable("movies") {
    val title = varchar("title", 120)
}
