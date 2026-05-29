package io.burgee.audit.adapter.outbound.security

import io.burgee.audit.application.port.outbound.ActorProvider
import io.burgee.security.resolveUsername
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityContextActorProvider : ActorProvider {
    override fun currentActor(): String =
        SecurityContextHolder.getContext().authentication.resolveUsername()
}
