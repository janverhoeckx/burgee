package io.github.janverhoeckx.burgee.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.Jwt

fun Authentication?.resolveUsername(fallback: String = "unknown"): String {
    if (this == null || !isAuthenticated) return fallback
    return when (val principal = principal) {
        is OidcUser -> principal.fullName ?: principal.email ?: principal.subject
        is Jwt -> principal.getClaimAsString("name") ?: principal.getClaimAsString("email") ?: principal.subject
        is UserDetails -> principal.username
        else -> name ?: fallback
    }
}
