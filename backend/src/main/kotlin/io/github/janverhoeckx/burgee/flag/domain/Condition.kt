package io.github.janverhoeckx.burgee.flag.domain

enum class ConditionOperator { IN }

/**
 * One requirement in a Targeting Rule: an Attribute name, an operator and a list of values.
 * A Condition whose Attribute is missing from the Evaluation Context does not match.
 */
@ConsistentCopyVisibility
data class Condition private constructor(
    val attribute: String,
    val operator: ConditionOperator,
    val values: List<String>,
) {
    fun matches(context: EvaluationContext): Boolean = when (operator) {
        ConditionOperator.IN -> context.attributes[attribute] in values
    }

    /** Raw, unvalidated Condition input, e.g. from an admin request. */
    data class Input(val attribute: String, val operator: String, val values: List<String>)

    companion object {
        const val MAX_VALUES = 1000

        /** Builds a Condition, silently removing duplicate values while keeping first-seen order. */
        fun of(attribute: String, operator: ConditionOperator, values: List<String>): Condition {
            val violations = violations(attribute, values)
            require(violations.isEmpty()) { "Invalid condition on '$attribute': $violations" }
            return Condition(attribute, operator, values.distinct())
        }

        /**
         * Violations of a Condition's own invariants, keyed by field (`attribute`, `values`, `values[i]`).
         * Value indices refer to [values] as given, before duplicates are removed.
         */
        fun violations(attribute: String, values: List<String>): Map<String, String> = buildMap {
            if (!Attribute.isValidName(attribute)) put("attribute", Attribute.NAME_RULE)
            if (values.isEmpty()) put("values", "must contain at least one value")
            if (values.distinct().size > MAX_VALUES) put("values", "must contain at most $MAX_VALUES values")
            values.forEachIndexed { index, value ->
                if (!Attribute.isValidValue(value)) put("values[$index]", Attribute.VALUE_RULE)
            }
        }
    }
}
