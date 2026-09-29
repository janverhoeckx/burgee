package io.github.janverhoeckx.burgee.flag.domain

/** Builds a flag's Targeting Rule (its Conditions) from raw input. */
object TargetingRule {

    sealed interface Parsed {
        data class Valid(val conditions: List<Condition>) : Parsed
        data class Invalid(val violations: Map<String, String>) : Parsed
    }

    /**
     * Validates raw Condition input. On failure, every violation is reported, keyed by its path
     * on the flag (e.g. `conditions[1].values[0]`).
     */
    fun parse(inputs: List<Condition.Input>): Parsed {
        val violations = linkedMapOf<String, String>()
        val seenAttributes = mutableSetOf<String>()
        inputs.forEachIndexed { index, input ->
            val path = "conditions[$index]"
            Condition.violations(input.attribute, input.values)
                .forEach { (field, message) -> violations["$path.$field"] = message }
            if (!seenAttributes.add(input.attribute)) {
                violations["$path.attribute"] = "duplicate attribute '${input.attribute}'"
            }
            if (operatorOf(input.operator) == null) {
                violations["$path.operator"] = "must be one of: ${ConditionOperator.entries.joinToString()}"
            }
        }
        if (violations.isNotEmpty()) return Parsed.Invalid(violations)
        return Parsed.Valid(inputs.map { Condition.of(it.attribute, operatorOf(it.operator)!!, it.values) })
    }

    private fun operatorOf(name: String): ConditionOperator? = ConditionOperator.entries.find { it.name == name }
}
