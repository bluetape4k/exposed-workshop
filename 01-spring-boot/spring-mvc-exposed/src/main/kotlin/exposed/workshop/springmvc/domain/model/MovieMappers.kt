package exposed.workshop.springmvc.domain.model

import org.jetbrains.exposed.v1.core.ResultRow

/**
 * [ResultRow]를 [ActorRecord]로 변환합니다.
 *
 * @return 변환된 배우 레코드
 */
fun ResultRow.toActorRecord() = ActorRecord(
    id = this[MovieSchema.ActorTable.id].value,
    firstName = this[MovieSchema.ActorTable.firstName],
    lastName = this[MovieSchema.ActorTable.lastName],
    birthday = this[MovieSchema.ActorTable.birthday]?.toString()
)

/**
 * [MovieSchema.ActorEntity]를 [ActorRecord]로 변환합니다.
 *
 * @return 변환된 배우 레코드
 */
fun MovieSchema.ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

/**
 * [ResultRow]를 [MovieRecord]로 변환합니다.
 *
 * @return 변환된 영화 레코드
 */
fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    id = this[MovieSchema.MovieTable.id].value
)

/**
 * [ResultRow]를 배우 목록과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 목록
 * @return 배우 정보가 포함된 영화 레코드
 */
fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    actors = actors.toList(),
    id = this[MovieSchema.MovieTable.id].value
)

/**
 * [MovieRecord]를 배우 컬렉션과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 컬렉션
 * @return 배우 정보가 포함된 영화 레코드
 */
fun MovieRecord.toMovieWithActorRecord(actors: Collection<ActorRecord>) = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toList(),
    id = this.id
)

/**
 * [MovieSchema.MovieEntity]를 [MovieRecord]로 변환합니다.
 *
 * @return 변환된 영화 레코드
 */
fun MovieSchema.MovieEntity.toMovieRecord() = MovieRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

/**
 * [MovieSchema.MovieEntity]를 출연 배우 정보와 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @return 배우 목록이 포함된 영화 레코드
 */
fun MovieSchema.MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
    id = this.id.value
)

/**
 * [ResultRow]를 [MovieWithProducingActorRecord]로 변환합니다.
 *
 * 제작자가 직접 배우로 출연한 경우의 영화-배우 정보를 담습니다.
 *
 * @return 제작자 겸 배우 정보가 포함된 레코드
 */
fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieSchema.MovieTable.name],
    producerActorName = this[MovieSchema.ActorTable.firstName] + " " + this[MovieSchema.ActorTable.lastName]
)
