package exposed.examples.spring.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Exposed JDBC를 사용하는 Spring Boot 4 인증 및 세션 메타데이터 예제이다.
 *
 * ## 계약
 * - Spring Security는 데이터베이스에서 로드한 사용자를 인증한다.
 * - 권한 검사는 user와 admin 역할을 구분한다.
 * - 세션/토큰 메타데이터는 HTTP Basic 인증 정보와 분리해 저장한다.
 */
@SpringBootApplication
class SpringAuthSessionApplication

fun main(args: Array<String>) {
    runApplication<SpringAuthSessionApplication>(*args)
}
