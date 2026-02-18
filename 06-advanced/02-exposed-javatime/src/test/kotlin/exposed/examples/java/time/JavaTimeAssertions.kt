package exposed.examples.java.time

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime


fun Int.nanoRoundTo100Nanos(): Int =
    this.toBigDecimal().divide(100.toBigDecimal(), RoundingMode.HALF_UP).toInt()

fun Int.nanoRoundToMicro(): Int =
    this.toBigDecimal().divide(1_000.toBigDecimal(), RoundingMode.HALF_UP).toInt()

fun Int.nanoRoundToMilli(): Int =
    this.toBigDecimal().divide(1_000_000.toBigDecimal(), RoundingMode.HALF_UP).toInt()

fun Int.nanoFloorToMicro(): Int = this / 1_000

fun Int.nanoFloorToMilli(): Int = this / 1_000_000


val today: LocalDate = LocalDate.now()

/**
 * ```sql
 * -- Postgres
 * CREATE TABLE IF NOT EXISTS citiestime (
 *      id SERIAL PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL,
 *      local_time TIMESTAMP NULL
 * );
 * ```
 */
object CitiesTime: IntIdTable("CitiesTime") {
    val name: Column<String> = varchar("name", 50)
    val local_time: Column<LocalDateTime?> = datetime("local_time").nullable()
}

@Serializable
data class ModifierData(
    val userId: Int,
    @Serializable(with = DateTimeSerializer::class)
    val timestamp: LocalDateTime,
)

/**
 * [LocalDateTime]을 Kotlinx Serialization 으로 직렬화하기 위한 Serializer
 */
object DateTimeSerializer: KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }
}
