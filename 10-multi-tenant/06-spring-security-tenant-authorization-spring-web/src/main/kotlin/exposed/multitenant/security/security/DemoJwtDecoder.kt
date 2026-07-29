package exposed.multitenant.security.security

import java.time.Instant
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

/**
 * 이 워크숍 예제에서만 사용하는 고정 토큰 JWT decoder이다.
 *
 * 이 decoder는 의도적으로 서명 없는 `alg=none` 토큰을 발급하며,
 * 운영 환경 수준의 토큰 검증을 수행하지 않는다. 운영 resource server에 복사해서 사용하면 안 된다.
 * 실제 서비스는 서명, issuer, audience, expiry, key rotation을
 * 신뢰할 수 있는 authorization server 또는 JWT 인프라를 통해 검증해야 한다.
 */
internal class DemoJwtDecoder : JwtDecoder {

    override fun decode(token: String): Jwt {
        val claims = tokenClaims[token] ?: throw JwtException("Unsupported demo token")
        val now = Instant.now()
        return Jwt(
            token,
            now,
            now.plusSeconds(300),
            mapOf("alg" to "none"),
            claims + mapOf("sub" to "demo-user"),
        )
    }

    companion object {
        private val tokenClaims: Map<String, Map<String, Any>> = mapOf(
            "demo-acme-token" to mapOf("tenant_id" to "acme"),
            "demo-globex-token" to mapOf("tenant_id" to "globex"),
            "demo-no-tenant-token" to emptyMap(),
            "demo-unknown-tenant-token" to mapOf("tenant_id" to "initech"),
            "demo-acme-upper-token" to mapOf("tenant_id" to " ACME "),
            "demo-malformed-tenant-token" to mapOf("tenant_id" to "acme,globex"),
            "demo-non-string-tenant-token" to mapOf("tenant_id" to 100),
        )
    }
}
