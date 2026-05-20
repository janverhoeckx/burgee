package io.burgee.flag

import tools.jackson.databind.ObjectMapper
import io.burgee.AbstractIT
import io.burgee.flag.adapter.inbound.web.CreateFeatureFlagRequest
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class PublicFlagControllerIT(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueKey(suffix: String) = "public-${System.nanoTime()}-$suffix"

    private fun seedFlag(key: String, enabled: Boolean): String {
        val response = mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateFeatureFlagRequest(
                    key = key,
                    name = "Flag $key",
                    description = "desc",
                    enabled = enabled,
                ),
            )
            with(admin)
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return response.substringAfter("\"id\":\"").substringBefore("\"")
    }

    @Test
    fun `GET list returns only key and enabled fields and is publicly accessible`() {
        val enabledKey = uniqueKey("on")
        val disabledKey = uniqueKey("off")
        seedFlag(enabledKey, enabled = true)
        seedFlag(disabledKey, enabled = false)

        mockMvc.get("/api/v1/flags") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.key == '$enabledKey')].enabled") { value(true) }
            jsonPath("$[?(@.key == '$disabledKey')].enabled") { value(false) }
            jsonPath("$[?(@.key == '$enabledKey')].id") { doesNotExist() }
            jsonPath("$[?(@.key == '$enabledKey')].description") { doesNotExist() }
            jsonPath("$[?(@.key == '$enabledKey')].name") { doesNotExist() }
        }
    }

    @Test
    fun `GET by key returns public projection when flag exists`() {
        val key = uniqueKey("by-key")
        seedFlag(key, enabled = true)

        mockMvc.get("/api/v1/flags/$key") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$.key") { value(key) }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.id") { doesNotExist() }
            jsonPath("$.name") { doesNotExist() }
            jsonPath("$.description") { doesNotExist() }
        }
    }

    @Test
    fun `GET by key returns 404 when flag does not exist`() {
        val missingKey = uniqueKey("missing")

        mockMvc.get("/api/v1/flags/$missingKey") {
            with(anonymous())
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Flag '$missingKey' not found") }
        }
    }

    @Test
    fun `GET by key reflects the latest enabled state after toggle`() {
        val key = uniqueKey("toggle")
        val id = seedFlag(key, enabled = false)

        mockMvc.get("/api/v1/flags/$key") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
        }

        mockMvc.post("/api/admin/flags/$id/toggle") {
            with(admin)
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/flags/$key") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
    }
}
