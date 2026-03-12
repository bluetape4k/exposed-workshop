package exposed.multitenant.springweb.domain.dtos

import exposed.multitenant.springweb.domain.model.MovieSchema.ActorEntity
import exposed.multitenant.springweb.domain.model.MovieSchema.ActorTable
import exposed.multitenant.springweb.domain.model.MovieSchema.MovieEntity
import exposed.multitenant.springweb.domain.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * [ResultRow]를 [ActorRecord]로 변환합니다.
 */
fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

/**
 * [ActorEntity]를 [ActorRecord]로 변환합니다.
 */
fun ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

/**
 * [ResultRow]를 [MovieRecord]로 변환합니다.
 */
fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    id = this[MovieTable.id].value
)

/**
 * [ResultRow]와 배우 목록을 [MovieWithActorRecord]로 변환합니다.
 */
fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) =
    MovieWithActorRecord(
        name = this[MovieTable.name],
        producerName = this[MovieTable.producerName],
        releaseDate = this[MovieTable.releaseDate].toString(),
        actors = actors,
        id = this[MovieTable.id].value
    )

/**
 * [MovieRecord]와 배우 목록을 [MovieWithActorRecord]로 확장합니다.
 */
fun MovieRecord.toMovieWithActorRecord(actors: List<ActorRecord>) =
    MovieWithActorRecord(
        name = this.name,
        producerName = this.producerName,
        releaseDate = this.releaseDate,
        actors = actors,
        id = this.id
    )

/**
 * [MovieEntity]를 [MovieRecord]로 변환합니다.
 */
fun MovieEntity.toMovieRecord() = MovieRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

/**
 * [MovieEntity]를 배우 정보를 포함한 [MovieWithActorRecord]로 변환합니다.
 */
fun MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
    id = this.id.value
)

/**
 * 영화 제목과 제작 배우 이름을 조회하는 결과를 [MovieWithProducingActorRecord]로 변환합니다.
 */
fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
