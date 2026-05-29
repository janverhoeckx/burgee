# REST Endpoint Integration Test Template

## Output path

`src/it/kotlin/{BASE_PATH}/{context}/{Model}IT.kt`

## Template

```kotlin
package {BASE}.{context}

import {BASE}.AbstractIT
import {BASE}.{context}.domain.models.{Model}
import {BASE}.{context}.domain.ports.{Model}Repository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete  // delete only
import org.springframework.test.web.servlet.get     // read only
import org.springframework.test.web.servlet.post    // create only
import org.springframework.test.web.servlet.put     // update only
import java.util.UUID

@AutoConfigureMockMvc
class {Model}IT(
    @param:Autowired private val mockMvc: MockMvc,
    @param:Autowired private val repository: {Model}Repository,
) : AbstractIT()

    private val spreekuurIdpId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val otherUserIdpId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private fun createTestModel(
        id: UUID = UUID.randomUUID(),
        userIdpId: UUID = spreekuurIdpId,
        /* other fields with defaults */
    ): {Model} {
        val model = {Model}(id, userIdpId, /* fields */)
        repository.create(model)
        return model
    }

    // POST — create:
    @Test
    fun `POST creates a {Model} and returns 201`() {
        mockMvc.post("/{context}/{model-kebab}") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ /* all fields */ }"""
            with(jwt().jwt { it.subject(spreekuurIdpId.toString()) })
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            // jsonPath("$.field") { value("...") }
        }
    }

    // GET — read:
    @Test
    fun `GET returns {Model} by id`() {
        val model = createTestModel()
        mockMvc.get("/{context}/{model-kebab}/${model.id}") {
            with(jwt().jwt { it.subject(spreekuurIdpId.toString()) })
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(model.id.toString()) }
        }
    }

    // PUT — update:
    @Test
    fun `PUT updates {Model} and returns 200`() {
        val model = createTestModel()
        mockMvc.put("/{context}/{model-kebab}/${model.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ /* updated fields */ }"""
            with(jwt().jwt { it.subject(spreekuurIdpId.toString()) })
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(model.id.toString()) }
        }
    }

    // DELETE — and verify subsequent GET is 404:
    @Test
    fun `DELETE removes {Model} and subsequent GET returns 404`() {
        val model = createTestModel()
        mockMvc.delete("/{context}/{model-kebab}/${model.id}") {
            with(jwt().jwt { it.subject(spreekuurIdpId.toString()) })
        }.andExpect { status { isNoContent() } }
        mockMvc.get("/{context}/{model-kebab}/${model.id}") {
            with(jwt().jwt { it.subject(spreekuurIdpId.toString()) })
        }.andExpect { status { isNotFound() } }
    }

    // GET 404:
    @Test
    fun `GET returns 404 for non-existent id`() {
        mockMvc.get("/{context}/{model-kebab}/${UUID.randomUUID()}") {
            with(jwt().jwt { it.subject(spreekuurIdpId.toString()) })
        }.andExpect { status { isNotFound() } }
    }

    // Cross-user isolation — for read, update, delete:
    @Test
    fun `GET returns 404 when record belongs to different user`() {
        val model = createTestModel()
        mockMvc.get("/{context}/{model-kebab}/${model.id}") {
            with(jwt().jwt { it.subject(otherUserIdpId.toString()) })
        }.andExpect { status { isNotFound() } }
    }

    // 401 tests — one per HTTP method:
    @Test
    fun `POST without token returns 401`() {
        mockMvc.post("/{context}/{model-kebab}") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ /* fields */ }"""
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET without token returns 401`() {
        mockMvc.get("/{context}/{model-kebab}/${UUID.randomUUID()}") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `PUT without token returns 401`() {
        mockMvc.put("/{context}/{model-kebab}/${UUID.randomUUID()}") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ /* fields */ }"""
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `DELETE without token returns 401`() {
        mockMvc.delete("/{context}/{model-kebab}/${UUID.randomUUID()}") {
            with(anonymous())
        }.andExpect { status { isUnauthorized() } }
    }
}
```
