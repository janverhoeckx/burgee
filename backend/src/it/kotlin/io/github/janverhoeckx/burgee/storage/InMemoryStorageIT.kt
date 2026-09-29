package io.github.janverhoeckx.burgee.storage

import io.github.janverhoeckx.burgee.AbstractInMemoryIT
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class InMemoryStorageIT(
    private val mockMvc: MockMvc,
) : AbstractInMemoryIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueKey(suffix: String) = "mem-${System.nanoTime()}-$suffix"

    private fun createFlag(key: String) =
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"key":"$key","name":"Flag $key","description":null,"enabled":false}"""
            with(admin)
        }

    @Test
    fun `a created and toggled flag is stored with its audit trail`() {
        val key = uniqueKey("lifecycle")
        val id = createFlag(key).andExpect { status { isCreated() } }
            .andReturn().response.contentAsString
            .substringAfter("\"id\":\"").substringBefore("\"")

        mockMvc.post("/api/admin/flags/$id/toggle") { with(admin) }
            .andExpect { status { isOk() } }

        mockMvc.get("/api/v1/flags/$key") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
        mockMvc.get("/api/admin/audit?flagId=$id") {
            with(admin)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].action") { value("TOGGLE") }
            jsonPath("$[1].action") { value("CREATE") }
            jsonPath("$[0].actor") { value("admin") }
        }
    }

    @Test
    fun `creating a flag with an existing key returns 409`() {
        val key = uniqueKey("dup")
        createFlag(key).andExpect { status { isCreated() } }

        createFlag(key).andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("Flag with key '$key' already exists") }
        }
    }

    @Test
    fun `GET auth info reports the in-memory storage mode`() {
        mockMvc.get("/api/auth/info") {
            with(anonymous())
        }.andExpect {
            status { isOk() }
            jsonPath("$.storage") { value("memory") }
        }
    }
}
