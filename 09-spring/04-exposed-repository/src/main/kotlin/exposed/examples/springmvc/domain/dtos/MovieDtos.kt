package exposed.examples.springmvc.domain.dtos

import io.bluetape4k.exposed.dao.HasIdentifier
import java.io.Serializable

/**
 * 영화 정보를 나타내는 DTO
 */
data class MovieDTO(
    override val id: Long = 0L,
    val name: String,
    val producerName: String,
    val releaseDate: String,
): HasIdentifier<Long>

/**
 * 영화 배우 정보를 담는 DTO
 */
data class ActorDTO(
    override val id: Long = 0L,
    val firstName: String,
    val lastName: String,
    val birthday: String? = null,

    ): HasIdentifier<Long>

/**
 * 영화 배우 정보와 해당 배우가 출연한 영화 정보를 나타내는 DTO
 */
data class MovieActorDTO(
    val movieId: Long,
    val actorId: Long,
): Serializable


/**
 * 영화 제목과 영화에 출연한 배우의 수를 나타내는 DTO
 */
data class MovieActorCountDTO(
    val movieName: String,
    val actorCount: Int,
): Serializable


/**
 * 영화 정보와 해당 영화에 출연한 배우 정보를 나타내는 DTO
 */
data class MovieWithActorDTO(
    override val id: Long = 0L,
    val name: String,
    val producerName: String,
    val releaseDate: String,
    val actors: MutableList<ActorDTO> = mutableListOf(),
): HasIdentifier<Long>


/**
 * 영화 제목과 영화를 제작한 배우의 이름을 나타내는 DTO
 */
data class MovieWithProducingActorDTO(
    val movieName: String,
    val producerActorName: String,
): Serializable
