package io.github.janverhoeckx.burgee.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "burgee.firebase")
data class FirebaseProperties(
    val projectId: String = "",
    val apiKey: String = "",
    val authDomain: String = "",
)
