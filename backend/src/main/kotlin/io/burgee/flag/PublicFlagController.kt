package io.burgee.flag

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/flags")
class PublicFlagController(private val service: FeatureFlagService) {

    @GetMapping
    fun list(): List<PublicFlagResponse> = service.listAll().map { it.toPublic() }

    @GetMapping("/{key}")
    fun get(@PathVariable key: String): PublicFlagResponse = service.getByKey(key).toPublic()
}
