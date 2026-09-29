package io.github.janverhoeckx.burgee.storage

import io.github.janverhoeckx.burgee.BurgeeApplication
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.NestedExceptionUtils

class InMemoryStorageStartupIT {

    @ParameterizedTest
    @ValueSource(strings = ["DB_URL", "spring.datasource.url"])
    fun `refuses to start in memory mode when a database URL is set`(setting: String) {
        assertThatThrownBy {
            SpringApplicationBuilder(BurgeeApplication::class.java)
                .run("--server.port=0", "--burgee.storage=memory", "--$setting=jdbc:postgresql://db.example.com/burgee")
                .close()
        }
            .extracting { NestedExceptionUtils.getMostSpecificCause(it).message }
            .asString()
            .contains("BURGEE_STORAGE=memory", setting)
    }
}
