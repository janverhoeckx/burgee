package io.github.janverhoeckx.burgee.security

import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authProperties: AuthProperties,
    private val firebaseProperties: FirebaseProperties,
    private val clientRegistrations: ObjectProvider<ClientRegistrationRepository>,
) {

    @GetMapping("/info")
    fun info(): AuthInfoResponse {
        val providers = if (authProperties.method == AuthProperties.Method.OAUTH2) {
            val repo = clientRegistrations.ifAvailable
            if (repo is InMemoryClientRegistrationRepository) {
                repo.map { reg ->
                    OAuthProviderInfo(
                        id = reg.registrationId,
                        name = reg.clientName,
                        loginUrl = "/oauth2/authorization/${reg.registrationId}",
                    )
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
        val firebase = if (authProperties.method == AuthProperties.Method.FIREBASE) {
            FirebaseClientConfig(
                apiKey = firebaseProperties.apiKey,
                authDomain = firebaseProperties.authDomain,
                projectId = firebaseProperties.projectId,
            )
        } else {
            null
        }
        return AuthInfoResponse(
            method = authProperties.method.name.lowercase(),
            providers = providers,
            firebase = firebase,
        )
    }

    @GetMapping("/user")
    fun user(authentication: Authentication?): ResponseEntity<UserInfoResponse> {
        if (authentication == null || !authentication.isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(UserInfoResponse(name = authentication.resolveUsername()))
    }
}

data class AuthInfoResponse(
    val method: String,
    val providers: List<OAuthProviderInfo> = emptyList(),
    val firebase: FirebaseClientConfig? = null,
)

data class OAuthProviderInfo(
    val id: String,
    val name: String,
    val loginUrl: String,
)

data class FirebaseClientConfig(
    val apiKey: String,
    val authDomain: String,
    val projectId: String,
)

data class UserInfoResponse(
    val name: String,
)
