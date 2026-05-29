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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(AuthProperties::class, FirebaseProperties::class, AdminProperties::class)
class SecurityConfig(
    private val authProperties: AuthProperties,
    private val firebaseProperties: FirebaseProperties,
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
    @ConditionalOnProperty(name = ["burgee.auth.method"], havingValue = "firebase")
    fun jwtDecoder(): JwtDecoder {
        return JwtDecoders.fromIssuerLocation("https://securetoken.google.com/${firebaseProperties.projectId}")
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
            AuthProperties.Method.OAUTH2 -> configureOAuth2(http, provisionUser)
            AuthProperties.Method.FIREBASE -> configureFirebase(http, provisionUser)
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

    private fun configureOAuth2(http: HttpSecurity, provisionUser: ResolveOrProvisionUserUseCase) {
        http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .oauth2Login { oauth2 ->
                oauth2.defaultSuccessUrl("/flags", true)
                oauth2.userInfoEndpoint { it.oidcUserService(provisioningOidcUserService(provisionUser)) }
            }
            .logout { logout ->
                logout.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern("/api/auth/logout"))
                logout.logoutSuccessUrl("/login")
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
    }

    private fun configureFirebase(http: HttpSecurity, provisionUser: ResolveOrProvisionUserUseCase) {
        http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(firebaseJwtAuthenticationConverter(provisionUser))
                }
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
    }

    private fun firebaseJwtAuthenticationConverter(
        provisionUser: ResolveOrProvisionUserUseCase,
    ): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val user = provisionUser.resolveOrProvision(
                ResolveOrProvisionUserUseCase.Command(
                    subject = jwt.subject,
                    provider = IdentityProvider.FIREBASE,
                    email = jwt.getClaimAsString("email"),
                    displayName = jwt.getClaimAsString("name"),
                ),
            )
            listOf(SimpleGrantedAuthority(user.role.authority))
        }
        return converter
    }

    private fun provisioningOidcUserService(
        provisionUser: ResolveOrProvisionUserUseCase,
    ): OAuth2UserService<OidcUserRequest, OidcUser> {
        val delegate = OidcUserService()
        return OAuth2UserService { request ->
            val oidcUser = delegate.loadUser(request)
            val user = provisionUser.resolveOrProvision(
                ResolveOrProvisionUserUseCase.Command(
                    subject = oidcUser.subject,
                    provider = IdentityProvider.OAUTH2,
                    email = oidcUser.email,
                    displayName = oidcUser.fullName,
                ),
            )
            val authorities = mutableSetOf<GrantedAuthority>(SimpleGrantedAuthority(user.role.authority))
            DefaultOidcUser(authorities, oidcUser.idToken, oidcUser.userInfo)
        }
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
