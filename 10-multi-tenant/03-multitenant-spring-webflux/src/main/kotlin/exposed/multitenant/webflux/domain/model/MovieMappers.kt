package exposed.multitenant.webflux.domain.model

import exposed.multitenant.webflux.domain.model.MovieSchema.ActorEntity
import exposed.multitenant.webflux.domain.model.MovieSchema.ActorTable
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieEntity
import exposed.multitenant.webflux.domain.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

fun ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

fun ResultRow.toMovieRecord() = MovieRecord(
    id = this[MovieTable.id].value,
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
)

fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    id = this[MovieTable.id].value,
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors,
)

fun MovieRecord.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    id = this.id,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors,
)

fun MovieEntity.toMovieRecord() = MovieRecord(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
)

fun MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
)


fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
