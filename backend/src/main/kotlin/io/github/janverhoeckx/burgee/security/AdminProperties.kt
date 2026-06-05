package io.github.janverhoeckx.burgee.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "burgee.admin")
data class AdminProperties(
    /** Built-in admin username for basic auth. */
    val username: String = "admin",
    /** Built-in admin password for basic auth. */
    val password: String = "admin",
    /**
     * Identity-provider subject (the `sub`/uid the IDP issues) that should be granted
     * admin on startup. Lets a first admin be bootstrapped from configuration when using
     * JWT, without manual database changes.
     */
    val subject: String = "",
)
