package exposed.multitenant.security.security

import java.time.Instant
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

/**
 * Fixed-token JWT decoder for this workshop example.
 *
 * This decoder intentionally emits unsigned `alg=none` tokens and performs no
 * production token validation. Do not copy it into production resource servers.
 * Real services must validate signatures, issuer, audience, expiry, and key
 * rotation through a real authorization server or trusted JWT infrastructure.
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
