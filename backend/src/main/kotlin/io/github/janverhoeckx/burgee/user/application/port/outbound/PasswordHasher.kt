package io.github.janverhoeckx.burgee.user.application.port.outbound

interface PasswordHasher {
    fun hash(rawPassword: String): String
}
