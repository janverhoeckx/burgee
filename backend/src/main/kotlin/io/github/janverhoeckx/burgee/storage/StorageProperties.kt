package io.github.janverhoeckx.burgee.storage

import org.springframework.boot.context.properties.ConfigurationProperties

/** Binds `burgee.storage` (env `BURGEE_STORAGE`): where Burgee keeps flags, users and audit entries. */
@ConfigurationProperties(prefix = "burgee")
data class StorageProperties(
    val storage: Mode = Mode.POSTGRES,
) {
    enum class Mode { POSTGRES, MEMORY }
}
