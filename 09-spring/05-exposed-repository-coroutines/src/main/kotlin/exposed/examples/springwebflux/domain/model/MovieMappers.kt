package exposed.examples.springwebflux.domain.model

import exposed.examples.springwebflux.domain.model.MovieSchema.ActorEntity
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieEntity
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * [ResultRow]를 [ActorRecord]로 변환합니다.
 *
 * @return 변환된 배우 레코드
 */
fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

/**
 * [ActorEntity]를 [ActorRecord]로 변환합니다.
 *
 * @return 변환된 배우 레코드
 */
fun ActorEntity.toActorRecord() = ActorRecord(
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
    id = this[MovieTable.id].value,
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
)

/**
 * [ResultRow]를 배우 목록과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 레코드 목록
 * @return 배우 목록이 포함된 영화 레코드
 */
fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) =
    MovieWithActorRecord(
        id = this[MovieTable.id].value,
        name = this[MovieTable.name],
        producerName = this[MovieTable.producerName],
        releaseDate = this[MovieTable.releaseDate].toString(),
        actors = actors,
    )

/**
 * [MovieRecord]를 배우 컬렉션과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 레코드 컬렉션
 * @return 배우 목록이 포함된 영화 레코드
 */
fun MovieRecord.toMovieWithActorRecord(actors: Collection<ActorRecord>) =
    MovieWithActorRecord(
        id = this.id,
        name = this.name,
        producerName = this.producerName,
        releaseDate = this.releaseDate,
        actors = actors.toList(),
    )

/**
 * [MovieEntity]를 [MovieRecord]로 변환합니다.
 *
 * @return 변환된 영화 레코드
 */
fun MovieEntity.toMovieRecord() = MovieRecord(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
)

/**
 * [MovieEntity]를 출연 배우 목록과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @return 배우 목록이 포함된 영화 레코드
 */
fun MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    id = this.id.value,
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
)

/**
 * [ResultRow]를 제작자 겸 배우 정보가 포함된 [MovieWithProducingActorRecord]로 변환합니다.
 *
 * @return 제작자 겸 배우 정보가 포함된 영화 레코드
 */
fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
