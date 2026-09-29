package io.github.janverhoeckx.burgee.flag.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EvaluationContextTest {

    private fun violations(attributes: Map<String, String>) =
        (EvaluationContext.parse(attributes) as Parsed.Invalid).violations

    private fun attributes(count: Int) = (1..count).associate { "a$it" to "v" }

    @Test
    fun `parse accepts valid Attributes and an empty context`() {
        assertThat(EvaluationContext.parse(mapOf("organisationId" to "acme")))
            .isEqualTo(Parsed.Valid(EvaluationContext(mapOf("organisationId" to "acme"))))
        assertThat(EvaluationContext.parse(emptyMap()))
            .isEqualTo(Parsed.Valid(EvaluationContext(emptyMap())))
    }

    @Test
    fun `an Evaluation Context holds at most 50 Attributes`() {
        assertThat(EvaluationContext.parse(attributes(50))).isInstanceOf(Parsed.Valid::class.java)
        assertThat(violations(attributes(51)))
            .containsExactlyEntriesOf(mapOf("attributes" to "must contain at most 50 attributes"))
    }

    @Test
    fun `an Attribute name must match the Attribute pattern and be at most 64 characters`() {
        listOf("1org", "_org", "org id", "", "org!", "a".repeat(65)).forEach { name ->
            assertThat(violations(mapOf(name to "acme"))).containsExactlyEntriesOf(
                mapOf("attributes.$name" to "name must match ^[A-Za-z][A-Za-z0-9_.-]*$ and be at most 64 characters"),
            )
        }
        assertThat(EvaluationContext.parse(mapOf("a".repeat(64) to "acme", "org.id_x-1" to "acme")))
            .isInstanceOf(Parsed.Valid::class.java)
    }

    @Test
    fun `an Attribute value must be non-blank and at most 256 characters`() {
        listOf("", "   ", "v".repeat(257)).forEach { value ->
            assertThat(violations(mapOf("organisationId" to value))).containsExactlyEntriesOf(
                mapOf("attributes.organisationId" to "value must not be blank and be at most 256 characters"),
            )
        }
        assertThat(EvaluationContext.parse(mapOf("organisationId" to "v".repeat(256))))
            .isInstanceOf(Parsed.Valid::class.java)
    }

    @Test
    fun `every invalid Attribute is reported`() {
        assertThat(violations(mapOf("1org" to "acme", "country" to " ", "plan" to "pro"))).containsOnlyKeys(
            "attributes.1org",
            "attributes.country",
        )
    }

    @Test
    fun `an invalid Evaluation Context cannot be constructed`() {
        assertThatThrownBy { EvaluationContext(mapOf("1org" to "acme")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { EvaluationContext(attributes(51)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
