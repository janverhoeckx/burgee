package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.application.port.inbound.EvaluateAllFlagsUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.EvaluateFlagUseCase
import io.github.janverhoeckx.burgee.flag.domain.getOrElse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/flags")
class PublicFlagController(
    private val evaluateFlag: EvaluateFlagUseCase,
    private val evaluateAllFlags: EvaluateAllFlagsUseCase,
) {

    @PostMapping("/evaluate")
    fun evaluateAll(@RequestBody(required = false) request: EvaluateRequest?): ResponseEntity<*> {
        val context = request.toEvaluationContext().getOrElse { return validationFailed(it) }
        return ResponseEntity.ok(evaluateAllFlags.evaluateAll(context).map { it.toResponse() })
    }

    @PostMapping("/{key}/evaluate")
    fun evaluate(@PathVariable key: String, @RequestBody(required = false) request: EvaluateRequest?): ResponseEntity<*> {
        val context = request.toEvaluationContext().getOrElse { return validationFailed(it) }
        return when (val result = evaluateFlag.evaluate(key, context)) {
            is EvaluateFlagUseCase.Result.Evaluated -> ResponseEntity.ok(result.evaluation.toResponse())
            EvaluateFlagUseCase.Result.NotFound -> notFound("Flag '$key' not found")
        }
    }
}
