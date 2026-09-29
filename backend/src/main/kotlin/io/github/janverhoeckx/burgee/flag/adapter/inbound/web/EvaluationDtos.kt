package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.domain.Attribute
import io.github.janverhoeckx.burgee.flag.domain.Evaluation
import io.github.janverhoeckx.burgee.flag.domain.EvaluationContext
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

private const val MAX_ATTRIBUTES = 50

class InvalidEvaluationContextException(val fieldErrors: Map<String, String>) :
    RuntimeException("Invalid evaluation context")

/**
 * Validates the submitted Attributes and turns them into an Evaluation Context.
 * A missing body or missing `attributes` is an empty context.
 */
fun EvaluateRequest?.toEvaluationContext(): EvaluationContext {
    val attributes = this?.attributes.orEmpty()
    if (attributes.size > MAX_ATTRIBUTES) {
        throw InvalidEvaluationContextException(
            mapOf("attributes" to "must contain at most $MAX_ATTRIBUTES attributes"),
        )
    }
    val errors = linkedMapOf<String, String>()
    val values = linkedMapOf<String, String>()
    for ((name, node) in attributes) {
        val field = "attributes.$name"
        if (!Attribute.isValidName(name)) {
            errors[field] = "name ${Attribute.NAME_RULE}"
            continue
        }
        if (node == null || !node.isString) {
            errors[field] = "must be a string"
            continue
        }
        val value = node.asString()
        if (!Attribute.isValidValue(value)) {
            errors[field] = "value ${Attribute.VALUE_RULE}"
            continue
        }
        values[name] = value
    }
    if (errors.isNotEmpty()) throw InvalidEvaluationContextException(errors)
    return EvaluationContext(values)
}
