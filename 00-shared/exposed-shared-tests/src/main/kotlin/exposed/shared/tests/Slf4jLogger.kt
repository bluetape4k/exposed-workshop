package exposed.shared.tests

import io.bluetape4k.logging.KotlinLogging
import org.slf4j.Logger

/**
 * 이 패키지 내에서 공용으로 사용하는 SLF4J 로거 인스턴스.
 *
 * [KotlinLogging]을 통해 지연 초기화되며, Exposed 테스트 인프라의
 * 로그 출력에 사용됩니다.
 */
internal val logger: Logger by lazy { KotlinLogging.logger { } }
