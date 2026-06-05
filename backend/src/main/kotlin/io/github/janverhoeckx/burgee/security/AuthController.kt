package io.github.janverhoeckx.burgee.security

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authProperties: AuthProperties,
    private val jwtProperties: JwtProperties,
) {

    @GetMapping("/info")
    fun info(): AuthInfoResponse {
        val oidc = if (authProperties.method == AuthProperties.Method.JWT) {
            OidcClientConfig(
                issuerUri = jwtProperties.issuerUri,
                clientId = jwtProperties.clientId,
                scope = jwtProperties.scope,
                resourceUri = jwtProperties.resourceUri.ifBlank { null },
            )
        } else {
            null
        }
        return AuthInfoResponse(
            method = authProperties.method.name.lowercase(),
            oidc = oidc,
        )
    }

    @GetMapping("/user")
    fun user(authentication: Authentication?): ResponseEntity<UserInfoResponse> {
        if (authentication == null || !authentication.isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
        return ResponseEntity.ok(
            UserInfoResponse(
                name = authentication.resolveUsername(),
                role = if (isAdmin) "ADMIN" else "USER",
                isAdmin = isAdmin,
            ),
        )
    }
}

data class AuthInfoResponse(
    val method: String,
    val oidc: OidcClientConfig? = null,
)

data class OidcClientConfig(
    val issuerUri: String,
    val clientId: String,
    val scope: String,
    val resourceUri: String? = null,
)

data class UserInfoResponse(
    val name: String,
    val role: String,
    val isAdmin: Boolean,
)
