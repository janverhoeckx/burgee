package io.github.janverhoeckx.burgee.flag.domain

/**
 * The outcome of building a domain value from raw input: either the value, or every violation
 * found, keyed by the path of the offending field (e.g. `conditions[1].values[0]`).
 */
sealed interface Parsed<out T> {
    data class Valid<out T>(val value: T) : Parsed<T>
    data class Invalid(val violations: Map<String, String>) : Parsed<Nothing>
}

/** The parsed value, or the result of [onInvalid] (which may `return` or `throw`) for invalid input. */
inline fun <T> Parsed<T>.getOrElse(onInvalid: (violations: Map<String, String>) -> T): T = when (this) {
    is Parsed.Valid -> value
    is Parsed.Invalid -> onInvalid(violations)
}
