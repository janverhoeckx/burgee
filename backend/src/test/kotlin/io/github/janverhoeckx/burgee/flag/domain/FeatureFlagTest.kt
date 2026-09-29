package io.github.janverhoeckx.burgee.flag.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FeatureFlagTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val later = now.plusSeconds(60)
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `create populates fields and stamps both timestamps`() {
        val flag = FeatureFlag.create(
            key = "checkout-v2",
            name = "Checkout v2",
            description = "Switch to new checkout flow",
            enabled = false,
            now = now,
            id = id,
        )

        assertThat(flag.id).isEqualTo(id)
        assertThat(flag.key).isEqualTo("checkout-v2")
        assertThat(flag.name).isEqualTo("Checkout v2")
        assertThat(flag.description).isEqualTo("Switch to new checkout flow")
        assertThat(flag.enabled).isFalse()
        assertThat(flag.createdAt).isEqualTo(now)
        assertThat(flag.updatedAt).isEqualTo(now)
    }

    @Test
    fun `create allows null description`() {
        val flag = FeatureFlag.create("k", "n", description = null, enabled = true, now = now)

        assertThat(flag.description).isNull()
        assertThat(flag.enabled).isTrue()
    }

    @Test
    fun `withDetails updates fields and refreshes updatedAt while preserving createdAt`() {
        val original = FeatureFlag.create("k", "n", "d", false, now, id)

        val updated = original.withDetails(
            name = "Renamed",
            description = "New description",
            enabled = true,
            now = later,
        )

        assertThat(updated.id).isEqualTo(original.id)
        assertThat(updated.key).isEqualTo(original.key)
        assertThat(updated.name).isEqualTo("Renamed")
        assertThat(updated.description).isEqualTo("New description")
        assertThat(updated.enabled).isTrue()
        assertThat(updated.createdAt).isEqualTo(now)
        assertThat(updated.updatedAt).isEqualTo(later)
    }

    @Test
    fun `withDetails leaves the original instance unchanged`() {
        val original = FeatureFlag.create("k", "n", "d", false, now, id)

        original.withDetails("Renamed", null, true, later)

        assertThat(original.name).isEqualTo("n")
        assertThat(original.description).isEqualTo("d")
        assertThat(original.enabled).isFalse()
        assertThat(original.updatedAt).isEqualTo(now)
    }

    @Test
    fun `toggled flips enabled and refreshes updatedAt`() {
        val flag = FeatureFlag.create("k", "n", null, enabled = false, now = now, id = id)

        val toggled = flag.toggled(later)

        assertThat(toggled.enabled).isTrue()
        assertThat(toggled.updatedAt).isEqualTo(later)
        assertThat(toggled.createdAt).isEqualTo(now)
    }

    @Test
    fun `toggled twice returns original enabled state`() {
        val flag = FeatureFlag.create("k", "n", null, enabled = true, now = now, id = id)

        val twice = flag.toggled(later).toggled(later.plusSeconds(1))

        assertThat(twice.enabled).isTrue()
    }

    @Test
    fun `a disabled flag evaluates to false`() {
        val flag = FeatureFlag.create("k", "n", null, enabled = false, now = now, id = id)

        assertThat(flag.evaluate(EvaluationContext.EMPTY)).isFalse()
    }

    @Test
    fun `an enabled flag evaluates to true for an empty context`() {
        val flag = FeatureFlag.create("k", "n", null, enabled = true, now = now, id = id)

        assertThat(flag.evaluate(EvaluationContext.EMPTY)).isTrue()
    }

    @Test
    fun `attributes no flag uses do not change the Evaluation`() {
        val context = EvaluationContext(mapOf("organisationId" to "acme"))
        val on = FeatureFlag.create("k", "n", null, enabled = true, now = now, id = id)
        val off = on.copy(enabled = false)

        assertThat(on.evaluate(context)).isTrue()
        assertThat(off.evaluate(context)).isFalse()
    }

    private fun targeted(vararg conditions: Condition, enabled: Boolean = true) =
        FeatureFlag.create("k", "n", null, enabled = enabled, now = now, id = id, conditions = conditions.toList())

    private fun context(vararg attributes: Pair<String, String>) = EvaluationContext(mapOf(*attributes))

    @Test
    fun `an enabled flag evaluates to true when the Attribute is in the Condition's values`() {
        val flag = targeted(Condition.of("organisationId", ConditionOperator.IN, listOf("acme", "globex")))

        assertThat(flag.evaluate(context("organisationId" to "globex"))).isTrue()
    }

    private val orgIn = Condition.of("organisationId", ConditionOperator.IN, listOf("acme", "globex"))

    @Test
    fun `an enabled flag evaluates to false when the Attribute value is not in the Condition's values`() {
        assertThat(targeted(orgIn).evaluate(context("organisationId" to "initech"))).isFalse()
    }

    @Test
    fun `matching is exact and case-sensitive`() {
        assertThat(targeted(orgIn).evaluate(context("organisationId" to "ACME"))).isFalse()
        assertThat(targeted(orgIn).evaluate(context("organisationId" to "acme "))).isFalse()
    }

    @Test
    fun `a Condition whose Attribute is missing from the context does not match`() {
        assertThat(targeted(orgIn).evaluate(context("country" to "nl"))).isFalse()
        assertThat(targeted(orgIn).evaluate(EvaluationContext.EMPTY)).isFalse()
    }

    @Test
    fun `every Condition must match`() {
        val flag = targeted(orgIn, Condition.of("country", ConditionOperator.IN, listOf("nl")))

        assertThat(flag.evaluate(context("organisationId" to "acme", "country" to "nl"))).isTrue()
        assertThat(flag.evaluate(context("organisationId" to "acme", "country" to "be"))).isFalse()
    }

    @Test
    fun `a disabled flag evaluates to false even when its Conditions match`() {
        assertThat(targeted(orgIn, enabled = false).evaluate(context("organisationId" to "acme"))).isFalse()
    }

    @Test
    fun `a Condition cannot be built with invalid input`() {
        assertThatThrownBy { Condition.of("organisationId", ConditionOperator.IN, emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { Condition.of("1org", ConditionOperator.IN, listOf("acme")) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a flag cannot hold two Conditions on the same Attribute`() {
        assertThatThrownBy { targeted(orgIn, Condition.of("organisationId", ConditionOperator.IN, listOf("x"))) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `withDetails replaces the Conditions and toggled keeps them`() {
        val flag = targeted(orgIn)
        val country = Condition.of("country", ConditionOperator.IN, listOf("nl"))

        assertThat(flag.withDetails("n", null, true, later, conditions = listOf(country)).conditions)
            .containsExactly(country)
        assertThat(flag.toggled(later).conditions).containsExactly(orgIn)
    }
}
