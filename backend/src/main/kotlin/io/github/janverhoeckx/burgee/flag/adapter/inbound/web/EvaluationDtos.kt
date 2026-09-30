package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.domain.Evaluation
import io.github.janverhoeckx.burgee.flag.domain.EvaluationContext
import io.github.janverhoeckx.burgee.flag.domain.Parsed
import tools.jackson.databind.JsonNode

/**
 * Public evaluate request body. Values are read as raw JSON nodes so that non-string
 * values (numbers, booleans, null, arrays, objects) are rejected instead of coerced.
 */
data class EvaluateRequest(
    val attributes: Map<String, JsonNode?>? = null,
)

/** Public projection of an Evaluation: `enabled` is the Evaluation result, not the master switch. */
data class EvaluationResponse(
    val key: String,
    val enabled: Boolean,
)

fun Evaluation.toResponse() = EvaluationResponse(key = flagKey, enabled = result)

/**
 * Turns the submitted Attributes into an Evaluation Context. A missing body or missing `attributes`
 * is an empty context. Values that are not JSON strings are rejected here; every other rule is the
 * domain's (see [EvaluationContext.parse]).
 */
fun EvaluateRequest?.toEvaluationContext(): Parsed<EvaluationContext> {
    val nodes = this?.attributes.orEmpty()
    val notStrings = nodes.filterValues { it == null || !it.isString }
    if (notStrings.isNotEmpty()) {
        return Parsed.Invalid(notStrings.keys.associate { "attributes.$it" to "must be a string" })
    }
    return EvaluationContext.parse(nodes.mapValues { (_, node) -> node!!.asString() })
}
