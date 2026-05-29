package io.github.janverhoeckx.burgee.audit.application.port.outbound

fun interface ActorProvider {
    fun currentActor(): String
}
