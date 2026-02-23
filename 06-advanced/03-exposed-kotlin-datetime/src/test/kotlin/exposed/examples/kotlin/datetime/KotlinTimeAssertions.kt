package exposed.examples.kotlin.datetime

import exposed.shared.tests.currentDialectTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.datetime.datetime
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalTime::class)
internal infix fun <T> T.shouldDateTimeEqualTo(d2: T?) {
    val d1 = this
    when {
        d1 == null && d2 == null                     -> return
        d1 == null                                   -> error("d1 is null while d2 is not")
        d2 == null                                   -> error("d1 is not null while d2 is null")
        d1 is LocalTime && d2 is LocalTime           -> {
            d1.toSecondOfDay() shouldBeEqualTo d2.toSecondOfDay()
            if (d2.nanosecond != 0) {
                d1.nanosecond shouldFractionalPartEqualTo d2.nanosecond
            }
        }
        d1 is LocalDateTime && d2 is LocalDateTime   -> {
            d1.toJavaLocalDateTime().toEpochSecond(ZoneOffset.UTC) shouldBeEqualTo d2.toJavaLocalDateTime()
                .toEpochSecond(ZoneOffset.UTC)
            d1.nanosecond shouldFractionalPartEqualTo d2.nanosecond
        }
        d1 is Instant && d2 is Instant               -> {
            d1.epochSeconds shouldBeEqualTo d2.epochSeconds
            d1.nanosecondsOfSecond shouldFractionalPartEqualTo d2.nanosecondsOfSecond
        }
        d1 is OffsetDateTime && d2 is OffsetDateTime -> {
            d1.toLocalDateTime().toKotlinLocalDateTime() shouldDateTimeEqualTo d2.toLocalDateTime()
                .toKotlinLocalDateTime()
            d1.offset shouldBeEqualTo d2.offset
        }
        else                                         -> d1 shouldBeEqualTo d2
    }
}

internal infix fun Int.shouldFractionalPartEqualTo(nano2: Int) {
    val nano1 = this
    val dialect = currentDialectTest
    val db = dialect.name
    when (dialect) {
        // accurate to 100 nanoseconds
        is SQLServerDialect                                 -> nano1.nanoRoundTo100Nanos() shouldBeEqualTo nano2.nanoRoundTo100Nanos()
        // microseconds
        is MariaDBDialect                                   -> nano1.nanoFloorToMicro() shouldBeEqualTo nano2.nanoFloorToMicro()

        is H2Dialect, is PostgreSQLDialect, is MysqlDialect -> {
            when ((dialect as? MysqlDialect)?.isFractionDateTimeSupported()) {
                null, true -> {
                    nano1.nanoRoundToMicro() shouldBeEqualTo nano2.nanoRoundToMicro()
                }
                else       -> {} // don't compare fractional part
            }
        }
        // milliseconds
        is OracleDialect                                    -> nano1.nanoRoundToMilli() shouldBeEqualTo nano2.nanoRoundToMilli()
        is SQLiteDialect                                    -> nano1.nanoFloorToMilli() shouldBeEqualTo nano2.nanoFloorToMilli()
        else                                                -> org.amshove.kluent.fail("Unknown dialect $db")
    }
}

internal fun Int.nanoRoundTo100Nanos(): Int =
    this.toBigDecimal().divide(100.toBigDecimal(), RoundingMode.HALF_UP).toInt()

internal fun Int.nanoRoundToMicro(): Int =
    this.toBigDecimal().divide(1_000.toBigDecimal(), RoundingMode.HALF_UP).toInt()

internal fun Int.nanoRoundToMilli(): Int =
    this.toBigDecimal().divide(1_000_000.toBigDecimal(), RoundingMode.HALF_UP).toInt()

internal fun Int.nanoFloorToMicro(): Int = this / 1_000

internal fun Int.nanoFloorToMilli(): Int = this / 1_000_000

internal val today: LocalDate = now().date

/**
 * ```sql
 * CREATE TABLE IF NOT EXISTS CITIESTIME (
 *      ID INT AUTO_INCREMENT PRIMARY KEY,
 *      "name" VARCHAR(50) NOT NULL,
 *      LOCAL_TIME DATETIME(9) NULL
 * )
 * ```
 */
object CitiesTime: IntIdTable("CitiesTime") {
    val name: Column<String> = varchar("name", 50) // Column<String>
    val local_time: Column<LocalDateTime?> = datetime("local_time").nullable() // Column<datetime>
}

/**
 * 변경 이력을 표현하기 위한 JSON 직렬화용 DTO.
 */
@Serializable
data class ModifierData(val userId: Int, val timestamp: LocalDateTime)
