package io.burgee.audit.adapter.inbound.web

import io.burgee.audit.application.port.inbound.ListAuditTrailUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/audit")
class AuditController(
    private val listAuditTrail: ListAuditTrailUseCase,
) {

    @GetMapping
    fun list(@RequestParam(required = false) flagId: UUID?): List<AuditEntryResponse> =
        when (flagId) {
            null -> listAuditTrail.list()
            else -> listAuditTrail.listForFlag(flagId)
        }.map { it.toResponse() }
}
