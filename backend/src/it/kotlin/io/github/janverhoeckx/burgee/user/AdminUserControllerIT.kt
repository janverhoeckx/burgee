package io.github.janverhoeckx.burgee.user

import tools.jackson.databind.ObjectMapper
import io.github.janverhoeckx.burgee.AbstractIT
import io.github.janverhoeckx.burgee.user.adapter.inbound.web.CreateUserRequest
import io.github.janverhoeckx.burgee.user.adapter.inbound.web.UpdateUserRequest
import io.github.janverhoeckx.burgee.user.application.port.outbound.PasswordHasher
import io.github.janverhoeckx.burgee.user.application.port.outbound.UserRepositoryPort
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
class AdminUserControllerIT(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepositoryPort,
    private val passwordHasher: PasswordHasher,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueSubject(suffix: String) = "user-${System.nanoTime()}-$suffix"

    private fun seedUser(
        subject: String,
        role: Role = Role.USER,
        password: String? = null,
    ): User = userRepository.save(
        User.create(
            subject = subject,
            email = "$subject@example.com",
            displayName = "Name $subject",
            role = role,
            provider = IdentityProvider.BASIC,
            passwordHash = password?.let { passwordHasher.hash(it) },
        ),
    )

    @Test
    fun `GET list returns all users including a seeded one`() {
        val subject = uniqueSubject("list")
        seedUser(subject)

        mockMvc.get("/api/admin/users") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.subject == '$subject')].displayName") { value("Name $subject") }
        }
    }

    @Test
    fun `GET list without credentials returns 401`() {
        mockMvc.get("/api/admin/users") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET list as non-admin returns 403`() {
        val subject = uniqueSubject("forbidden-list")
        seedUser(subject, password = "secret")

        mockMvc.get("/api/admin/users") {
            with(httpBasic(subject, "secret"))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `GET by id returns the seeded user`() {
        val subject = uniqueSubject("byid")
        val user = seedUser(subject)

        mockMvc.get("/api/admin/users/${user.id}") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(user.id.toString()) }
            jsonPath("$.subject") { value(subject) }
            jsonPath("$.role") { value("USER") }
            jsonPath("$.provider") { value("BASIC") }
            jsonPath("$.createdAt") { exists() }
            jsonPath("$.updatedAt") { exists() }
        }
    }

    @Test
    fun `GET by id returns 404 for non-existent id`() {
        mockMvc.get("/api/admin/users/00000000-0000-0000-0000-000000000000") {
            with(admin)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("User 00000000-0000-0000-0000-000000000000 not found") }
        }
    }

    @Test
    fun `GET by id without credentials returns 401`() {
        mockMvc.get("/api/admin/users/00000000-0000-0000-0000-000000000000") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `POST creates a user and returns 201`() {
        val subject = uniqueSubject("create")
        mockMvc.post("/api/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateUserRequest(
                    subject = subject,
                    email = "$subject@example.com",
                    displayName = "Created $subject",
                    role = Role.USER,
                    password = "secret",
                ),
            )
            with(admin)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.subject") { value(subject) }
            jsonPath("$.email") { value("$subject@example.com") }
            jsonPath("$.displayName") { value("Created $subject") }
            jsonPath("$.role") { value("USER") }
            jsonPath("$.provider") { value("BASIC") }
        }
    }

    @Test
    fun `POST returns 409 when subject already exists`() {
        val subject = uniqueSubject("dup")
        seedUser(subject)

        mockMvc.post("/api/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateUserRequest(subject = subject))
            with(admin)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("User with id '$subject' already exists") }
        }
    }

    @Test
    fun `POST returns 400 when validation fails`() {
        mockMvc.post("/api/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"subject":"","email":"not-an-email","role":"USER"}"""
            with(admin)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.subject") { exists() }
            jsonPath("$.fieldErrors.email") { exists() }
        }
    }

    @Test
    fun `POST without credentials returns 401`() {
        mockMvc.post("/api/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateUserRequest(subject = uniqueSubject("unauth")))
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `POST as non-admin returns 403`() {
        val subject = uniqueSubject("forbidden-post")
        seedUser(subject, password = "secret")

        mockMvc.post("/api/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateUserRequest(subject = uniqueSubject("nope")))
            with(httpBasic(subject, "secret"))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `PUT updates a user and returns 200`() {
        val subject = uniqueSubject("upd")
        val user = seedUser(subject)

        mockMvc.put("/api/admin/users/${user.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateUserRequest(
                    email = "renamed@example.com",
                    displayName = "Renamed",
                    role = Role.ADMIN,
                ),
            )
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(user.id.toString()) }
            jsonPath("$.email") { value("renamed@example.com") }
            jsonPath("$.displayName") { value("Renamed") }
            jsonPath("$.role") { value("ADMIN") }
        }
    }

    @Test
    fun `PUT returns 404 for non-existent id`() {
        mockMvc.put("/api/admin/users/00000000-0000-0000-0000-000000000000") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateUserRequest(role = Role.USER))
            with(admin)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("User 00000000-0000-0000-0000-000000000000 not found") }
        }
    }

    @Test
    fun `PUT without credentials returns 401`() {
        mockMvc.put("/api/admin/users/00000000-0000-0000-0000-000000000000") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateUserRequest(role = Role.USER))
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `PUT as non-admin returns 403`() {
        val subject = uniqueSubject("forbidden-put")
        val user = seedUser(subject, password = "secret")

        mockMvc.put("/api/admin/users/${user.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateUserRequest(role = Role.ADMIN))
            with(httpBasic(subject, "secret"))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `DELETE removes a user and subsequent GET returns 404`() {
        val subject = uniqueSubject("del")
        val user = seedUser(subject)

        mockMvc.delete("/api/admin/users/${user.id}") {
            with(admin)
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/admin/users/${user.id}") {
            with(admin)
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `DELETE returns 404 for non-existent id`() {
        mockMvc.delete("/api/admin/users/00000000-0000-0000-0000-000000000000") {
            with(admin)
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `DELETE without credentials returns 401`() {
        mockMvc.delete("/api/admin/users/00000000-0000-0000-0000-000000000000") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `DELETE as non-admin returns 403`() {
        val subject = uniqueSubject("forbidden-del")
        val victim = seedUser(uniqueSubject("victim"))
        seedUser(subject, password = "secret")

        mockMvc.delete("/api/admin/users/${victim.id}") {
            with(httpBasic(subject, "secret"))
        }.andExpect { status { isForbidden() } }
    }
}
