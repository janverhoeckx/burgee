package io.github.janverhoeckx.burgee.storage

import io.github.janverhoeckx.burgee.BurgeeApplication
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.NestedExceptionUtils

class InMemoryStorageStartupIT {

    private fun start(vararg properties: String) =
        SpringApplicationBuilder(BurgeeApplication::class.java)
            .run("--server.port=0", *properties.map { "--$it" }.toTypedArray())
            .close()

    private fun rootCauseMessage(throwable: Throwable): String? =
        NestedExceptionUtils.getMostSpecificCause(throwable).message

    @Test
    fun `refuses to start in memory mode when DB_URL is set`() {
        assertThatThrownBy { start("burgee.storage=memory", "DB_URL=jdbc:postgresql://db.example.com/burgee") }
            .extracting(::rootCauseMessage)
            .asString()
            .contains("BURGEE_STORAGE=memory", "DB_URL")
    }

    @Test
    fun `refuses to start in memory mode when spring datasource url is set`() {
        assertThatThrownBy {
            start("burgee.storage=memory", "spring.datasource.url=jdbc:postgresql://db.example.com/burgee")
        }
            .extracting(::rootCauseMessage)
            .asString()
            .contains("BURGEE_STORAGE=memory", "spring.datasource.url")
    }
}
