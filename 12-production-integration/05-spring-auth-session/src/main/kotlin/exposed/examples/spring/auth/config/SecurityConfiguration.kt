package exposed.examples.spring.auth.config

import exposed.examples.spring.auth.repository.AuthRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
internal class SecurityConfiguration {

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(repository: AuthRepository): UserDetailsService =
        UserDetailsService { username ->
            val account = repository.findUser(username)
                ?: throw UsernameNotFoundException(username)
            User.withUsername(account.username)
                .password(account.passwordHash)
                .roles(*account.roles.toTypedArray())
                .build()
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.GET, "/api/public").permitAll()
                    .requestMatchers("/api/admin").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
        return http.build()
    }
}
