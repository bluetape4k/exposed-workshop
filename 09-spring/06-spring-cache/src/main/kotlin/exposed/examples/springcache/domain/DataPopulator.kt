package exposed.examples.springcache.domain

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * 애플리케이션 시작 시 국가 데이터를 데이터베이스에 초기화하는 컴포넌트.
 *
 * [ApplicationReadyEvent] 이벤트를 수신하여 [CountryTable]에 국가 코드 데이터를
 * 일괄 삽입(batch insert)합니다.
 */
@Component
class DataPopulator: ApplicationListener<ApplicationReadyEvent> {

    companion object: KLogging() {
        /** ISO 3166-1 alpha-2 형식의 국가 코드 목록 */
        val COUNTRY_CODES: List<String> =
            listOf(
                "AF", "AX",
                "AL", "DZ", "AS", "AD", "AO", "AI", "AQ", "AG", "AR", "AM", "AW", "AU", "AT",
                "AZ", "BS", "BH", "BD", "BB", "BY", "BE", "BZ", "BJ", "BM", "BT", "BO", "BQ",
                "BA", "BW", "BV", "BR", "IO", "BN", "BG", "BF", "BI", "KH", "CM", "CA", "CV",
                "KY", "CF", "TD", "CL", "CN", "CX", "CC", "CO", "KM", "CG", "CD", "CK", "CR",
                "CI", "HR", "CU", "CW", "CY", "CZ", "DK", "DJ", "DM", "DO", "EC", "EG", "SV",
                "GQ", "ER", "EE", "ET", "FK", "FO", "FJ", "FI", "FR", "GF", "PF", "TF", "GA",
                "GM", "GE", "DE", "GH", "GI", "GR", "GL", "GD", "GP", "GU", "GT", "GG", "GN",
                "GW", "GY", "HT", "HM", "VA", "HN", "HK", "HU", "IS", "IN", "ID", "IR", "IQ",
                "IE", "IM", "IL", "IT", "JM", "JP", "JE", "JO", "KZ", "KE", "KI", "KP", "KR",
                "KW", "KG", "LA", "LV", "LB", "LS", "LR", "LY", "LI", "LT", "LU", "MO", "MK",
                "MG", "MW", "MY", "MV", "ML", "MT", "MH", "MQ", "MR", "MU", "YT", "MX", "FM",
                "MD", "MC", "MN", "ME", "MS", "MA", "MZ", "MM", "NA", "NR", "NP", "NL", "NC",
                "NZ", "NI", "NE", "NG", "NU", "NF", "MP", "NO", "OM", "PK", "PW", "PS", "PA",
                "PG", "PY", "PE", "PH", "PN", "PL", "PT", "PR", "QA", "RE", "RO", "RU", "RW",
                "BL", "SH", "KN", "LC", "MF", "PM", "VC", "WS", "SM", "ST", "SA", "SN", "RS",
                "SC", "SL", "SG", "SX", "SK", "SI", "SB", "SO", "ZA", "GS", "SS", "ES", "LK",
                "SD", "SR", "SJ", "SZ", "SE", "CH", "SY", "TW", "TJ", "TZ", "TH", "TL", "TG",
                "TK", "TO", "TT", "TN", "TR", "TM", "TC", "TV", "UG", "UA", "AE", "GB", "US",
                "UM", "UY", "UZ", "VU", "VE", "VN", "VG", "VI", "WF", "EH", "YE", "ZM", "ZW"
            )
    }

    /**
     * 애플리케이션 준비 완료 이벤트 수신 시 국가 데이터를 일괄 삽입합니다.
     *
     * @param event 애플리케이션 준비 완료 이벤트
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        log.info { "Populate country data ..." }
        transaction {
            CountryTable.batchInsert(COUNTRY_CODES, shouldReturnGeneratedValues = false) { code ->
                this[CountryTable.code] = code
                this[CountryTable.name] = "$code Country"
                this[CountryTable.description] = "Country code for $code" + "동해물과 백두산이 마르고 닳도록".repeat(100)
            }
        }
        log.info { "Populate country data done." }
    }
}
