package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.ListFlagsUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.UpdateFlagUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/flags")
class AdminFlagController(
    private val listFlags: ListFlagsUseCase,
    private val getFlagById: GetFlagByIdUseCase,
    private val createFlag: CreateFlagUseCase,
    private val updateFlag: UpdateFlagUseCase,
    private val toggleFlag: ToggleFlagUseCase,
    private val deleteFlag: DeleteFlagUseCase,
) {

    @GetMapping
    fun list(): List<FeatureFlagResponse> = listFlags.list().map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<*> =
        when (val result = getFlagById.getById(id)) {
            is GetFlagByIdUseCase.Result.Found -> ResponseEntity.ok(result.flag.toResponse())
            GetFlagByIdUseCase.Result.NotFound -> notFound("Flag $id not found")
        }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateFeatureFlagRequest): ResponseEntity<*> =
        when (val result = createFlag.create(request.toCommand())) {
            is CreateFlagUseCase.Result.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(result.flag.toResponse())
            is CreateFlagUseCase.Result.DuplicateKey ->
                apiError(HttpStatus.CONFLICT, "Flag with key '${result.key}' already exists")
        }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateFeatureFlagRequest,
    ): ResponseEntity<*> =
        when (val result = updateFlag.update(request.toCommand(id))) {
            is UpdateFlagUseCase.Result.Updated -> ResponseEntity.ok(result.flag.toResponse())
            UpdateFlagUseCase.Result.NotFound -> notFound("Flag $id not found")
        }

    @PostMapping("/{id}/toggle")
    fun toggle(@PathVariable id: UUID): ResponseEntity<*> =
        when (val result = toggleFlag.toggle(id)) {
            is ToggleFlagUseCase.Result.Toggled -> ResponseEntity.ok(result.flag.toResponse())
            ToggleFlagUseCase.Result.NotFound -> notFound("Flag $id not found")
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<*> =
        when (deleteFlag.delete(id)) {
            DeleteFlagUseCase.Result.Deleted -> ResponseEntity.noContent().build<Void>()
            DeleteFlagUseCase.Result.NotFound -> notFound("Flag $id not found")
        }
}
