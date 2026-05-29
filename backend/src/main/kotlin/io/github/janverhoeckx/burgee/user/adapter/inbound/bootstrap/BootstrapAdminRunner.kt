package io.github.janverhoeckx.burgee.user.adapter.inbound.bootstrap

import io.github.janverhoeckx.burgee.security.AdminProperties
import io.github.janverhoeckx.burgee.security.AuthProperties
import io.github.janverhoeckx.burgee.user.application.port.inbound.EnsureBootstrapAdminUseCase
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class BootstrapAdminRunner(
    private val authProperties: AuthProperties,
    private val adminProperties: AdminProperties,
    private val ensureBootstrapAdmin: EnsureBootstrapAdminUseCase,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val provider = IdentityProvider.valueOf(authProperties.method.name)
        if (provider != IdentityProvider.BASIC && adminProperties.subject.isNotBlank()) {
            log.info("Bootstrap admin subject configured for {}: {}", provider, adminProperties.subject)
        }
        ensureBootstrapAdmin.ensureBootstrapAdmin(
            EnsureBootstrapAdminUseCase.Command(
                provider = provider,
                basicUsername = adminProperties.username,
                basicPassword = adminProperties.password,
                idpAdminSubject = adminProperties.subject,
            ),
        )
    }
}
