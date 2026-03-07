package exposed.workshop.springmvc.domain.model

import org.jetbrains.exposed.v1.core.ResultRow

/**
 * Exposed [ResultRow]를 [ActorRecord]로 변환합니다.
 *
 * [MovieSchema.ActorTable]의 컬럼 값을 읽어 데이터 레코드를 생성합니다.
 *
 * @return 변환된 [ActorRecord]
 */
fun ResultRow.toActorRecord() = ActorRecord(
    id = this[MovieSchema.ActorTable.id].value,
    firstName = this[MovieSchema.ActorTable.firstName],
    lastName = this[MovieSchema.ActorTable.lastName],
    birthday = this[MovieSchema.ActorTable.birthday]?.toString()
)

/**
 * Exposed DAO [MovieSchema.ActorEntity]를 [ActorRecord]로 변환합니다.
 *
 * @return 변환된 [ActorRecord]
 */
fun MovieSchema.ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

/**
 * Exposed [ResultRow]를 [MovieRecord]로 변환합니다.
 *
 * [MovieSchema.MovieTable]의 컬럼 값을 읽어 데이터 레코드를 생성합니다.
 *
 * @return 변환된 [MovieRecord]
 */
fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    id = this[MovieSchema.MovieTable.id].value
)

/**
 * Exposed [ResultRow]를 배우 목록과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 목록
 * @return 배우 목록이 포함된 [MovieWithActorRecord]
 */
fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    actors = actors.toList(),
    id = this[MovieSchema.MovieTable.id].value
)

/**
 * [MovieRecord]를 배우 목록과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 컬렉션
 * @return 배우 목록이 포함된 [MovieWithActorRecord]
 */
fun MovieRecord.toMovieWithActorRecord(actors: Collection<ActorRecord>) = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toList(),
    id = this.id
)

/**
 * Exposed DAO [MovieSchema.MovieEntity]를 [MovieRecord]로 변환합니다.
 *
 * @return 변환된 [MovieRecord]
 */
fun MovieSchema.MovieEntity.toMovieRecord() = MovieRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

/**
 * Exposed DAO [MovieSchema.MovieEntity]를 배우 목록과 함께 [MovieWithActorRecord]로 변환합니다.
 *
 * 엔티티의 연관된 배우 목록을 함께 매핑합니다.
 *
 * @return 배우 목록이 포함된 [MovieWithActorRecord]
 */
fun MovieSchema.MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
    id = this.id.value
)

/**
 * Exposed [ResultRow]를 [MovieWithProducingActorRecord]로 변환합니다.
 *
 * 영화 이름과 프로듀서 겸 배우의 전체 이름을 결합합니다.
 *
 * @return 변환된 [MovieWithProducingActorRecord]
 */
fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieSchema.MovieTable.name],
    producerActorName = this[MovieSchema.ActorTable.firstName] + " " + this[MovieSchema.ActorTable.lastName]
)
