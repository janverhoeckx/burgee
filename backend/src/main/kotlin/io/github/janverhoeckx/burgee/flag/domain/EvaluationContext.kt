package io.github.janverhoeckx.burgee.flag.domain

/**
 * The Attributes a client submits when asking for a flag's value. May be empty.
 */
data class EvaluationContext(val attributes: Map<String, String>) {
    companion object {
        val EMPTY = EvaluationContext(emptyMap())
    }
}
