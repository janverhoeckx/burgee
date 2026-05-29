package io.github.janverhoeckx.burgee.user.adapter.outbound.security

import io.github.janverhoeckx.burgee.user.application.port.outbound.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHasher(
    private val encoder: PasswordEncoder,
) : PasswordHasher {
    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)
        ?: error("Password encoder returned no hash")
}
