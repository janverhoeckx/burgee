package io.github.janverhoeckx.burgee.flag

import io.github.janverhoeckx.burgee.AbstractIT
import org.hamcrest.Matchers.contains
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/** Targeting Rules end to end: admin API round-trips and their effect on the public evaluate API. */
@AutoConfigureMockMvc
class TargetingRuleIT(
    private val mockMvc: MockMvc,
    private val jdbcTemplate: JdbcTemplate,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueKey(suffix: String) = "targeting-${System.nanoTime()}-$suffix"

    private val orgCondition = """{"attribute":"organisationId","operator":"IN","values":["acme","globex"]}"""

    private fun createFlag(key: String, enabled: Boolean = true, conditions: String = "[$orgCondition]"): ResultActionsDsl =
        mockMvc.post("/api/admin/flags") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"key":"$key","name":"Flag $key","enabled":$enabled,"conditions":$conditions}"""
            with(admin)
        }

    private fun createAndExtractId(key: String, enabled: Boolean = true, conditions: String = "[$orgCondition]") =
        createFlag(key, enabled, conditions).andExpect { status { isCreated() } }
            .andReturn().response.contentAsString
            .substringAfter("\"id\":\"").substringBefore("\"")

    @Test
    fun `admin create stores the Conditions and returns them on create and read`() {
        val key = uniqueKey("create")

        val id = createFlag(key).andExpect {
            status { isCreated() }
            jsonPath("$.conditions.length()") { value(1) }
            jsonPath("$.conditions[0].attribute") { value("organisationId") }
            jsonPath("$.conditions[0].operator") { value("IN") }
            jsonPath("$.conditions[0].values") { value(contains("acme", "globex")) }
        }.andReturn().response.contentAsString.substringAfter("\"id\":\"").substringBefore("\"")

        mockMvc.get("/api/admin/flags/$id") { with(admin) }.andExpect {
            status { isOk() }
            jsonPath("$.conditions.length()") { value(1) }
            jsonPath("$.conditions[0].attribute") { value("organisationId") }
            jsonPath("$.conditions[0].operator") { value("IN") }
            jsonPath("$.conditions[0].values") { value(contains("acme", "globex")) }
        }
        mockMvc.get("/api/admin/flags") { with(admin) }.andExpect {
            jsonPath("$[?(@.key == '$key')].conditions[0].attribute") { value("organisationId") }
        }
    }

    private fun updateFlag(id: String, body: String): ResultActionsDsl =
        mockMvc.put("/api/admin/flags/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = body
            with(admin)
        }

    @Test
    fun `admin update replaces the whole list of Conditions`() {
        val id = createAndExtractId(uniqueKey("update"))
        val newConditions = """[
            {"attribute":"country","operator":"IN","values":["nl"]},
            {"attribute":"plan","operator":"IN","values":["pro","enterprise"]}
        ]"""

        updateFlag(id, """{"name":"renamed","enabled":true,"conditions":$newConditions}""").andExpect {
            status { isOk() }
            jsonPath("$.conditions.length()") { value(2) }
            jsonPath("$.conditions[0].attribute") { value("country") }
            jsonPath("$.conditions[1].attribute") { value("plan") }
        }
        mockMvc.get("/api/admin/flags/$id") { with(admin) }.andExpect {
            jsonPath("$.conditions.length()") { value(2) }
            jsonPath("$.conditions[0].attribute") { value("country") }
            jsonPath("$.conditions[0].values") { value(contains("nl")) }
            jsonPath("$.conditions[1].attribute") { value("plan") }
            jsonPath("$.conditions[1].values") { value(contains("pro", "enterprise")) }
        }
    }

    @Test
    fun `admin update without conditions clears the Targeting Rule`() {
        val id = createAndExtractId(uniqueKey("clear"))

        updateFlag(id, """{"name":"renamed","enabled":true}""").andExpect {
            status { isOk() }
            jsonPath("$.conditions.length()") { value(0) }
        }
        mockMvc.get("/api/admin/flags/$id") { with(admin) }.andExpect {
            jsonPath("$.conditions.length()") { value(0) }
        }
    }

    @Test
    fun `toggling a flag keeps its Conditions`() {
        val id = createAndExtractId(uniqueKey("toggle"))

        mockMvc.post("/api/admin/flags/$id/toggle") { with(admin) }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
            jsonPath("$.conditions[0].attribute") { value("organisationId") }
        }
    }

    @Test
    fun `deleting a flag deletes its Conditions with it`() {
        val key = uniqueKey("delete")
        val id = createAndExtractId(key)

        mockMvc.delete("/api/admin/flags/$id") { with(admin) }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/admin/flags/$id") { with(admin) }.andExpect { status { isNotFound() } }
        val remaining = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM flag_condition WHERE flag_id = ?::uuid", Int::class.java, id,
        )
        assertThat(remaining).isZero()
        // The key is free again and the new flag starts without the old Conditions.
        createFlag(key, conditions = "[]").andExpect {
            status { isCreated() }
            jsonPath("$.conditions.length()") { value(0) }
        }
    }

    private fun evaluate(key: String, attributes: String): ResultActionsDsl =
        mockMvc.post("/api/v1/flags/$key/evaluate") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"attributes":$attributes}"""
            with(anonymous())
        }

    private fun evaluateAll(attributes: String): ResultActionsDsl =
        mockMvc.post("/api/v1/flags/evaluate") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"attributes":$attributes}"""
            with(anonymous())
        }

    @Test
    fun `evaluate is true only when the Attribute value is in the Condition's values`() {
        val key = uniqueKey("eval")
        createAndExtractId(key)

        evaluate(key, """{"organisationId":"globex"}""").andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
            jsonPath("$.conditions") { doesNotExist() }
        }
        evaluate(key, """{"organisationId":"initech"}""").andExpect { jsonPath("$.enabled") { value(false) } }
        evaluate(key, """{"organisationId":"ACME"}""").andExpect { jsonPath("$.enabled") { value(false) } }
        evaluate(key, """{"country":"nl"}""").andExpect { jsonPath("$.enabled") { value(false) } }
        evaluate(key, "{}").andExpect { jsonPath("$.enabled") { value(false) } }
    }

    @Test
    fun `bulk evaluate applies each flag's Targeting Rule and never returns Conditions`() {
        val targeted = uniqueKey("bulk-targeted")
        val everyone = uniqueKey("bulk-everyone")
        createAndExtractId(targeted)
        createAndExtractId(everyone, conditions = "[]")

        evaluateAll("""{"organisationId":"acme"}""").andExpect {
            status { isOk() }
            jsonPath("$[?(@.key == '$targeted')].enabled") { value(true) }
            jsonPath("$[?(@.key == '$everyone')].enabled") { value(true) }
            jsonPath("$[*].conditions") { doesNotExist() }
        }
        evaluateAll("""{"organisationId":"initech"}""").andExpect {
            jsonPath("$[?(@.key == '$targeted')].enabled") { value(false) }
            jsonPath("$[?(@.key == '$everyone')].enabled") { value(true) }
        }
        evaluateAll("{}").andExpect {
            jsonPath("$[?(@.key == '$targeted')].enabled") { value(false) }
            jsonPath("$[?(@.key == '$everyone')].enabled") { value(true) }
        }
    }

    @Test
    fun `evaluate is false when one of several Conditions fails`() {
        val key = uniqueKey("multi")
        createAndExtractId(key, conditions = """[$orgCondition,{"attribute":"country","operator":"IN","values":["nl"]}]""")

        evaluate(key, """{"organisationId":"acme","country":"nl"}""").andExpect { jsonPath("$.enabled") { value(true) } }
        evaluate(key, """{"organisationId":"acme","country":"be"}""").andExpect { jsonPath("$.enabled") { value(false) } }
        evaluateAll("""{"organisationId":"acme","country":"be"}""").andExpect {
            jsonPath("$[?(@.key == '$key')].enabled") { value(false) }
        }
    }

    @Test
    fun `a disabled flag evaluates to false even when its Conditions match`() {
        val key = uniqueKey("disabled")
        createAndExtractId(key, enabled = false)

        evaluate(key, """{"organisationId":"acme"}""").andExpect { jsonPath("$.enabled") { value(false) } }
        evaluateAll("""{"organisationId":"acme"}""").andExpect {
            jsonPath("$[?(@.key == '$key')].enabled") { value(false) }
        }
    }

    private fun condition(attribute: String = "organisationId", operator: String = "IN", values: List<String> = listOf("acme")) =
        """{"attribute":"$attribute","operator":"$operator","values":[${values.joinToString(",") { "\"$it\"" }}]}"""

    private fun expectCreateRejected(conditions: String, field: String, message: String) {
        createFlag(uniqueKey("invalid"), conditions = conditions).andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Validation failed") }
            jsonPath("$.fieldErrors['$field']") { value(message) }
        }
    }

    @Test
    fun `admin create rejects an empty value list`() {
        expectCreateRejected("[${condition(values = emptyList())}]", "conditions[0].values", "must contain at least one value")
    }

    @Test
    fun `admin create rejects two Conditions on the same Attribute`() {
        expectCreateRejected(
            "[${condition()},${condition(values = listOf("globex"))}]",
            "conditions[1].attribute",
            "duplicate attribute 'organisationId'",
        )
    }

    @Test
    fun `admin create rejects invalid or too long Attribute names`() {
        listOf("1org", "org id", "org!", "a".repeat(65)).forEach { name ->
            expectCreateRejected(
                "[${condition(attribute = name)}]",
                "conditions[0].attribute",
                "must match ^[A-Za-z][A-Za-z0-9_.-]*$ and be at most 64 characters",
            )
        }
    }

    @Test
    fun `admin create rejects blank or too long values`() {
        listOf("", "   ", "v".repeat(257)).forEach { value ->
            expectCreateRejected(
                "[${condition(values = listOf("acme", value))}]",
                "conditions[0].values[1]",
                "must not be blank and be at most 256 characters",
            )
        }
    }

    @Test
    fun `admin create rejects more than 1000 values and accepts 1000`() {
        val values = (1..1000).map { "org-$it" }

        expectCreateRejected(
            "[${condition(values = values + "org-1001")}]",
            "conditions[0].values",
            "must contain at most 1000 values",
        )
        createFlag(uniqueKey("thousand"), conditions = "[${condition(values = values)}]").andExpect {
            status { isCreated() }
            jsonPath("$.conditions[0].values.length()") { value(1000) }
        }
    }

    @Test
    fun `admin create rejects an operator other than IN`() {
        expectCreateRejected("[${condition(operator = "EQ")}]", "conditions[0].operator", "must be one of: IN")
    }

    @Test
    fun `admin create silently removes duplicate values`() {
        createFlag(uniqueKey("dedupe"), conditions = "[${condition(values = listOf("acme", "globex", "acme"))}]")
            .andExpect {
                status { isCreated() }
                jsonPath("$.conditions[0].values") { value(contains("acme", "globex")) }
            }
    }

    @Test
    fun `admin create rejects a null value inside values with a field error`() {
        expectCreateRejected(
            """[{"attribute":"organisationId","operator":"IN","values":["acme",null]}]""",
            "conditions[0].values[1]",
            "must not be blank and be at most 256 characters",
        )
    }

    @Test
    fun `admin create rejects null Condition fields with field errors`() {
        createFlag(uniqueKey("null-fields"), conditions = """[{"attribute":null,"operator":null,"values":null}]""")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Validation failed") }
                jsonPath("$.fieldErrors['conditions[0].attribute']") {
                    value("must match ^[A-Za-z][A-Za-z0-9_.-]*$ and be at most 64 characters")
                }
                jsonPath("$.fieldErrors['conditions[0].operator']") { value("must be one of: IN") }
                jsonPath("$.fieldErrors['conditions[0].values']") { value("must contain at least one value") }
            }
    }

    @Test
    fun `admin update rejects a null value inside values with a field error`() {
        val id = createAndExtractId(uniqueKey("update-null"))

        updateFlag(id, """{"name":"renamed","enabled":true,"conditions":[{"attribute":"country","operator":"IN","values":[null]}]}""")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.fieldErrors['conditions[0].values[0]']") {
                    value("must not be blank and be at most 256 characters")
                }
            }
    }

    @Test
    fun `admin create counts submitted values, not distinct ones, against the 1000 limit`() {
        val values = (1..1000).map { "org-$it" }

        expectCreateRejected(
            "[${condition(values = values + "org-1")}]",
            "conditions[0].values",
            "must contain at most 1000 values",
        )
    }

    @Test
    fun `admin update rejects an invalid Targeting Rule and keeps the stored one`() {
        val key = uniqueKey("update-invalid")
        val id = createAndExtractId(key)

        updateFlag(id, """{"name":"renamed","enabled":true,"conditions":[${condition(values = emptyList())}]}""")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Validation failed") }
                jsonPath("$.fieldErrors['conditions[0].values']") { value("must contain at least one value") }
            }
        mockMvc.get("/api/admin/flags/$id") { with(admin) }.andExpect {
            jsonPath("$.name") { value("Flag $key") }
            jsonPath("$.conditions[0].values") { value(contains("acme", "globex")) }
        }
    }
}
