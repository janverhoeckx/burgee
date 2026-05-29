package io.github.janverhoeckx.burgee.security

import io.github.janverhoeckx.burgee.AbstractIT
import io.github.janverhoeckx.burgee.user.application.port.outbound.PasswordHasher
import io.github.janverhoeckx.burgee.user.application.port.outbound.UserRepositoryPort
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class AuthControllerIT(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepositoryPort,
    private val passwordHasher: PasswordHasher,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun seedUser(subject: String, password: String): User =
        userRepository.save(
            User.create(
                subject = subject,
                email = "$subject@example.com",
                displayName = "Name $subject",
                role = Role.USER,
                provider = IdentityProvider.BASIC,
                passwordHash = passwordHasher.hash(password),
            ),
        )

    @Test
    fun `GET info returns the basic auth method without credentials`() {
        mockMvc.get("/api/auth/info") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$.method") { value("basic") }
            jsonPath("$.providers") { isEmpty() }
            jsonPath("$.firebase") { doesNotExist() }
        }
    }

    @Test
    fun `GET user returns admin identity for the bootstrap admin`() {
        mockMvc.get("/api/auth/user") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("admin") }
            jsonPath("$.role") { value("ADMIN") }
            jsonPath("$.admin") { value(true) }
        }
    }

    @Test
    fun `GET user returns non-admin identity for a regular user`() {
        val subject = "auth-user-${System.nanoTime()}"
        seedUser(subject, "secret")

        mockMvc.get("/api/auth/user") {
            with(httpBasic(subject, "secret"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value(subject) }
            jsonPath("$.role") { value("USER") }
            jsonPath("$.admin") { value(false) }
        }
    }

    @Test
    fun `GET user without credentials returns 401`() {
        mockMvc.get("/api/auth/user") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }
}
