package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.domain.EvaluationContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class EvaluationDtosTest {

    private val mapper = JsonMapper()

    private fun request(vararg attributes: Pair<String, String>) =
        EvaluateRequest(attributes.associate { (name, json) -> name to mapper.readTree(json) })

    private fun fieldErrors(request: EvaluateRequest) =
        catchThrowableOfType(InvalidEvaluationContextException::class.java) { request.toEvaluationContext() }
            .fieldErrors

    @Test
    fun `string values become the Evaluation Context`() {
        assertThat(request("organisationId" to "\"acme\"", "country" to "\"nl\"").toEvaluationContext())
            .isEqualTo(EvaluationContext(mapOf("organisationId" to "acme", "country" to "nl")))
    }

    @Test
    fun `a missing body or missing attributes is an empty context`() {
        assertThat((null as EvaluateRequest?).toEvaluationContext()).isEqualTo(EvaluationContext(emptyMap()))
        assertThat(EvaluateRequest().toEvaluationContext()).isEqualTo(EvaluationContext(emptyMap()))
    }

    @Test
    fun `values that are not JSON strings are rejected`() {
        listOf("42", "true", "null", """["acme"]""", """{"id":"acme"}""").forEach { json ->
            assertThat(fieldErrors(request("organisationId" to json)))
                .containsExactlyEntriesOf(mapOf("attributes.organisationId" to "must be a string"))
        }
        assertThat(fieldErrors(EvaluateRequest(mapOf("organisationId" to null))))
            .containsExactlyEntriesOf(mapOf("attributes.organisationId" to "must be a string"))
    }

    @Test
    fun `domain violations are passed through as field errors`() {
        assertThat(fieldErrors(request("1org" to "\"acme\""))).containsExactlyEntriesOf(
            mapOf("attributes.1org" to "name must match ^[A-Za-z][A-Za-z0-9_.-]*$ and be at most 64 characters"),
        )
    }
}
