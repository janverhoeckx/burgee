package io.github.janverhoeckx.burgee.security

import io.github.janverhoeckx.burgee.user.application.port.inbound.FindUserBySubjectUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.ResolveOrProvisionUserUseCase
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(AuthProperties::class, JwtProperties::class, AdminProperties::class)
class SecurityConfig(
    private val authProperties: AuthProperties,
    private val jwtProperties: JwtProperties,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @ConditionalOnProperty(name = ["burgee.auth.method"], havingValue = "basic", matchIfMissing = true)
    fun userDetailsService(findUserBySubject: FindUserBySubjectUseCase): UserDetailsService =
        UserDetailsService { username ->
            val user = findUserBySubject.findBySubject(username)
                ?: throw UsernameNotFoundException("Unknown user '$username'")
            val passwordHash = user.passwordHash
                ?: throw UsernameNotFoundException("User '$username' cannot sign in with a password")
            User.builder()
                .username(user.subject)
                .password(passwordHash)
                .authorities(SimpleGrantedAuthority(user.role.authority))
                .build()
        }

    @Bean
    @ConditionalOnProperty(name = ["burgee.auth.method"], havingValue = "jwt")
    fun jwtDecoder(): JwtDecoder {
        val resourceUri = jwtProperties.resourceUri
        val decoder = NimbusJwtDecoder.withIssuerLocation(jwtProperties.issuerUri).build()
        decoder.setJwtValidator(
            JwtValidators.createAtJwtValidator()
                .issuer(jwtProperties.issuerUri)
                .audience(resourceUri)
                .clientId(jwtProperties.clientId)
                .build(),
        )
        return decoder
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        provisionUser: ResolveOrProvisionUserUseCase,
    ): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                auth.requestMatchers("/api/v1/flags/**").permitAll()
                auth.requestMatchers("/api/auth/info").permitAll()
                auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                auth.requestMatchers("/api/admin/**").hasRole("ADMIN")
                auth.requestMatchers("/api/auth/**").authenticated()
                auth.requestMatchers("/api/**").denyAll()
                auth.anyRequest().permitAll()
            }

        when (authProperties.method) {
            AuthProperties.Method.BASIC -> configureBasicAuth(http)
            AuthProperties.Method.JWT -> configureJwt(http, provisionUser)
        }

        return http.build()
    }

    private fun configureBasicAuth(http: HttpSecurity) {
        http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic { }
            .formLogin { it.disable() }
            .logout { it.disable() }
    }

    private fun configureJwt(http: HttpSecurity, provisionUser: ResolveOrProvisionUserUseCase) {
        http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(provisioningJwtAuthenticationConverter(provisionUser))
                }
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
    }

    private fun provisioningJwtAuthenticationConverter(
        provisionUser: ResolveOrProvisionUserUseCase,
    ): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val user = provisionUser.resolveOrProvision(
                ResolveOrProvisionUserUseCase.Command(
                    subject = jwt.subject,
                    provider = IdentityProvider.JWT,
                    email = jwt.getClaimAsString("email"),
                    displayName = jwt.getClaimAsString("name"),
                ),
            )
            listOf(SimpleGrantedAuthority(user.role.authority))
        }
        return converter
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
