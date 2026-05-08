package io.burgee.flag.adapter.inbound.web

import io.burgee.flag.application.port.inbound.GetFlagByKeyUseCase
import io.burgee.flag.application.port.inbound.ListFlagsUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/flags")
class PublicFlagController(
    private val listFlags: ListFlagsUseCase,
    private val getFlagByKey: GetFlagByKeyUseCase,
) {

    @GetMapping
    fun list(): List<PublicFlagResponse> = listFlags.list().map { it.toPublic() }

    @GetMapping("/{key}")
    fun get(@PathVariable key: String): ResponseEntity<*> =
        when (val result = getFlagByKey.getByKey(key)) {
            is GetFlagByKeyUseCase.Result.Found -> ResponseEntity.ok(result.flag.toPublic())
            GetFlagByKeyUseCase.Result.NotFound -> notFound("Flag '$key' not found")
        }
}
