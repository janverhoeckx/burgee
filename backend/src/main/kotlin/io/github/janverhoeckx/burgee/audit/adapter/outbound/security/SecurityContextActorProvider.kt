package io.github.janverhoeckx.burgee.audit.adapter.outbound.security

import io.github.janverhoeckx.burgee.audit.application.port.outbound.ActorProvider
import io.github.janverhoeckx.burgee.security.resolveUsername
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityContextActorProvider : ActorProvider {
    override fun currentActor(): String =
        SecurityContextHolder.getContext().authentication.resolveUsername()
}
