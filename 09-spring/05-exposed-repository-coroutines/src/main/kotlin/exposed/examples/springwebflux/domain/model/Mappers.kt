package exposed.examples.springwebflux.domain.model

import exposed.examples.springwebflux.domain.dtos.ActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithProducingActorDTO
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorEntity
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieEntity
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieTable
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
    actors = actors.toMutableList(),
)

fun MovieDTO.toMovieWithActorDTO(actors: Collection<ActorDTO>) = MovieWithActorDTO(
    id = this.id,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toMutableList(),
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
    actors = this.actors.map { it.toActorDTO() }.toMutableList(),
)


fun ResultRow.toMovieWithProducingActorDTO() = MovieWithProducingActorDTO(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
