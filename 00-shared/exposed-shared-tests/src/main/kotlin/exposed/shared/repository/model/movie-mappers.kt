package exposed.shared.repository.model


import exposed.shared.repository.model.MovieSchema.ActorTable
import exposed.shared.repository.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow


/**
 * [ResultRow]를 [ActorRecord]로 변환합니다.
 *
 * @return 배우 정보를 담은 [ActorRecord]
 */
fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

/**
 * [MovieSchema.ActorEntity]를 [ActorRecord]로 변환합니다.
 *
 * @return 배우 정보를 담은 [ActorRecord]
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
 * @return 영화 정보를 담은 [MovieRecord]
 */
fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    id = this[MovieTable.id].value
)

/**
 * [ResultRow]와 배우 목록을 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 목록
 * @return 영화와 배우 정보를 담은 [MovieWithActorRecord]
 */
fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors.toList(),
    id = this[MovieTable.id].value
)

/**
 * [MovieRecord]와 배우 목록을 [MovieWithActorRecord]로 변환합니다.
 *
 * @param actors 영화에 출연한 배우 목록
 * @return 영화와 배우 정보를 담은 [MovieWithActorRecord]
 */
fun MovieRecord.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toList(),
    id = this.id
)

/**
 * [MovieSchema.MovieEntity]를 [MovieRecord]로 변환합니다.
 *
 * @return 영화 정보를 담은 [MovieRecord]
 */
fun MovieSchema.MovieEntity.toMovieRecord() = MovieRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

/**
 * [MovieSchema.MovieEntity]를 배우 목록을 포함한 [MovieWithActorRecord]로 변환합니다.
 *
 * @return 영화와 배우 정보를 담은 [MovieWithActorRecord]
 */
fun MovieSchema.MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
    id = this.id.value
)


/**
 * [ResultRow]를 제작자 배우 정보를 포함한 [MovieWithProducingActorRecord]로 변환합니다.
 *
 * @return 영화명과 제작자 배우명을 담은 [MovieWithProducingActorRecord]
 */
fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
