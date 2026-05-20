package io.burgee.flag

import tools.jackson.databind.ObjectMapper
import io.burgee.AbstractIT
import io.burgee.flag.adapter.inbound.web.CreateFeatureFlagRequest
import io.burgee.flag.adapter.inbound.web.UpdateFeatureFlagRequest
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
class AdminFlagControllerIT(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueKey(suffix: String) = "flag-${System.nanoTime()}-$suffix"

    private fun createRequest(key: String, enabled: Boolean = false) = CreateFeatureFlagRequest(
        key = key,
        name = "Flag $key",
        description = "desc for $key",
        enabled = enabled,
    )

    private fun createAndExtractId(request: CreateFeatureFlagRequest): String =
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(admin)
        }.andReturn().response.contentAsString
            .substringAfter("\"id\":\"").substringBefore("\"")

    @Test
    fun `POST creates a flag and returns 201`() {
        val key = uniqueKey("create")
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest(key, enabled = true))
            with(admin)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.key") { value(key) }
            jsonPath("$.name") { value("Flag $key") }
            jsonPath("$.description") { value("desc for $key") }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.createdAt") { exists() }
            jsonPath("$.updatedAt") { exists() }
        }
    }

    @Test
    fun `POST returns 409 when key already exists`() {
        val key = uniqueKey("dup")
        createAndExtractId(createRequest(key))

        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest(key))
            with(admin)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("Flag with key '$key' already exists") }
        }
    }

    @Test
    fun `POST returns 400 when validation fails`() {
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"key":"","name":"","description":null,"enabled":false}"""
            with(admin)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Validation failed") }
            jsonPath("$.fieldErrors.key") { exists() }
            jsonPath("$.fieldErrors.name") { exists() }
        }
    }

    @Test
    fun `POST without credentials returns 401`() {
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest(uniqueKey("unauth")))
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET list returns all flags including newly created one`() {
        val key = uniqueKey("list")
        createAndExtractId(createRequest(key))

        mockMvc.get("/api/admin/flags") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.key == '$key')].name") { value("Flag $key") }
        }
    }

    @Test
    fun `GET list without credentials returns 401`() {
        mockMvc.get("/api/admin/flags") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET by id returns flag after creation`() {
        val key = uniqueKey("byid")
        val id = createAndExtractId(createRequest(key))

        mockMvc.get("/api/admin/flags/$id") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
            jsonPath("$.key") { value(key) }
            jsonPath("$.enabled") { value(false) }
        }
    }

    @Test
    fun `GET by id returns 404 for non-existent id`() {
        mockMvc.get("/api/admin/flags/00000000-0000-0000-0000-000000000000") {
            with(admin)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Flag 00000000-0000-0000-0000-000000000000 not found") }
        }
    }

    @Test
    fun `GET by id without credentials returns 401`() {
        mockMvc.get("/api/admin/flags/00000000-0000-0000-0000-000000000000") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `PUT updates flag and returns 200`() {
        val key = uniqueKey("upd")
        val id = createAndExtractId(createRequest(key))

        mockMvc.put("/api/admin/flags/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateFeatureFlagRequest(name = "renamed", description = "new desc", enabled = true),
            )
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
            jsonPath("$.name") { value("renamed") }
            jsonPath("$.description") { value("new desc") }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `PUT returns 404 for non-existent id`() {
        mockMvc.put("/api/admin/flags/00000000-0000-0000-0000-000000000000") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateFeatureFlagRequest(name = "x", description = null, enabled = false),
            )
            with(admin)
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `PUT without credentials returns 401`() {
        mockMvc.put("/api/admin/flags/00000000-0000-0000-0000-000000000000") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateFeatureFlagRequest(name = "x", description = null, enabled = false),
            )
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `POST toggle flips the enabled value`() {
        val key = uniqueKey("toggle")
        val id = createAndExtractId(createRequest(key, enabled = false))

        mockMvc.post("/api/admin/flags/$id/toggle") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
            jsonPath("$.enabled") { value(true) }
        }

        mockMvc.post("/api/admin/flags/$id/toggle") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
        }
    }

    @Test
    fun `POST toggle returns 404 for non-existent id`() {
        mockMvc.post("/api/admin/flags/00000000-0000-0000-0000-000000000000/toggle") {
            with(admin)
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `POST toggle without credentials returns 401`() {
        mockMvc.post("/api/admin/flags/00000000-0000-0000-0000-000000000000/toggle") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `DELETE removes flag and subsequent GET returns 404`() {
        val key = uniqueKey("del")
        val id = createAndExtractId(createRequest(key))

        mockMvc.delete("/api/admin/flags/$id") {
            with(admin)
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/admin/flags/$id") {
            with(admin)
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `DELETE returns 404 for non-existent id`() {
        mockMvc.delete("/api/admin/flags/00000000-0000-0000-0000-000000000000") {
            with(admin)
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `DELETE without credentials returns 401`() {
        mockMvc.delete("/api/admin/flags/00000000-0000-0000-0000-000000000000") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `POST with wrong credentials returns 401`() {
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest(uniqueKey("wrong")))
            with(httpBasic("admin", "wrong-password"))
        }.andExpect { status { isUnauthorized() } }
    }
}
