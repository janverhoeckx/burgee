package io.github.janverhoeckx.burgee.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "burgee.jwt")
data class JwtProperties(
    /** OIDC issuer URI used to validate bearer tokens and to drive the frontend OIDC client. */
    val issuerUri: String = "",
    /** Public client id the SPA uses for the authorization-code (PKCE) flow. */
    val clientId: String = "",
    /** Scopes the SPA requests during login. */
    val scope: String = "openid profile email",
    /** Resource indicator (RFC 8707) sent during login. */
    val resourceUri: String = "",
)
