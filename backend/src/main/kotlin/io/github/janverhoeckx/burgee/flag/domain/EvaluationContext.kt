package io.github.janverhoeckx.burgee.flag.domain

/**
 * The Attributes a client submits when asking for a flag's value. May be empty.
 * At most [MAX_ATTRIBUTES] Attributes; names and values follow [AttributeRules].
 */
data class EvaluationContext(val attributes: Map<String, String>) {
    init {
        val violations = violations(attributes)
        require(violations.isEmpty()) { "Invalid evaluation context: $violations" }
    }

    companion object {
        const val MAX_ATTRIBUTES = 50

        /**
         * Validates submitted Attributes. Violations are keyed `attributes` (too many) or
         * `attributes.<name>` (invalid name or value).
         */
        fun parse(attributes: Map<String, String>): Parsed<EvaluationContext> {
            val violations = violations(attributes)
            return if (violations.isEmpty()) Parsed.Valid(EvaluationContext(attributes)) else Parsed.Invalid(violations)
        }

        private fun violations(attributes: Map<String, String>): Map<String, String> {
            if (attributes.size > MAX_ATTRIBUTES) {
                return mapOf("attributes" to "must contain at most $MAX_ATTRIBUTES attributes")
            }
            return buildMap {
                for ((name, value) in attributes) {
                    when {
                        !AttributeRules.isValidName(name) -> put("attributes.$name", "name ${AttributeRules.NAME_RULE}")
                        !AttributeRules.isValidValue(value) -> put("attributes.$name", "value ${AttributeRules.VALUE_RULE}")
                    }
                }
            }
        }
    }
}
