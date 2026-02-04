package exposed.workshop.springwebflux.domain.model

import io.bluetape4k.collections.eclipse.toFastList
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorRecord() = ActorRecord(
    id = this[MovieSchema.ActorTable.id].value,
    firstName = this[MovieSchema.ActorTable.firstName],
    lastName = this[MovieSchema.ActorTable.lastName],
    birthday = this[MovieSchema.ActorTable.birthday]?.toString()
)

fun MovieSchema.ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

fun ResultRow.toMovieRecord() = MovieRecord(
    id = this[MovieSchema.MovieTable.id].value,
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
)

fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    id = this[MovieSchema.MovieTable.id].value,
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    actors = actors.toFastList(),
)

fun MovieRecord.toMovieWithActorRecord(actors: Collection<ActorRecord>) = MovieWithActorRecord(
    id = this.id,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toFastList(),
)

fun MovieSchema.MovieEntity.toMovieRecord() = MovieRecord(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
)

fun MovieSchema.MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toFastList(),
)


fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieSchema.MovieTable.name],
    producerActorName = this[MovieSchema.ActorTable.firstName] + " " + this[MovieSchema.ActorTable.lastName]
)
