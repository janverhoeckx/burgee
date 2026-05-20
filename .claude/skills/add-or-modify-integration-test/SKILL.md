---
name: add-or-modify-integration-test
description: Adds or modifies an integration test for a REST controller in the burgee feature-flag service (Spring Boot + Testcontainers + MockMvc + HTTP Basic auth). Use when the user asks to write, add, update, or extend an IT test, integration test, or `*ControllerIT.kt` for a burgee controller.
disable-model-invocation: true
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Add or modify an integration test

Integration tests live in `backend/src/it/kotlin/io/burgee/` and run against a real PostgreSQL container with Flyway-migrated schema. They exercise the full HTTP stack via MockMvc and Spring Security with HTTP Basic auth.

## Non-negotiable rules

Read these before generating anything. Every one of them has bitten a previous run of this skill.

1. **MUST inject dependencies as `private val` primary-constructor parameters.** Never write `@Autowired lateinit var mockMvc: MockMvc` (or any other `@Autowired lateinit var`) in an IT class. `AbstractIT` carries `@TestConstructor(autowireMode = ALL)`, so JUnit 5 resolves constructor parameters from the Spring context by type — no `@Autowired` annotation needed. If you catch yourself importing `org.springframework.beans.factory.annotation.Autowired`, stop and switch to constructor injection.
2. **MUST import `tools.jackson.databind.ObjectMapper`** (Jackson 3). Never `com.fasterxml.jackson.databind.ObjectMapper`. Burgee is Spring Boot 4 and the autowired bean is Jackson 3.
3. **MUST import `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.** The Spring Boot 3 path `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` does not exist here.
4. **MUST use `httpBasic("admin", "admin")`** for admin endpoints. Never `@WithMockUser`, JWT, or `csrf()`.
5. **MUST generate unique keys per test** (`"<feature>-${System.nanoTime()}-$suffix"`) — the Postgres container is shared and never reset.
6. **MUST use lowercase-only keys** matching the `^[a-z0-9][a-z0-9._-]*$` validation regex (use `kebab-case` suffixes, not `camelCase`).
7. **MUST assert `andExpect { status { isCreated() } }` inside any `seedFlag` / `createAndExtractId` helper** before extracting the id. Otherwise a silent 400 turns into a junk id and a downstream 404.

## Arguments

Expected format:
```
<ControllerName> <operations>
```

- `<ControllerName>` — the controller's class name, e.g. `AdminFlagController` or `PublicFlagController`
- `<operations>` — one or more endpoint verbs to cover: `list`, `get`, `create`, `update`, `toggle`, `delete`, or `all`

**If either argument is missing, ask the user before generating.**

## Project conventions

- **Package layout:** controllers are at `io.burgee.<feature>.adapter.inbound.web.<Name>Controller`. The matching IT test goes to `backend/src/it/kotlin/io/burgee/<feature>/<Name>IT.kt` (same `<feature>` package as production code, but flattened — no `adapter.inbound.web` segment in the IT package).
- **Base class:** every IT test extends `io.burgee.AbstractIT`, which starts a `postgres:16` Testcontainer via `@ServiceConnection`, activates the `integration-test` profile, and is meta-annotated with `@TestConstructor(autowireMode = ALL)` so concrete IT classes can declare dependencies as plain constructor `val`s without needing `@Autowired`.
- **Authentication:** burgee uses HTTP Basic auth, **not JWT**. Use `httpBasic("admin", "admin")` from `SecurityMockMvcRequestPostProcessors` for admin endpoints. Public endpoints (`/api/v1/flags/**`) accept anonymous requests.
- **Body serialization:** use the autowired `ObjectMapper` from **Jackson 3** (`tools.jackson.databind.ObjectMapper`, not `com.fasterxml.jackson.databind.ObjectMapper`) with the request DTOs from `io.burgee.flag.adapter.inbound.web.*` (e.g. `CreateFeatureFlagRequest`, `UpdateFeatureFlagRequest`). Avoid hand-written JSON strings except for negative validation cases.
- **State isolation:** the Postgres container is shared across tests, so generate unique keys per test (e.g. `"flag-${System.nanoTime()}-$suffix"`). Do not assume an empty database between tests.
- **No `csrf()`** — CSRF is disabled in `SecurityConfig`.

## Endpoint coverage checklist

For each operation, include both the happy path and the documented error cases. Copy this checklist into your reasoning and tick items off as you write:

```
- [ ] Happy path test (201/200/204 with body assertions)
- [ ] 404 test for non-existent id/key (read, update, toggle, delete)
- [ ] 409 test for duplicate-key conflicts (create)
- [ ] 400 test with invalid body (create, update — when validation rules exist)
- [ ] 401 test with `anonymous()` for every admin endpoint
- [ ] 401 test with wrong-password `httpBasic(...)` (once per IT class is enough)
```

Public endpoints (no auth required) should test that anonymous access works and only the public projection (`key`, `enabled`) is returned.

## Test class skeleton

Copy this exact shape. The only edits you should make are class name, package, DTO type, URL paths, and which HTTP-verb imports you keep. **Do not add `@Autowired` anywhere**, and do not change `private val` to `lateinit var`.

```kotlin
package io.burgee.<feature>

import tools.jackson.databind.ObjectMapper
import io.burgee.AbstractIT
import io.burgee.<feature>.adapter.inbound.web.<RequestDto>
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
// add only the verbs you actually use:
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
class <Name>IT(
    // Constructor injection only. Do NOT switch to `@Autowired lateinit var`.
    // AbstractIT is annotated `@TestConstructor(autowireMode = ALL)`, so these
    // `val`s are resolved from the Spring context by type without `@Autowired`.
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : AbstractIT() {

    private val admin get() = httpBasic("admin", "admin")

    private fun uniqueKey(suffix: String) = "<feature>-${System.nanoTime()}-$suffix"

    // helper for endpoints that create a resource and return an id:
    private fun createAndExtractId(request: <RequestDto>): String {
        // assert isCreated() so a silent 400 surfaces here, not as a downstream 404
        val body = mockMvc.post("/api/admin/<resource>") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
            with(admin)
        }.andExpect { status { isCreated() } }
            .andReturn().response.contentAsString
        return body.substringAfter("\"id\":\"").substringBefore("\"")
    }
}
```

## Reference patterns

For complete, working examples, read these files before generating new ones:

- `backend/src/it/kotlin/io/burgee/flag/AdminFlagControllerIT.kt` — full CRUD + toggle + 401/404/409/400 cases against a secured endpoint
- `backend/src/it/kotlin/io/burgee/flag/PublicFlagControllerIT.kt` — anonymous-accessible endpoints that only expose a public projection

## Execution steps

1. **Parse arguments.** If either is missing, ask the user.
2. **Read the controller source** at `backend/src/main/kotlin/io/burgee/<feature>/adapter/inbound/web/<Name>.kt` to identify each `@RequestMapping`, status codes returned by sealed `Result` types, and the DTOs in `FlagDtos.kt` (or the feature's equivalent).
3. **Read one reference IT** above to match style and import order.
4. **Generate or edit** `backend/src/it/kotlin/io/burgee/<feature>/<Name>IT.kt`, covering only the requested operations plus the checklist items that apply to them.
5. **Pre-verification scan** of the file you just wrote — every item below must be true before you call the task done:
   - [ ] No `import org.springframework.beans.factory.annotation.Autowired` line exists.
   - [ ] No `lateinit var` declaration exists.
   - [ ] Class declaration is `class <Name>IT(...) : AbstractIT()` with `private val` parameters.
   - [ ] `ObjectMapper` import is `tools.jackson.databind.ObjectMapper`.
   - [ ] `AutoConfigureMockMvc` import is `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
   - [ ] Every test key matches `^[a-z0-9][a-z0-9._-]*$` (no `camelCase` segments inside keys).
   - [ ] Every helper that creates a resource asserts the success status before extracting fields.
6. **Verify**: `cd backend && ./mvnw verify` (or `mvn verify`) runs the Failsafe-bound IT tests.

## Anti-patterns to avoid

The "Non-negotiable rules" section above covers the most common mistakes. Additional, lower-frequency pitfalls:

- **Do not** put IT tests under `src/test/kotlin/` — Failsafe only picks up `*IT.kt` from `src/it/kotlin/`.
- **Do not** assert on `createdAt`/`updatedAt` values — only `exists()`; the timestamps are non-deterministic.
- **Do not** rely on database state from a previous test — even though the container is shared, treat every test as starting from an unknown population of flags.
