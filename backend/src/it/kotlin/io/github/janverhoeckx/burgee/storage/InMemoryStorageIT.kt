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

        mockMvc.post("/api/v1/flags/$key/evaluate") {
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
    fun `a flag's Targeting Rule is stored and applied on evaluation`() {
        val key = uniqueKey("targeted")
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"key":"$key","name":"Flag $key","enabled":true,
                "conditions":[{"attribute":"organisationId","operator":"IN","values":["acme","globex"]}]}"""
            with(admin)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.conditions[0].values[1]") { value("globex") }
        }

        mockMvc.post("/api/v1/flags/$key/evaluate") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"attributes":{"organisationId":"globex"}}"""
            with(anonymous())
        }.andExpect { jsonPath("$.enabled") { value(true) } }
        mockMvc.post("/api/v1/flags/$key/evaluate") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"attributes":{"organisationId":"initech"}}"""
            with(anonymous())
        }.andExpect { jsonPath("$.enabled") { value(false) } }
    }

    @Test
    fun `a created user is stored and can sign in`() {
        val subject = "mem-user-${System.nanoTime()}"
        mockMvc.post("/api/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"subject":"$subject","role":"USER","password":"secret"}"""
            with(admin)
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/auth/user") {
            with(httpBasic(subject, "secret"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value(subject) }
            jsonPath("$.role") { value("USER") }
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
