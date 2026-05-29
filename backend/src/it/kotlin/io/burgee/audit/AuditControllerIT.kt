package io.burgee.audit

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
class AuditControllerIT(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueKey(suffix: String) = "audit-${System.nanoTime()}-$suffix"

    private fun createAndExtractId(key: String): String =
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateFeatureFlagRequest(key = key, name = "Flag $key", description = "d", enabled = false),
            )
            with(admin)
        }.andReturn().response.contentAsString
            .substringAfter("\"id\":\"").substringBefore("\"")

    @Test
    fun `audit trail captures the full lifecycle of a flag`() {
        val key = uniqueKey("life")
        val id = createAndExtractId(key)

        mockMvc.put("/api/admin/flags/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateFeatureFlagRequest(name = "renamed", description = "d", enabled = false),
            )
            with(admin)
        }
        mockMvc.post("/api/admin/flags/$id/toggle") { with(admin) }
        mockMvc.delete("/api/admin/flags/$id") { with(admin) }

        mockMvc.get("/api/admin/audit?flagId=$id") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(4) }
            jsonPath("$[0].action") { value("DELETE") }
            jsonPath("$[1].action") { value("TOGGLE") }
            jsonPath("$[2].action") { value("UPDATE") }
            jsonPath("$[3].action") { value("CREATE") }
            jsonPath("$[0].actor") { value("admin") }
            jsonPath("$[0].flagKey") { value(key) }
        }
    }

    @Test
    fun `audit entry records who performed the action and a detail`() {
        val key = uniqueKey("who")
        val id = createAndExtractId(key)

        mockMvc.get("/api/admin/audit?flagId=$id") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].action") { value("CREATE") }
            jsonPath("$[0].actor") { value("admin") }
            jsonPath("$[0].flagKey") { value(key) }
            jsonPath("$[0].detail") { exists() }
            jsonPath("$[0].occurredAt") { exists() }
        }
    }

    @Test
    fun `audit list is included in the global feed`() {
        val key = uniqueKey("feed")
        createAndExtractId(key)

        mockMvc.get("/api/admin/audit") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.flagKey == '$key')].action") { value("CREATE") }
        }
    }

    @Test
    fun `audit endpoint without credentials returns 401`() {
        mockMvc.get("/api/admin/audit") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }
}
