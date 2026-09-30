package io.github.janverhoeckx.burgee.flag

import tools.jackson.databind.ObjectMapper
import io.github.janverhoeckx.burgee.AbstractIT
import io.github.janverhoeckx.burgee.flag.adapter.inbound.web.CreateFeatureFlagRequest
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
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
                CreateFeatureFlagRequest(key = key, name = "Flag $key", description = "desc", enabled = enabled),
            )
            with(admin)
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return response.substringAfter("\"id\":\"").substringBefore("\"")
    }

    private fun evaluate(key: String, body: String? = """{"attributes":{}}"""): ResultActionsDsl =
        mockMvc.post("/api/v1/flags/$key/evaluate") {
            if (body != null) {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }
            with(anonymous())
        }

    private fun evaluateAll(body: String? = """{"attributes":{}}"""): ResultActionsDsl =
        mockMvc.post("/api/v1/flags/evaluate") {
            if (body != null) {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }
            with(anonymous())
        }

    @Test
    fun `evaluate returns only key and the Evaluation result and is publicly accessible`() {
        val key = uniqueKey("single")
        seedFlag(key, enabled = true)

        evaluate(key, """{"attributes":{"organisationId":"acme"}}""").andExpect {
            status { isOk() }
            jsonPath("$.key") { value(key) }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.id") { doesNotExist() }
            jsonPath("$.name") { doesNotExist() }
            jsonPath("$.description") { doesNotExist() }
        }
    }

    @Test
    fun `evaluate returns 404 for an unknown key`() {
        val missingKey = uniqueKey("missing")

        evaluate(missingKey).andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Flag '$missingKey' not found") }
        }
    }

    @Test
    fun `evaluate treats a missing body as an empty context`() {
        val key = uniqueKey("no-body")
        seedFlag(key, enabled = true)

        evaluate(key, body = null).andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `evaluate treats a body without attributes as an empty context`() {
        val key = uniqueKey("no-attrs")
        seedFlag(key, enabled = true)

        evaluate(key, body = "{}").andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `bulk evaluate returns every flag including disabled ones and is publicly accessible`() {
        val enabledKey = uniqueKey("on")
        val disabledKey = uniqueKey("off")
        seedFlag(enabledKey, enabled = true)
        seedFlag(disabledKey, enabled = false)

        evaluateAll("""{"attributes":{"organisationId":"acme"}}""").andExpect {
            status { isOk() }
            jsonPath("$[?(@.key == '$enabledKey')].enabled") { value(true) }
            jsonPath("$[?(@.key == '$disabledKey')].enabled") { value(false) }
            jsonPath("$[?(@.key == '$enabledKey')].id") { doesNotExist() }
            jsonPath("$[?(@.key == '$enabledKey')].name") { doesNotExist() }
            jsonPath("$[?(@.key == '$enabledKey')].description") { doesNotExist() }
        }
    }

    @Test
    fun `evaluate rejects a number attribute value with 400`() {
        val key = uniqueKey("number")
        seedFlag(key, enabled = true)

        evaluate(key, """{"attributes":{"organisationId":42}}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Validation failed") }
            jsonPath("$.fieldErrors['attributes.organisationId']") { value("must be a string") }
        }
    }

    @Test
    fun `evaluate and bulk evaluate reject null, array, object and boolean attribute values with 400`() {
        val key = uniqueKey("non-string")
        seedFlag(key, enabled = true)

        listOf("null", """["acme"]""", """{"id":"acme"}""", "true").forEach { value ->
            val body = """{"attributes":{"organisationId":$value}}"""
            evaluate(key, body).andExpect {
                status { isBadRequest() }
                jsonPath("$.fieldErrors['attributes.organisationId']") { value("must be a string") }
            }
            evaluateAll(body).andExpect {
                status { isBadRequest() }
                jsonPath("$.fieldErrors['attributes.organisationId']") { value("must be a string") }
            }
        }
    }

    private fun attributesBody(count: Int) =
        (1..count).joinToString(",", prefix = """{"attributes":{""", postfix = "}}") { """"a$it":"v"""" }

    @Test
    fun `evaluate accepts 50 attributes and rejects 51 with 400`() {
        val key = uniqueKey("many")
        seedFlag(key, enabled = true)

        evaluate(key, attributesBody(50)).andExpect { status { isOk() } }
        evaluate(key, attributesBody(51)).andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.attributes") { value("must contain at most 50 attributes") }
        }
    }

    @Test
    fun `evaluate rejects invalid attribute names with 400`() {
        val key = uniqueKey("names")
        seedFlag(key, enabled = true)
        val message = "name must match ^[A-Za-z][A-Za-z0-9_.-]*$ and be at most 64 characters"

        listOf("1org", "_org", "org id", "", "org!", "a".repeat(65)).forEach { name ->
            evaluate(key, """{"attributes":{"$name":"acme"}}""").andExpect {
                status { isBadRequest() }
                jsonPath("$.fieldErrors['attributes.$name']") { value(message) }
            }
        }
        evaluate(key, """{"attributes":{"${"a".repeat(64)}":"acme","org.id_x-1":"acme"}}""")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `evaluate rejects blank or too long attribute values with 400`() {
        val key = uniqueKey("values")
        seedFlag(key, enabled = true)

        listOf("", "   ", "v".repeat(257)).forEach { value ->
            evaluate(key, """{"attributes":{"organisationId":"$value"}}""").andExpect {
                status { isBadRequest() }
                jsonPath("$.fieldErrors['attributes.organisationId']") {
                    value("value must not be blank and be at most 256 characters")
                }
            }
        }
        evaluate(key, """{"attributes":{"organisationId":"${"v".repeat(256)}"}}""")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `bulk evaluate treats a missing body as an empty context`() {
        val key = uniqueKey("bulk-no-body")
        seedFlag(key, enabled = true)

        evaluateAll(body = null).andExpect {
            status { isOk() }
            jsonPath("$[?(@.key == '$key')].enabled") { value(true) }
        }
    }
}
