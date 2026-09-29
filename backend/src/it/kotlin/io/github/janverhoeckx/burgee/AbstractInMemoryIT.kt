package io.github.janverhoeckx.burgee

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor

/** Base for integration tests that run Burgee in the In-memory storage mode, without a Postgres container. */
@SpringBootTest(properties = ["burgee.storage=memory"])
@ActiveProfiles("integration-test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class AbstractInMemoryIT
