package exposed.examples.springmvc.domain.dtos

import exposed.examples.springmvc.domain.model.MovieSchema
import exposed.examples.springmvc.domain.model.MovieSchema.ActorEntity
import exposed.examples.springmvc.domain.model.MovieSchema.ActorTable
import exposed.examples.springmvc.domain.model.MovieSchema.MovieEntity
import org.jetbrains.exposed.sql.ResultRow


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
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    id = this[MovieSchema.MovieTable.id].value
)

fun ResultRow.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    actors = actors.toMutableList(),
    id = this[MovieSchema.MovieTable.id].value
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
    movieName = this[MovieSchema.MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
