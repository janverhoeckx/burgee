package io.burgee.flag

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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/flags")
class AdminFlagController(private val service: FeatureFlagService) {

    @GetMapping
    fun list(): List<FeatureFlagResponse> = service.listAll().map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): FeatureFlagResponse = service.get(id).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid request: CreateFeatureFlagRequest): FeatureFlagResponse =
        service.create(request).toResponse()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateFeatureFlagRequest,
    ): FeatureFlagResponse = service.update(id, request).toResponse()

    @PostMapping("/{id}/toggle")
    fun toggle(@PathVariable id: UUID): FeatureFlagResponse = service.toggle(id).toResponse()

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
