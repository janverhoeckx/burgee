package io.github.janverhoeckx.burgee.flag.domain

/**
 * A flag's Targeting Rule: zero or more Conditions that must all match. No Conditions means it
 * matches every Evaluation Context. Two Conditions can never share an Attribute.
 */
data class TargetingRule(val conditions: List<Condition> = emptyList()) {
    init {
        val duplicates = duplicateAttributes(conditions.map { it.attribute })
        require(duplicates.isEmpty()) {
            "A Targeting Rule cannot hold two Conditions on the same Attribute: ${duplicates.values}"
        }
    }

    fun matches(context: EvaluationContext): Boolean = conditions.all { it.matches(context) }

    /** Renders the rule, e.g. `[organisationId IN (acme, globex) AND country IN (nl)]`. */
    fun describe(): String = conditions.joinToString(separator = " AND ", prefix = "[", postfix = "]") { it.describe() }

    companion object {
        /**
         * Validates raw Condition input. On failure, every violation is reported, keyed by its path
         * on the flag (e.g. `conditions[1].values[0]`).
         */
        fun parse(inputs: List<Condition.Input>): Parsed<TargetingRule> {
            val violations = linkedMapOf<String, String>()
            val conditions = mutableListOf<Condition>()
            val duplicates = duplicateAttributes(inputs.map { it.attribute })
            inputs.forEachIndexed { index, input ->
                val path = "conditions[$index]"
                when (val parsed = Condition.parse(input)) {
                    is Parsed.Valid -> conditions += parsed.value
                    is Parsed.Invalid -> parsed.violations.forEach { (field, message) -> violations["$path.$field"] = message }
                }
                duplicates[index]?.let { violations["$path.attribute"] = "duplicate attribute '$it'" }
            }
            return if (violations.isEmpty()) Parsed.Valid(TargetingRule(conditions)) else Parsed.Invalid(violations)
        }

        /** Index → Attribute of every Condition whose Attribute already appeared earlier in the list. */
        private fun duplicateAttributes(attributes: List<String?>): Map<Int, String> {
            val seen = mutableSetOf<String>()
            return buildMap {
                attributes.forEachIndexed { index, attribute ->
                    if (attribute != null && !seen.add(attribute)) put(index, attribute)
                }
            }
        }
    }
}
