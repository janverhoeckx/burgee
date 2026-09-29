package io.github.janverhoeckx.burgee.flag.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TargetingRuleTest {

    private fun input(attribute: String = "organisationId", operator: String = "IN", values: List<String> = listOf("acme")) =
        Condition.Input(attribute, operator, values)

    @Test
    fun `parse builds Conditions and silently removes duplicate values, keeping first-seen order`() {
        val parsed = TargetingRule.parse(listOf(input(values = listOf("globex", "acme", "globex", "acme"))))

        assertThat(parsed).isEqualTo(
            TargetingRule.Parsed.Valid(
                listOf(Condition.of("organisationId", ConditionOperator.IN, listOf("globex", "acme"))),
            ),
        )
    }

    private fun violations(vararg inputs: Condition.Input) =
        (TargetingRule.parse(inputs.toList()) as TargetingRule.Parsed.Invalid).violations

    @Test
    fun `a Condition with an empty value list is rejected`() {
        assertThat(violations(input(), input(attribute = "country", values = emptyList())))
            .containsExactlyEntriesOf(mapOf("conditions[1].values" to "must contain at least one value"))
    }

    @Test
    fun `two Conditions on the same Attribute are rejected`() {
        assertThat(violations(input(), input(attribute = "country"), input(values = listOf("globex"))))
            .containsExactlyEntriesOf(mapOf("conditions[2].attribute" to "duplicate attribute 'organisationId'"))
    }

    @Test
    fun `an Attribute name must match the Attribute pattern and be at most 64 characters`() {
        listOf("1org", "_org", "org id", "", "org!", "a".repeat(65)).forEach { name ->
            assertThat(violations(input(attribute = name))).containsExactlyEntriesOf(
                mapOf("conditions[0].attribute" to "must match ^[A-Za-z][A-Za-z0-9_.-]*$ and be at most 64 characters"),
            )
        }
        assertThat(TargetingRule.parse(listOf(input(attribute = "a".repeat(64)), input(attribute = "org.id_x-1"))))
            .isInstanceOf(TargetingRule.Parsed.Valid::class.java)
    }

    @Test
    fun `values must be non-blank and at most 256 characters`() {
        assertThat(violations(input(values = listOf("acme", "", "   ", "v".repeat(257))))).containsExactlyEntriesOf(
            mapOf(
                "conditions[0].values[1]" to "must not be blank and be at most 256 characters",
                "conditions[0].values[2]" to "must not be blank and be at most 256 characters",
                "conditions[0].values[3]" to "must not be blank and be at most 256 characters",
            ),
        )
        assertThat(TargetingRule.parse(listOf(input(values = listOf("v".repeat(256))))))
            .isInstanceOf(TargetingRule.Parsed.Valid::class.java)
    }

    private fun distinctValues(count: Int) = (1..count).map { "v$it" }

    @Test
    fun `a Condition may hold at most 1000 distinct values`() {
        assertThat(violations(input(values = distinctValues(1001))))
            .containsExactlyEntriesOf(mapOf("conditions[0].values" to "must contain at most 1000 values"))
        assertThat(TargetingRule.parse(listOf(input(values = distinctValues(1000) + "v1"))))
            .isInstanceOf(TargetingRule.Parsed.Valid::class.java)
    }

    @Test
    fun `IN is the only operator`() {
        listOf("EQ", "in", "").forEach { operator ->
            assertThat(violations(input(operator = operator)))
                .containsExactlyEntriesOf(mapOf("conditions[0].operator" to "must be one of: IN"))
        }
    }
}
