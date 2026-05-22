package exposed.multitenant.security.config

import exposed.multitenant.security.security.CredentialConflictFilter
import exposed.multitenant.security.security.DemoApiKeyAuthenticationFilter
import exposed.multitenant.security.security.DemoJwtDecoder
import exposed.multitenant.security.security.DemoSessionAuthenticationFilter
import exposed.multitenant.security.security.TenantAuthenticationResolver
import exposed.multitenant.security.security.TenantAuthorizationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
internal class SecurityConfiguration {

    @Bean
    fun jwtDecoder(): JwtDecoder =
        DemoJwtDecoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val tenantResolver = TenantAuthenticationResolver()
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
            .addFilterBefore(CredentialConflictFilter(), BearerTokenAuthenticationFilter::class.java)
            .addFilterBefore(DemoApiKeyAuthenticationFilter(), BearerTokenAuthenticationFilter::class.java)
            .addFilterBefore(DemoSessionAuthenticationFilter(), BearerTokenAuthenticationFilter::class.java)
            .addFilterAfter(TenantAuthorizationFilter(tenantResolver), AuthorizationFilter::class.java)
        return http.build()
    }
}
