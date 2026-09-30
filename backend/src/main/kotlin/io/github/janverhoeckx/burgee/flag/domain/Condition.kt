package io.github.janverhoeckx.burgee.flag.domain

enum class ConditionOperator {
    IN;

    companion object {
        /** The operator with exactly this (case-sensitive) name, or null. */
        fun fromName(name: String?): ConditionOperator? = entries.find { it.name == name }
    }
}

@ConsistentCopyVisibility
data class Condition private constructor(
    val attribute: String,
    val operator: ConditionOperator,
    val values: List<String>,
) {
    fun matches(context: EvaluationContext): Boolean = when (operator) {
        ConditionOperator.IN -> context.attributes[attribute] in values
    }

    /** Renders the Condition, e.g. `organisationId IN (acme, globex)`. */
    fun describe(): String = "$attribute $operator (${values.joinToString()})"

    /** Raw, unvalidated Condition input, e.g. from an admin request. Any field may be missing. */
    data class Input(val attribute: String?, val operator: String?, val values: List<String?>?)

    companion object {
        const val MAX_VALUES = 1000

        /** Builds a Condition, silently removing duplicate values while keeping first-seen order. */
        fun of(attribute: String, operator: ConditionOperator, values: List<String>): Condition {
            val violations = violations(attribute, values)
            require(violations.isEmpty()) { "Invalid condition on '$attribute': $violations" }
            return Condition(attribute, operator, values.distinct())
        }

        /**
         * Validates raw input. Violations are keyed by field (`attribute`, `operator`, `values`, `values[i]`);
         * value indices refer to the values as submitted, before duplicates are removed.
         */
        fun parse(input: Input): Parsed<Condition> {
            val operator = ConditionOperator.fromName(input.operator)
            val violations = buildMap {
                putAll(violations(input.attribute, input.values))
                if (operator == null) put("operator", "must be one of: ${ConditionOperator.entries.joinToString()}")
            }
            if (violations.isNotEmpty()) return Parsed.Invalid(violations)
            // No violations means the attribute, the operator and every value are present.
            return Parsed.Valid(of(input.attribute!!, operator!!, input.values.orEmpty().filterNotNull()))
        }

        private fun violations(attribute: String?, values: List<String?>?): Map<String, String> = buildMap {
            if (!AttributeRules.isValidName(attribute)) put("attribute", AttributeRules.NAME_RULE)
            if (values.isNullOrEmpty()) put("values", "must contain at least one value")
            if (values != null && values.size > MAX_VALUES) put("values", "must contain at most $MAX_VALUES values")
            values?.forEachIndexed { index, value ->
                if (!AttributeRules.isValidValue(value)) put("values[$index]", AttributeRules.VALUE_RULE)
            }
        }
    }
}
