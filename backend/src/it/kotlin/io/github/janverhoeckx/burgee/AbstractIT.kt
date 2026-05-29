package io.github.janverhoeckx.burgee

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("integration-test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractIT {
    companion object {
        @JvmStatic
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .also { it.start() }
    }
}
