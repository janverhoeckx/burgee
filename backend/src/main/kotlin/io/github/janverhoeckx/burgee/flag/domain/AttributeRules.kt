package io.github.janverhoeckx.burgee.flag.domain

/**
 * Validation rules for Attribute names and values, shared by the Evaluation Context a client submits
 * and the Conditions of a Targeting Rule.
 */
object AttributeRules {
    const val NAME_PATTERN = "^[A-Za-z][A-Za-z0-9_.-]*$"
    const val MAX_NAME_LENGTH = 64
    const val MAX_VALUE_LENGTH = 256

    const val NAME_RULE = "must match $NAME_PATTERN and be at most $MAX_NAME_LENGTH characters"
    const val VALUE_RULE = "must not be blank and be at most $MAX_VALUE_LENGTH characters"

    private val NAME_REGEX = Regex(NAME_PATTERN)

    fun isValidName(name: String?): Boolean =
        name != null && name.length <= MAX_NAME_LENGTH && NAME_REGEX.matches(name)

    fun isValidValue(value: String?): Boolean =
        value != null && value.isNotBlank() && value.length <= MAX_VALUE_LENGTH
}
