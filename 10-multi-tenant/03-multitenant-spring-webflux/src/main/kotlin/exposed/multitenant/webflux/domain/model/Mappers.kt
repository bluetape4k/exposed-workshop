package exposed.multitenant.webflux.domain.model

import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.domain.dtos.MovieDTO
import exposed.multitenant.webflux.domain.dtos.MovieWithActorDTO
import exposed.multitenant.webflux.domain.dtos.MovieWithProducingActorDTO
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorEntity
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieEntity
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieTable
import io.bluetape4k.collections.eclipse.toFastList
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorDTO() = ActorDTO(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

fun ActorEntity.toActorDTO() = ActorDTO(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

fun ResultRow.toMovieDTO() = MovieDTO(
    id = this[MovieTable.id].value,
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
)

fun ResultRow.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    id = this[MovieTable.id].value,
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors.toFastList(),
)

fun MovieDTO.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    id = this.id,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toFastList(),
)

fun MovieEntity.toMovieDTO() = MovieDTO(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
)

fun MovieEntity.toMovieWithActorDTO() = MovieWithActorDTO(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorDTO() }.toFastList(),
)


fun ResultRow.toMovieWithProducingActorDTO() = MovieWithProducingActorDTO(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
