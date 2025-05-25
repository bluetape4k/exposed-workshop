package exposed.multitenant.springweb.domain.dtos

import exposed.multitenant.springweb.domain.model.MovieSchema.ActorEntity
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import exposed.multitenant.springweb.domain.model.MovieSchema.MovieEntity
import exposed.multitenant.springweb.domain.model.MovieSchema.MovieTable
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
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    id = this[MovieTable.id].value
)

fun ResultRow.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors.toMutableList(),
    id = this[MovieTable.id].value
)

fun MovieDTO.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toMutableList(),
    id = this.id
)

fun MovieEntity.toMovieDTO() = MovieDTO(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

fun MovieEntity.toMovieWithActorDTO() = MovieWithActorDTO(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorDTO() }.toMutableList(),
    id = this.id.value
)


fun ResultRow.toMovieWithProducingActorDTO() = MovieWithProducingActorDTO(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
