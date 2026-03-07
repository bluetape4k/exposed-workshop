package exposed.shared.repository.model


import exposed.shared.repository.model.MovieSchema.ActorTable
import exposed.shared.repository.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow


/**
 * [ResultRow]를 [ActorRecord]로 변환하는 확장 함수.
 *
 * [ActorTable]의 컬럼 값을 읽어 [ActorRecord] 데이터 클래스로 매핑합니다.
 *
 * @return 변환된 [ActorRecord] 인스턴스
 */
fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

/**
 * [MovieSchema.ActorEntity] DAO 엔티티를 [ActorRecord]로 변환하는 확장 함수.
 *
 * DAO 엔티티의 프로퍼티를 읽어 [ActorRecord] 데이터 클래스로 매핑합니다.
 *
 * @return 변환된 [ActorRecord] 인스턴스
 */
fun MovieSchema.ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

/**
 * [ResultRow]를 [MovieRecord]로 변환하는 확장 함수.
 *
 * [MovieTable]의 컬럼 값을 읽어 [MovieRecord] 데이터 클래스로 매핑합니다.
 *
 * @return 변환된 [MovieRecord] 인스턴스
 */
fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    id = this[MovieTable.id].value
)

/**
 * [ResultRow]를 배우 목록과 함께 [MovieWithActorRecord]로 변환하는 확장 함수.
 *
 * [MovieTable]의 컬럼 값과 별도로 조회한 배우 목록을 조합하여 [MovieWithActorRecord]를 생성합니다.
 *
 * @param actors 이 영화에 출연한 배우 목록
 * @return 변환된 [MovieWithActorRecord] 인스턴스
 */
fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors.toList(),
    id = this[MovieTable.id].value
)

/**
 * [MovieRecord]를 배우 목록과 함께 [MovieWithActorRecord]로 변환하는 확장 함수.
 *
 * 기존 [MovieRecord]의 기본 정보에 배우 목록을 추가하여 [MovieWithActorRecord]를 생성합니다.
 *
 * @param actors 이 영화에 출연한 배우 목록
 * @return 변환된 [MovieWithActorRecord] 인스턴스
 */
fun MovieRecord.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toList(),
    id = this.id
)

/**
 * [MovieSchema.MovieEntity] DAO 엔티티를 [MovieRecord]로 변환하는 확장 함수.
 *
 * DAO 엔티티의 프로퍼티를 읽어 [MovieRecord] 데이터 클래스로 매핑합니다.
 * 배우 정보는 포함되지 않습니다.
 *
 * @return 변환된 [MovieRecord] 인스턴스
 */
fun MovieSchema.MovieEntity.toMovieRecord() = MovieRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

/**
 * [MovieSchema.MovieEntity] DAO 엔티티를 배우 목록을 포함한 [MovieWithActorRecord]로 변환하는 확장 함수.
 *
 * DAO 엔티티의 `actors` 연관 관계를 함께 로드하여 [MovieWithActorRecord]를 생성합니다.
 * 이 함수는 N+1 쿼리 문제를 유발할 수 있으므로 주의가 필요합니다.
 *
 * @return 배우 목록이 포함된 [MovieWithActorRecord] 인스턴스
 */
fun MovieSchema.MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toList(),
    id = this.id.value
)


/**
 * [ResultRow]를 영화-제작 배우 정보를 담은 [MovieWithProducingActorRecord]로 변환하는 확장 함수.
 *
 * [MovieTable]과 [ActorTable]을 조인한 결과 행에서 영화 제목과 제작 배우 이름을 추출합니다.
 *
 * @return 변환된 [MovieWithProducingActorRecord] 인스턴스
 */
fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
