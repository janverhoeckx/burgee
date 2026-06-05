package io.github.janverhoeckx.burgee.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "burgee.auth")
data class AuthProperties(
    val method: Method = Method.BASIC,
) {
    enum class Method { BASIC, JWT }
}
